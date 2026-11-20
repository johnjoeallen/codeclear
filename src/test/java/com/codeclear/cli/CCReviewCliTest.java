package com.codeclear.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.codeclear.ai.CCAiDriverStatus;
import com.codeclear.ai.CCCandidateConfirmer;
import com.codeclear.ai.CCCandidateVerdict;
import com.codeclear.review.CCReviewService;
import com.codeclear.rules.CCDetectionType;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleDefinition;
import com.codeclear.rules.CCRuleEngine;
import com.codeclear.rules.CCRuleMetadata;
import com.codeclear.rules.CCRuleSeverity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CCReviewCliTest {

    private static final CCRuleEngine ENGINE = new CCRuleEngine();
    private static final String EMPTY_CATCH = "class A {\n  void f() {\n    try { g(); } catch (Exception e) { }\n  }\n}\n";

    private static final CCCandidateConfirmer NO_AI = new CCCandidateConfirmer() {
        @Override
        public CCAiDriverStatus status() {
            return CCAiDriverStatus.notReady("no AI here");
        }

        @Override
        public CCCandidateVerdict confirm(CCRuleDefinition rule, CCRuleCandidate candidate, String source) {
            return CCCandidateVerdict.noVerdict("no AI here");
        }
    };

    @AfterAll
    static void close() {
        ENGINE.close();
    }

    private static CCRuleDefinition rule(String id, CCRuleSeverity severity, CCDetectionType type, String dsl) {
        return new CCRuleDefinition(id, Optional.empty(),
                new CCRuleMetadata(List.of("**/*.java"), "X", severity, CCRuleConfidence.MEDIUM, type),
                "h", Optional.empty(), dsl == null ? Map.of() : Map.of("rule.dsl", dsl));
    }

    private static CCReviewCli cli(CCRuleDefinition... rules) {
        return new CCReviewCli(new CCReviewService(List.of(rules), ENGINE, NO_AI, "test"));
    }

    private static final CCRuleDefinition EMPTY_CATCH_RULE = rule("java/empty-catch", CCRuleSeverity.MEDIUM, CCDetectionType.AST,
            "rule 'r'\nCatch.each { c -> skip project.config('quiet') == 'yes'\n report c, 'Empty catch block' }");

    /** The result of one command run. */
    private record Run(int exit, String out, String err) { }

    private static Run run(CCReviewCli cli, Path dir, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        List<String> all = new ArrayList<>(List.of("review"));
        all.addAll(List.of(args));
        int exit = cli.run(all.toArray(String[]::new), dir, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static Path project(Path dir) throws IOException {
        Files.createDirectories(dir.resolve("src/main/java"));
        Files.writeString(dir.resolve("src/main/java/A.java"), EMPTY_CATCH);
        return dir;
    }

    @Test
    void reviewsAFolderAndPrintsFindingsWithASummary(@TempDir Path dir) throws Exception {
        Run r = run(cli(EMPTY_CATCH_RULE), project(dir), "src");

        assertThat(r.exit()).isEqualTo(CCReviewCli.OK);
        assertThat(r.out()).contains("src/main/java/A.java:3  MEDIUM  java/empty-catch", "Empty catch block",
                "1 finding from 1 rule", "AI: test - no AI here");
    }

    @Test
    void theDefaultPathIsTheWorkingDirectory(@TempDir Path dir) throws Exception {
        assertThat(run(cli(EMPTY_CATCH_RULE), project(dir)).out()).contains("src/main/java/A.java:3");
    }

    @Test
    void aSingleFileCanBeNamed(@TempDir Path dir) throws Exception {
        project(dir);

        assertThat(run(cli(EMPTY_CATCH_RULE), dir, "src/main/java/A.java").out()).contains("A.java:3");
    }

    @Test
    void foldersAreSearchedForJavaFilesAndBuildOutputIsSkipped(@TempDir Path dir) throws Exception {
        project(dir);
        Files.createDirectories(dir.resolve("target/generated"));
        Files.writeString(dir.resolve("target/generated/G.java"), EMPTY_CATCH);
        Files.writeString(dir.resolve("src/main/java/notes.txt"), "not java");

        Run r = run(cli(EMPTY_CATCH_RULE), dir);

        assertThat(r.out()).contains("src/main/java/A.java").doesNotContain("G.java").doesNotContain("notes.txt");
    }

    @Test
    void jsonOutputCarriesTheWholeResult(@TempDir Path dir) throws Exception {
        Run r = run(cli(EMPTY_CATCH_RULE), project(dir), "--format=json");

        assertThat(r.exit()).isZero();
        assertThat(r.out()).contains("\"findings\"", "\"ruleId\" : \"java/empty-catch\"", "\"severity\" : \"MEDIUM\"",
                "\"aiDriver\" : \"test\"", "\"skippedRules\"");
    }

    @Test
    void failOnSetsTheExitCodeFromTheWorstSeverity(@TempDir Path dir) throws Exception {
        project(dir);
        CCReviewCli cli = cli(EMPTY_CATCH_RULE);

        assertThat(run(cli, dir, "--fail-on=medium").exit()).isEqualTo(CCReviewCli.FINDINGS);
        assertThat(run(cli, dir, "--fail-on=low").exit()).isEqualTo(CCReviewCli.FINDINGS);
        assertThat(run(cli, dir, "--fail-on=high").exit()).isEqualTo(CCReviewCli.OK);
        assertThat(run(cli, dir, "--fail-on=never").exit()).isEqualTo(CCReviewCli.OK);
        assertThat(run(cli, dir).exit()).isEqualTo(CCReviewCli.OK);
    }

    @Test
    void configFactsReachTheRules(@TempDir Path dir) throws Exception {
        Run quiet = run(cli(EMPTY_CATCH_RULE), project(dir), "--config=quiet=yes");

        assertThat(quiet.out()).contains("0 findings");
    }

    @Test
    void skippedRulesAreSummarisedAndListedOnRequest(@TempDir Path dir) throws Exception {
        CCReviewCli cli = cli(EMPTY_CATCH_RULE,
                rule("java/judgement", CCRuleSeverity.LOW, CCDetectionType.AI, null),
                rule("java/todo", CCRuleSeverity.LOW, CCDetectionType.AST, null));
        project(dir);

        Run summary = run(cli, dir);
        Run listed = run(cli, dir, "--show-skipped");

        assertThat(summary.out()).contains("Not checked: 1 need the AI, 1 have no detector yet", "(--show-skipped lists them)");
        assertThat(listed.out()).contains("NEEDS_AI  java/judgement", "NO_DETECTOR  java/todo").doesNotContain("--show-skipped lists");
    }

    @Test
    void unconfirmedCandidatesAreShownSeparatelyFromFindings(@TempDir Path dir) throws Exception {
        CCReviewCli cli = cli(rule("java/hybrid", CCRuleSeverity.LOW, CCDetectionType.HYBRID,
                "rule 'r'\nCatch.each { c -> candidate c, 'maybe a problem' }"));

        Run r = run(cli, project(dir));

        assertThat(r.out()).contains("Unconfirmed candidates", "src/main/java/A.java:3  java/hybrid", "maybe a problem",
                "0 findings");
    }

    @Test
    void helpPrintsUsageAndSucceeds(@TempDir Path dir) {
        Run r = run(cli(), dir, "--help");

        assertThat(r.exit()).isZero();
        assertThat(r.out()).contains("Usage: codeclear review", "--diff=<rev>", "--fail-on=<level>");
    }

    @Test
    void badUsageIsExitCodeTwoWithAMessage(@TempDir Path dir) throws Exception {
        project(dir);
        CCReviewCli cli = cli(EMPTY_CATCH_RULE);

        assertThat(run(cli, dir, "--bogus").err()).contains("unknown option --bogus");
        assertThat(run(cli, dir, "--format=xml").err()).contains("--format must be text or json");
        assertThat(run(cli, dir, "--fail-on=sometimes").err()).contains("--fail-on must be");
        assertThat(run(cli, dir, "--config=oops").err()).contains("--config needs key=value");
        assertThat(run(cli, dir, "missing").err()).contains("no such file or folder: missing");
        for (String args : new String[]{"--bogus", "--format=xml", "--fail-on=sometimes", "--config=oops", "missing"}) {
            assertThat(run(cli, dir, args).exit()).as(args).isEqualTo(CCReviewCli.USAGE);
        }
    }

    @Test
    void anEmptyFolderIsAnErrorNotACleanReview(@TempDir Path dir) {
        Run r = run(cli(EMPTY_CATCH_RULE), dir);

        assertThat(r.exit()).isEqualTo(CCReviewCli.USAGE);
        assertThat(r.err()).contains("no files to review");
    }

    @Test
    void frameworkSettingsPassedOnTheCommandLineAreNotRejected(@TempDir Path dir) throws Exception {
        assertThat(run(cli(EMPTY_CATCH_RULE), project(dir), "--codeclear.rules-dir=/x", "--logging.level.root=ERROR").exit()).isZero();
    }

    @Test
    void diffReviewsOnlyTheChangedLines(@TempDir Path dir) throws Exception {
        assumeTrue(git(dir, "init", "-q") == 0, "git is not available");
        Files.createDirectories(dir.resolve("src"));
        Files.writeString(dir.resolve("src/A.java"), EMPTY_CATCH);
        git(dir, "add", "-A");
        git(dir, "-c", "user.email=a@b", "-c", "user.name=t", "commit", "-qm", "base");
        Files.writeString(dir.resolve("src/A.java"), EMPTY_CATCH.replace("  void f() {",
                "  void h() {\n    try { g(); } catch (Exception e) { }\n  }\n  void f() {"));

        Run r = run(cli(EMPTY_CATCH_RULE), dir, "--diff=HEAD");

        assertThat(r.out()).contains("src/A.java:3").doesNotContain("src/A.java:6").contains("1 finding");
    }

    @Test
    void diffWithNothingChangedSaysSo(@TempDir Path dir) throws Exception {
        assumeTrue(git(dir, "init", "-q") == 0, "git is not available");
        Files.writeString(dir.resolve("A.java"), EMPTY_CATCH);
        git(dir, "add", "-A");
        git(dir, "-c", "user.email=a@b", "-c", "user.name=t", "commit", "-qm", "base");

        Run r = run(cli(EMPTY_CATCH_RULE), dir, "--diff=HEAD");

        assertThat(r.exit()).isEqualTo(CCReviewCli.USAGE);
        assertThat(r.err()).contains("nothing changed since HEAD");
    }

    @Test
    void aGitFailureIsReported(@TempDir Path dir) throws Exception {
        assumeTrue(git(dir, "init", "-q") == 0, "git is not available");
        project(dir);

        Run r = run(cli(EMPTY_CATCH_RULE), dir, "--diff=no-such-rev");

        assertThat(r.exit()).isEqualTo(CCReviewCli.USAGE);
        assertThat(r.err()).contains("no-such-rev");
    }

    private static int git(Path dir, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        return p.waitFor();
    }
}
