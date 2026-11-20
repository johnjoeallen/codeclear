package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GroovyRuleDetectorTest {

    private static final GroovyRuleDetector DETECTOR = new GroovyRuleDetector(Duration.ofMillis(1500));
    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();
    private static final String SOURCE = "class A {\n  void f() {}\n  void g() {}\n}\n";

    @AfterAll
    static void close() {
        DETECTOR.close();
    }

    private static CCRuleDefinition rule(String script) {
        return rule(script, CCRuleSeverity.MEDIUM, CCRuleConfidence.MEDIUM, "**/*.java");
    }

    private static CCRuleDefinition rule(String script, CCRuleSeverity severity, CCRuleConfidence confidence, String glob) {
        return new CCRuleDefinition("java/test-rule", Optional.empty(),
                new CCRuleMetadata(List.of(glob), "CORRECTNESS", severity, confidence, CCDetectionType.AST),
                "hash", Optional.of(script));
    }

    private static CCFileAnalysis file(CCChangedLines changed) {
        return ANALYZER.analyze("src/main/java/A.java", SOURCE, changed);
    }

    @Test
    void reportsFindingsWithRuleSeverityAndConfidence() {
        CCRuleResult r = DETECTOR.detect(rule("""
                import com.github.javaparser.ast.body.MethodDeclaration
                ast.findAll(MethodDeclaration).collect { m -> finding(m.begin.get().line, "method ${m.nameAsString}") }
                """, CCRuleSeverity.HIGH, CCRuleConfidence.HIGH, "**/*.java"), file(CCChangedLines.all()));

        assertThat(r.ok()).isTrue();
        assertThat(r.findings()).extracting(CCRuleFinding::line, CCRuleFinding::message, CCRuleFinding::severity,
                CCRuleFinding::confidence, CCRuleFinding::path, CCRuleFinding::ruleId)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(2, "method f", CCRuleSeverity.HIGH, CCRuleConfidence.HIGH, "src/main/java/A.java", "java/test-rule"),
                        org.assertj.core.api.Assertions.tuple(3, "method g", CCRuleSeverity.HIGH, CCRuleConfidence.HIGH, "src/main/java/A.java", "java/test-rule"));
    }

    @Test
    void dropsResultsOutsideChangedLinesAndCountsThem() {
        CCRuleResult r = DETECTOR.detect(rule("[finding(2, 'on a changed line'), finding(3, 'not changed'), candidate(4, 'also not changed')]"),
                file(CCChangedLines.of(Set.of(2))));

        assertThat(r.findings()).extracting(CCRuleFinding::line).containsExactly(2);
        assertThat(r.candidates()).isEmpty();
        assertThat(r.droppedOffDiff()).isEqualTo(2);
    }

    @Test
    void scriptCanLowerButNeverRaiseConfidence() {
        CCRuleResult r = DETECTOR.detect(rule("[finding(2, 'low', 'LOW'), finding(3, 'tries to raise', 'HIGH')]"),
                file(CCChangedLines.all()));

        assertThat(r.findings()).extracting(CCRuleFinding::confidence)
                .containsExactly(CCRuleConfidence.LOW, CCRuleConfidence.MEDIUM);
    }

    @Test
    void hybridCandidatesAreReturnedSeparately() {
        CCRuleResult r = DETECTOR.detect(rule("[candidate(2, 'needs a look')]"), file(CCChangedLines.all()));

        assertThat(r.findings()).isEmpty();
        assertThat(r.candidates()).extracting(CCRuleCandidate::line, CCRuleCandidate::evidence)
                .containsExactly(org.assertj.core.api.Assertions.tuple(2, "needs a look"));
    }

    @Test
    void resultsAreSortedAndDeduplicated() {
        CCRuleResult r = DETECTOR.detect(rule("[finding(3, 'b'), finding(2, 'a'), finding(3, 'b'), finding(2, 'a')]"),
                file(CCChangedLines.all()));

        assertThat(r.findings()).extracting(CCRuleFinding::line, CCRuleFinding::message)
                .containsExactly(org.assertj.core.api.Assertions.tuple(2, "a"), org.assertj.core.api.Assertions.tuple(3, "b"));
    }

    @Test
    void ruleThatDoesNotApplyToThePathIsSkipped() {
        CCRuleResult r = DETECTOR.detect(rule("[finding(2, 'x')]", CCRuleSeverity.LOW, CCRuleConfidence.LOW, "**/*.py"),
                file(CCChangedLines.all()));

        assertThat(r.ok()).isTrue();
        assertThat(r.findings()).isEmpty();
    }

    @Test
    void aiRulesAreNeverRun() {
        CCRuleDefinition ai = new CCRuleDefinition("java/test-rule", Optional.empty(),
                new CCRuleMetadata(List.of("**/*.java"), "X", CCRuleSeverity.LOW, CCRuleConfidence.LOW, CCDetectionType.AI),
                "hash", Optional.empty());

        CCRuleResult r = DETECTOR.detect(ai, file(CCChangedLines.all()));

        assertThat(r.ok()).isTrue();
        assertThat(r.findings()).isEmpty();
    }

    @Test
    void missingScriptIsAFailureNotAnException() {
        CCRuleDefinition noScript = new CCRuleDefinition("java/test-rule", Optional.empty(),
                new CCRuleMetadata(List.of("**/*.java"), "X", CCRuleSeverity.LOW, CCRuleConfidence.LOW, CCDetectionType.AST),
                "hash", Optional.empty());

        assertThat(DETECTOR.detect(noScript, file(CCChangedLines.all())).failure()).hasValueSatisfying(s -> assertThat(s).contains("no script"));
    }

    @Test
    void scriptExceptionBecomesAFailure() {
        CCRuleResult r = DETECTOR.detect(rule("throw new IllegalStateException('boom')"), file(CCChangedLines.all()));

        assertThat(r.failure()).hasValueSatisfying(s -> assertThat(s).contains("boom"));
    }

    @Test
    void badReturnValuesAreFailures() {
        assertThat(DETECTOR.detect(rule("'not a list'"), file(CCChangedLines.all())).failure()).isPresent();
        assertThat(DETECTOR.detect(rule("['a string']"), file(CCChangedLines.all())).failure()).isPresent();
        assertThat(DETECTOR.detect(rule("[finding(2, 'x', 'BOGUS')]"), file(CCChangedLines.all())).failure()).isPresent();
        assertThat(DETECTOR.detect(rule("[finding(2, '')]"), file(CCChangedLines.all())).failure()).isPresent();
    }

    @Test
    void nullReturnMeansNoFindings() {
        assertThat(DETECTOR.detect(rule("null"), file(CCChangedLines.all())).findings()).isEmpty();
    }

    @Test
    void syntaxErrorIsACompilationFailure() {
        CCRuleResult r = DETECTOR.detect(rule("def x = ("), file(CCChangedLines.all()));

        assertThat(r.ok()).isFalse();
    }

    @Test
    void runawayScriptIsStoppedByTheTimeout() {
        long start = System.nanoTime();
        CCRuleResult r = DETECTOR.detect(rule("def i = 0\nwhile (true) { i++ }"), file(CCChangedLines.all()));
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertThat(r.failure()).hasValueSatisfying(s -> assertThat(s).contains("exceeded"));
        assertThat(ms).isLessThan(5000);
    }

    @Test
    void aTimedOutScriptDoesNotBreakTheNextOne() {
        DETECTOR.detect(rule("while (true) { }"), file(CCChangedLines.all()));

        assertThat(DETECTOR.detect(rule("[finding(2, 'still works')]"), file(CCChangedLines.all())).findings()).hasSize(1);
    }

    @Test
    void scriptsCanUseTheModelAndFileBindings() {
        CCRuleResult r = DETECTOR.detect(rule("""
                import com.github.javaparser.ast.expr.NameExpr
                def types = ast.findAll(NameExpr).collect { model.typeOf(it) }
                [finding(2, "path=${file.path} language=${file.language} types=${types.size()}")]
                """), file(CCChangedLines.all()));

        assertThat(r.findings()).extracting(CCRuleFinding::message).containsExactly("path=src/main/java/A.java language=java types=0");
    }

    static Stream<Arguments> forbiddenScripts() {
        return Stream.of(
                Arguments.of("System.exit(1)", "method call 'exit'"),
                Arguments.of("def s = System\n[]", "java.lang.System"),
                Arguments.of("new File('/etc/passwd').text", "java.io.File"),
                Arguments.of("Runtime.getRuntime().exec('id')", "method call 'exec'"),
                Arguments.of("def r = Runtime\n[]", "java.lang.Runtime"),
                Arguments.of("'id'.execute()", "execute"),
                Arguments.of("evaluate('1+1')", "evaluate"),
                Arguments.of("new GroovyShell().evaluate('1')", "method call 'evaluate'"),
                Arguments.of("def g = new GroovyShell()\n[]", "groovy.lang.GroovyShell"),
                Arguments.of("Class.forName('java.lang.Runtime')", "method call 'forName'"),
                Arguments.of("def c = Class\n[]", "java.lang.Class"),
                Arguments.of("ast.getClass()", "getClass"),
                Arguments.of("ast.class", "property 'class'"),
                Arguments.of("ast.metaClass", "metaClass"),
                Arguments.of("def n = 'getClass'\nast.\"$n\"()", "computed method call name"),
                Arguments.of("def p = 'class'\nast.\"$p\"", "computed property name"),
                Arguments.of("ast.&getClass", "method pointer"),
                Arguments.of("ast.@parentNode", "direct field access"),
                Arguments.of("import java.io.File\n[]", "java.io.File"),
                Arguments.of("import java.nio.file.Files\n[]", "java.nio.file.Files"),
                Arguments.of("import java.net.*\n[]", "star import"),
                Arguments.of("import static java.lang.System.exit\nexit(1)", "static"),
                Arguments.of("@groovy.transform.ASTTest(value={ println 'x' })\ndef x = 1\n[]", "annotation"),
                Arguments.of("@Grab('a:b:1')\nimport groovy.lang.Closure\n[]", "annotation"),
                Arguments.of("class Evil { static void go() { } }\n[]", "not allowed in a rule script"),
                Arguments.of("def r = new Runnable() { void run() { } }\n[]", "constructor of type"),
                Arguments.of("Thread.sleep(10)", "method call 'sleep'"),
                Arguments.of("def t = Thread\n[]", "java.lang.Thread"),
                Arguments.of("new Thread({ } as Runnable).start()", "constructor of type java.lang.Thread"),
                Arguments.of("def x = ast as java.io.Serializable\n[]", "Serializable"),
                Arguments.of("this.invokeMethod('evaluate', ['1'] as Object[])", "invokeMethod"),
                Arguments.of("getBinding().getVariable('file')", "getBinding"),
                Arguments.of("println 'hi'", "println"),
                Arguments.of("ast.removeForced()", "removeForced"),
                Arguments.of("ast.remove()", "remove()"),
                Arguments.of("ast.replace(null)", "replace"),
                Arguments.of("owner", "owner"),
                Arguments.of("def c = { delegate }\n[]", "delegate"));
    }

    @ParameterizedTest(name = "rejects: {0}")
    @MethodSource("forbiddenScripts")
    void sandboxRejectsForbiddenScript(String script, String expectedInReason) {
        CCRuleResult r = DETECTOR.detect(rule(script), file(CCChangedLines.all()));

        assertThat(r.failure()).as("script should have been rejected").isPresent();
        assertThat(r.failure().get()).contains(expectedInReason);
    }

    @Test
    void sandboxStillAllowsOrdinaryRuleCode() {
        CCRuleResult r = DETECTOR.detect(rule("""
                import com.github.javaparser.ast.body.MethodDeclaration
                import java.util.regex.Pattern

                def names = new ArrayList<String>()
                def counts = [:] as LinkedHashMap
                def helper = { String s -> s.toUpperCase() }
                def pattern = Pattern.compile('[fg]')
                ast.findAll(MethodDeclaration).each { MethodDeclaration m ->
                    try {
                        if (pattern.matcher(m.nameAsString).matches()) {
                            names << helper(m.nameAsString)
                            counts[m.nameAsString] = (counts[m.nameAsString] ?: 0) + 1
                        }
                    } catch (RuntimeException e) {
                        throw e
                    }
                }
                names.remove('G')
                [finding(2, names.join(',') + counts.size() + Math.max(1, 2))]
                """), file(CCChangedLines.all()));

        assertThat(r.failure()).isEmpty();
        assertThat(r.findings()).extracting(CCRuleFinding::message).containsExactly("F22");
    }
}
