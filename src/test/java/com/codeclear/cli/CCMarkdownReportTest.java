package com.codeclear.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.review.CCReviewFinding;
import com.codeclear.review.CCReviewResult;
import com.codeclear.review.CCSkippedRule;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleSeverity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CCMarkdownReportTest {

    private static final String SOURCE = String.join("\n",
            "class A {",            // 1
            "  void f() {",         // 2
            "    try {",            // 3
            "      work();",        // 4
            "    } catch (Exception e) { }",  // 5
            "  }",                 // 6
            "  void g() {",         // 7
            "    other();",         // 8
            "  }",                 // 9
            "}") + "\n";

    private static CCReviewFinding finding(String path, int line, String message, CCRuleSeverity severity,
                                           CCRuleConfidence confidence, CCReviewFinding.Source source) {
        return new CCReviewFinding("java/empty-catch-block", path, line, message, severity, confidence, source);
    }

    private static CCReviewFinding finding(String path, int line, String message) {
        return finding(path, line, message, CCRuleSeverity.MEDIUM, CCRuleConfidence.MEDIUM, CCReviewFinding.Source.RULE);
    }

    private static CCReviewResult result(List<CCReviewFinding> findings) {
        return new CCReviewResult(findings, List.of(), List.of(), List.of(), 4, 0, "dummy", "not ready", List.of());
    }

    @Test
    void showsTheFlaggedLineWithNumberedContextAndAMarker() {
        String md = CCMarkdownReport.render(result(List.of(finding("src/A.java", 5, "Empty catch block"))),
                Map.of("src/A.java", SOURCE), 1);

        assertThat(md).contains("### `src/A.java`", "**Line 5** · MEDIUM · `java/empty-catch-block`", "Empty catch block");
        assertThat(md).contains("```java\n"
                + "  2 |   void f() {\n"
                + "  3 |     try {\n"
                + "  4 |       work();\n"
                + "> 5 |     } catch (Exception e) { }\n"
                + "  6 |   }\n"
                + "  7 |   void g() {\n"
                + "  8 |     other();\n"
                + "```");
    }

    @Test
    void theContextIsClippedAtTheStartAndEndOfTheFile() {
        String top = CCMarkdownReport.render(result(List.of(finding("A.java", 1, "x"))), Map.of("A.java", SOURCE), 1);
        String bottom = CCMarkdownReport.render(result(List.of(finding("A.java", 10, "x"))), Map.of("A.java", SOURCE), 1);

        assertThat(top).contains("> 1 | class A {").contains("  4 |       work();").doesNotContain("  5 |");
        assertThat(bottom).contains("> 10 | }").contains("   7 |   void g() {").doesNotContain("  11 |");
    }

    @Test
    void lineNumbersAreAlignedToTheWidestNumberShown() {
        String lines = java.util.stream.IntStream.rangeClosed(1, 120).mapToObj(i -> "line" + i).collect(java.util.stream.Collectors.joining("\n"));

        String md = CCMarkdownReport.render(result(List.of(finding("A.java", 98, "x"))), Map.of("A.java", lines), 1);

        assertThat(md).contains("   95 | line95", ">  98 | line98", "  101 | line101");
    }

    @Test
    void codeThatContainsBackticksGetsALongerFence() {
        String source = "String s = \"\"\"\n  ```java\n  x\n  ```\n  \"\"\";\n";

        String md = CCMarkdownReport.render(result(List.of(finding("A.java", 2, "x"))), Map.of("A.java", source), 1);

        assertThat(md).contains("````java\n").contains("\n````\n");
    }

    @Test
    void findingsAreGroupedByFileAndOrderedByLine() {
        String md = CCMarkdownReport.render(result(List.of(
                finding("src/B.java", 9, "second file"), finding("src/A.java", 8, "later"), finding("src/A.java", 5, "earlier"))),
                Map.of("src/A.java", SOURCE, "src/B.java", SOURCE), 2);

        assertThat(md.indexOf("### `src/A.java`")).isLessThan(md.indexOf("### `src/B.java`"));
        assertThat(md.indexOf("earlier")).isLessThan(md.indexOf("later")).isLessThan(md.indexOf("second file"));
        assertThat(md.split("### `src/A.java`", -1)).hasSize(2); // one heading per file
    }

    @Test
    void theSummaryCountsFindingsBySeverityAndRule() {
        String md = CCMarkdownReport.render(result(List.of(
                finding("A.java", 5, "a"), finding("A.java", 8, "b", CCRuleSeverity.HIGH, CCRuleConfidence.MEDIUM, CCReviewFinding.Source.RULE))),
                Map.of("A.java", SOURCE), 3);

        assertThat(md).contains("| Files reviewed | 3 |", "| Rules run | 4 |", "| Findings | 2 |", "| HIGH | 1 |", "| MEDIUM | 1 |",
                "| `java/empty-catch-block` | 2 |", "| AI | dummy - not ready |");
        assertThat(md).doesNotContain("| CRITICAL |").doesNotContain("| LOW |");
    }

    @Test
    void lowConfidenceAndAiConfirmationAreStated() {
        String md = CCMarkdownReport.render(result(List.of(
                finding("A.java", 5, "a", CCRuleSeverity.LOW, CCRuleConfidence.LOW, CCReviewFinding.Source.RULE),
                finding("A.java", 8, "b", CCRuleSeverity.LOW, CCRuleConfidence.MEDIUM, CCReviewFinding.Source.AI_CONFIRMED))),
                Map.of("A.java", SOURCE), 1);

        assertThat(md).contains("LOW (low confidence)", "confirmed by the AI");
    }

    @Test
    void aCleanReviewSaysSoAndHasNoTables() {
        String md = CCMarkdownReport.render(result(List.of()), Map.of(), 7);

        assertThat(md).contains("## Findings", "No findings.", "| Files reviewed | 7 |").doesNotContain("| Severity |");
    }

    @Test
    void unconfirmedCandidatesAreSeparateAndExplained() {
        CCReviewResult r = new CCReviewResult(List.of(), List.of(new CCRuleCandidate("java/hybrid", "A.java", 5, "maybe a problem")),
                List.of(), List.of(), 1, 0, "dummy", "not ready", List.of());

        String md = CCMarkdownReport.render(r, Map.of("A.java", SOURCE), 1);

        assertThat(md).contains("## Unconfirmed candidates", "They are not findings", "**Line 5** · `java/hybrid`", "maybe a problem",
                "> 5 |     } catch (Exception e) { }");
        assertThat(md).contains("No findings.");
    }

    @Test
    void rulesThatWereNotCheckedAreListedByReasonAndFailuresGiveTheirDetail() {
        CCReviewResult r = new CCReviewResult(List.of(), List.of(), List.of(
                new CCSkippedRule("java/judgement", CCSkippedRule.Reason.NEEDS_AI, "needs the AI"),
                new CCSkippedRule("java/todo", CCSkippedRule.Reason.NO_DETECTOR, "none"),
                new CCSkippedRule("java/broken", CCSkippedRule.Reason.FAILED, "bad rule: unknown field")),
                List.of(), 1, 0, "dummy", "not ready", List.of());

        String md = CCMarkdownReport.render(r, Map.of(), 1);

        assertThat(md).contains("## Not checked", "<summary>Need the AI (1)</summary>", "- `java/judgement`",
                "<summary>No detector written yet (1)</summary>", "<summary>Failed to run (1)</summary>",
                "- `java/broken`: bad rule: unknown field", "| Rules not checked | 3 |");
    }

    @Test
    void messagesCannotBreakTheTableOrInjectHtml() {
        String md = CCMarkdownReport.render(result(List.of(finding("A.java", 5, "a | b <script>x</script>\nsecond line"))),
                Map.of("A.java", SOURCE), 1);

        assertThat(md).contains("a \\| b &lt;script&gt;x&lt;/script&gt; second line").doesNotContain("<script>");
    }

    @Test
    void aFindingWhoseSourceIsUnknownIsStillListedWithoutASnippet() {
        String md = CCMarkdownReport.render(result(List.of(finding("Gone.java", 5, "x"), finding("A.java", 99, "beyond the end"))),
                Map.of("A.java", SOURCE), 2);

        assertThat(md).contains("### `Gone.java`", "**Line 5**", "**Line 99**").doesNotContain("```");
    }

    @Test
    void windowsLineEndingsAndTrailingSpacesDoNotLeakIntoTheSnippet() {
        String md = CCMarkdownReport.render(result(List.of(finding("A.java", 2, "x"))),
                Map.of("A.java", "class A {   \r\n  int x;\t \r\n}\r\n"), 1);

        assertThat(md).contains("  1 | class A {\n", "> 2 |   int x;\n", "  3 | }\n").doesNotContain("\r");
    }

    @Test
    void notesAreListed() {
        CCReviewResult r = new CCReviewResult(List.of(), List.of(), List.of(), List.of("1 file(s) not analysed: only Java has an analyzer so far"),
                1, 0, "dummy", "not ready", List.of());

        assertThat(CCMarkdownReport.render(r, Map.of(), 1)).contains("## Notes", "- 1 file(s) not analysed");
    }

    @Test
    void suppressedFindingsAreListedWithoutCode() {
        CCReviewResult r = new CCReviewResult(List.of(), List.of(), List.of(), List.of(), 1, 0, "dummy", "not ready",
                List.of(new com.codeclear.review.CCSuppressedFinding("java/empty-catch-block", "A.java", 2, "secret message")));

        String md = CCMarkdownReport.render(r, Map.of("A.java", "class A {\n  int x;\n}\n"), 1);

        assertThat(md).contains("## Suppressed", "| Suppressed | 1 |", "**Line 2** · `java/empty-catch-block` · suppressed")
                .doesNotContain("int x;").doesNotContain("secret message");
    }
}
