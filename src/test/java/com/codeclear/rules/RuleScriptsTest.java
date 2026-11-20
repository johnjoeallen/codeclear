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

/** Runs every committed rule script against its own tests.md, through the real detector and sandbox. */
class RuleScriptsTest {

    private static final Path REVIEW_ROOT = Path.of("review");
    private static final GroovyRuleDetector DETECTOR = new GroovyRuleDetector();

    @AfterAll
    static void close() {
        DETECTOR.close();
    }

    @Test
    void everyRuleFolderLoads() {
        assertThat(CCRuleLoader.loadAll(REVIEW_ROOT)).hasSize(320);
    }

    @TestFactory
    Stream<DynamicTest> everyScriptIsCurrentAndPassesItsTests() {
        List<CCRuleDefinition> withScripts = CCRuleLoader.loadAll(REVIEW_ROOT).stream()
                .filter(r -> r.script().isPresent())
                .toList();
        assertThat(withScripts).as("rules that have a script").isNotEmpty();
        return withScripts.stream().map(rule -> dynamicTest(rule.id(), () -> {
            assertThat(rule.stale()).as("script header matches rule.md and API version").isFalse();
            CCRuleTestRunner.Report report = new CCRuleTestRunner(DETECTOR).run(rule);
            assertThat(report.cases()).as("cases").isNotEmpty();
            report.cases().stream().filter(c -> !c.passed()).forEach(c ->
                    org.junit.jupiter.api.Assertions.fail(rule.id() + ": " + c.name() + ": " + c.detail()));
        }));
    }

    /** Gate check: every "Do not flag" bullet needs at least one test case labelled do-not-flag:*. */
    @TestFactory
    Stream<DynamicTest> everyDoNotFlagBulletHasATest() {
        return CCRuleLoader.loadAll(REVIEW_ROOT).stream().filter(r -> r.script().isPresent()).map(rule ->
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
        // 172 executable rules exist; two have scripts so far. This documents the backlog, it is not a target.
        assertThat(staleCount).isEqualTo(172 - 2);
    }
}
