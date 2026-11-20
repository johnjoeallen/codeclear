package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Runs every committed rule detector (Groovy and restricted-language) against its own tests.md, through the real engine. */
class RuleScriptsTest {

    private static final Path REVIEW_ROOT = Path.of("review");
    private static final GroovyRuleDetector DETECTOR = new GroovyRuleDetector();
    private static final com.codeclear.rules.dsl.DslRuleDetector DSL = new com.codeclear.rules.dsl.DslRuleDetector();

    @AfterAll
    static void close() {
        DETECTOR.close();
    }

    @Test
    void everyRuleFolderLoads() {
        assertThat(CCRuleLoader.loadAll(REVIEW_ROOT)).hasSize(320);
    }

    @TestFactory
    Stream<DynamicTest> everyDetectorIsCurrentAndPassesItsTests() {
        List<CCRuleDefinition> withDetectors = CCRuleLoader.loadAll(REVIEW_ROOT).stream()
                .filter(CCRuleDefinition::hasDetector)
                .toList();
        assertThat(withDetectors).as("rules that have a detector").isNotEmpty();
        List<DynamicTest> tests = new java.util.ArrayList<>();
        for (CCRuleDefinition rule : withDetectors) {
            if (rule.script().isPresent()) {
                tests.add(dynamicTest(rule.id() + " [groovy]", () -> check(rule,
                        new CCRuleEngine(DETECTOR, DSL, CCRuleEngine.Prefer.GROOVY))));
            }
            if (!rule.dsl().isEmpty()) {
                tests.add(dynamicTest(rule.id() + " [dsl]", () -> check(rule,
                        new CCRuleEngine(DETECTOR, DSL, CCRuleEngine.Prefer.DSL))));
            }
        }
        return tests.stream();
    }

    private static void check(CCRuleDefinition rule, CCRuleEngine engine) {
        assertThat(rule.stale()).as("detector headers match rule.md and the API version").isFalse();
        CCRuleTestRunner.Report report = new CCRuleTestRunner(engine).run(rule);
        assertThat(report.cases()).as("cases").isNotEmpty();
        report.cases().stream().filter(c -> !c.passed()).forEach(c ->
                org.junit.jupiter.api.Assertions.fail(rule.id() + ": " + c.name() + ": " + c.detail()));
    }

    /** Gate check: every "Do not flag" bullet needs at least one test case labelled do-not-flag:*. */
    @TestFactory
    Stream<DynamicTest> everyDoNotFlagBulletHasATest() {
        return CCRuleLoader.loadAll(REVIEW_ROOT).stream().filter(CCRuleDefinition::hasDetector).map(rule ->
                dynamicTest(rule.id(), () -> {
                    Path dir = rule.dir().orElseThrow();
                    String md = java.nio.file.Files.readString(dir.resolve("rule.md"));
                    var m = java.util.regex.Pattern.compile("(?s)## Do not flag\\n(.*?)(\\n## |\\z)").matcher(md);
                    long bullets = m.find() ? m.group(1).lines().filter(l -> l.startsWith("- ")).count() : 0;
                    var labels = new java.util.HashSet<String>();
                    CCRuleTests.load(dir).cases().forEach(c -> c.covers().stream()
                            .filter(l -> l.startsWith("do-not-flag:")).forEach(labels::add));
                    assertThat((long) labels.size()).as("distinct do-not-flag labels vs bullets").isGreaterThanOrEqualTo(bullets);
                }));
    }

    @Test
    void executableRulesWithoutScriptsAreStale() {
        long staleCount = CCRuleLoader.loadAll(REVIEW_ROOT).stream().filter(CCRuleDefinition::stale).count();
        // 173 executable rules exist; 152 have a detector so far. This documents the backlog, it is not a target.
        assertThat(staleCount).isEqualTo(173 - 152);
    }
}
