package com.codeclear.rules.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.JavaFileAnalyzer;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.codeclear.rules.CCRuleTests;

/**
 * A tier 2 rule in the restricted language: lazy JPA associations read outside a session. Needs a project-wide
 * model (entities, fetch types, transactions, configuration), so each case is a small multi-file project.
 */
class DslProjectTest {

    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();
    private static final Path TESTS = Path.of("review/java/lazy-association-outside-session");

    @Test
    void lazyAssociationRules() throws Exception {
        check("lazy-association", TESTS, List.of("rule.dsl", "serialized.dsl"));
    }

    @Test
    void bigDecimalMisuse() throws Exception {
        check("bigdecimal-misuse", Path.of("review/java/bigdecimal-misuse"), List.of("rule.dsl", "equals.dsl"));
    }

    @Test
    void genericExceptionInPublicApi() throws Exception {
        check("generic-exception-in-public-api", Path.of("review/java/generic-exception-in-public-api"), List.of("rule.dsl", "throw.dsl"));
    }

    @Test
    void unboundedExecutorQueue() throws Exception {
        check("unbounded-executor-queue", Path.of("review/java/unbounded-executor-queue"), List.of("rule.dsl", "pool.dsl"));
    }

    @Test
    void transactionalOnPrivateMethod() throws Exception {
        check("transactional-on-private-method", Path.of("review/java/transactional-on-private-method"), List.of("rule.dsl", "selfcall.dsl"));
    }

    @Test
    void optionalFieldOrParameter() throws Exception {
        check("optional-field-or-parameter", Path.of("review/java/optional-field-or-parameter"), List.of("rule.dsl", "param.dsl"));
    }

    @Test
    void timezoneAmbiguity() throws Exception {
        check("timezone-ambiguity", Path.of("review/java/timezone-ambiguity"), List.of("rule.dsl", "sdf.dsl", "field.dsl"));
    }

    @Test
    void floatingPointMoney() throws Exception {
        check("floating-point-money", Path.of("review/java/floating-point-money"), List.of("rule.dsl", "param.dsl"));
    }

    @Test
    void unconsumedLazyStream() throws Exception {
        check("unconsumed-lazy-stream", Path.of("review/java/unconsumed-lazy-stream"), List.of("rule.dsl", "terminal.dsl", "local.dsl"));
    }

    @Test
    void threadlocalNotCleaned() throws Exception {
        check("threadlocal-not-cleaned", Path.of("review/java/threadlocal-not-cleaned"), List.of("rule.dsl"));
    }

    @Test
    void logInjection() throws Exception {
        check("log-injection", Path.of("review/common/log-injection"), List.of("rule.dsl"));
    }

    private void check(String label, Path testsDir, List<String> ruleNames) throws Exception {
        CCRuleTests.Tests tests = CCRuleTests.load(testsDir);
        List<RuleAst> rules = new ArrayList<>();
        for (String r : ruleNames) {
            rules.add(DslParser.parse(Files.readString(testsDir.resolve(r))));
        }
        List<String> failures = new ArrayList<>();
        for (CCRuleTests.Case c : tests.cases()) {
            String got = run(rules, c, tests.sharedPrelude()).toString();
            String exp = expected(c).toString();
            if (!got.equals(exp)) {
                failures.add(c.name() + ": got " + got + ", expected " + exp);
            }
        }
        int total = tests.cases().size();
        System.out.println("DSL " + label + ": " + (total - failures.size()) + "/" + total + " cases pass");
        failures.forEach(f -> System.out.println("  FAIL " + f));

        if (!failures.isEmpty()) {
            // mutants are only meaningful against a passing baseline: a failing case would "kill" every mutant
            assertThat(failures).isEmpty();
        }
        for (int ri = 0; ri < rules.size(); ri++) {
            List<DslMutator.Mutant> mutants = DslMutator.mutants(rules.get(ri));
            List<String> survivors = new ArrayList<>();
            for (DslMutator.Mutant m : mutants) {
                List<RuleAst> variant = new ArrayList<>(rules);
                variant.set(ri, m.rule());
                boolean caught = false;
                for (CCRuleTests.Case c : tests.cases()) {
                    try {
                        if (!run(variant, c, tests.sharedPrelude()).toString().equals(expected(c).toString())) {
                            caught = true;
                            break;
                        }
                    } catch (RuntimeException e) {
                        caught = true;
                        break;
                    }
                }
                if (!caught) {
                    survivors.add(m.description());
                }
            }
            System.out.println("DSL " + rules.get(ri).id() + ": " + mutants.size() + " mutants, "
                    + (mutants.size() - survivors.size()) + " killed, " + survivors.size() + " survived");
            survivors.forEach(s -> System.out.println("  SURVIVED " + s));
        }
        assertThat(failures).isEmpty();
    }

    private static List<String> expected(CCRuleTests.Case c) {
        return c.expect().stream().map(e -> e.path() + ":" + e.line() + (e.candidate() ? ":CANDIDATE" : ":" + (e.confidence() == null ? "HIGH" : e.confidence()))).sorted().toList();
    }

    private static List<String> run(List<RuleAst> rules, CCRuleTests.Case c, String sharedPrelude) {
        String prelude = c.prelude() != null ? c.prelude() : sharedPrelude;
        List<CCFileAnalysis> files = new ArrayList<>();
        List<CompilationUnit> units = new ArrayList<>();
        for (CCRuleTests.SourceFile f : c.files()) {
            CCFileAnalysis fa = ANALYZER.analyze(f.path(), CCRuleTests.analysisText(prelude, f.source()), CCChangedLines.all());
            files.add(fa);
            units.add((CompilationUnit) fa.ast());
        }
        DslProject project = new DslProject(units, new HashMap<>(c.config()));
        int offset = CCRuleTests.lineOffset(prelude);
        List<String> out = new ArrayList<>();
        for (RuleAst rule : rules) {
            for (CCFileAnalysis fa : files) {
                new DslInterpreter().run(rule, fa, "HIGH", project)
                        .forEach(f -> out.add(fa.file().getPath() + ":" + (f.line() - offset) + (f.candidate() ? ":CANDIDATE" : ":" + f.confidence())));
            }
        }
        return out.stream().sorted().toList();
    }
}
