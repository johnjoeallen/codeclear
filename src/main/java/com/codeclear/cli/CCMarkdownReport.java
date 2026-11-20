package com.codeclear.cli;

import com.codeclear.review.CCReviewFinding;
import com.codeclear.review.CCReviewResult;
import com.codeclear.review.CCSkippedRule;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleSeverity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a review as a Markdown document: a summary, then each finding with the code around it, grouped by file, then the
 * candidates the AI could not judge and the rules that were not checked. Meant to be read in an editor or a pull request.
 */
final class CCMarkdownReport {

    /** Lines of code shown above and below the flagged line. */
    static final int CONTEXT = 3;

    private static final Pattern BACKTICKS = Pattern.compile("`+");

    private CCMarkdownReport() { }

    /**
     * @param sources       the text of each reviewed file by path, for the snippets
     * @param filesReviewed how many files the review covered
     */
    static String render(CCReviewResult r, Map<String, String> sources, int filesReviewed) {
        StringBuilder b = new StringBuilder("# CodeClear review\n\n");
        summary(b, r, filesReviewed);

        b.append("## Findings\n\n");
        if (r.findings().isEmpty()) {
            b.append("No findings.\n\n");
        } else {
            Map<String, List<CCReviewFinding>> byFile = new TreeMap<>();
            r.findings().forEach(f -> byFile.computeIfAbsent(f.path(), k -> new ArrayList<>()).add(f));
            byFile.forEach((path, list) -> {
                b.append("### `").append(path).append("`\n\n");
                list.stream().sorted(java.util.Comparator.comparingInt(CCReviewFinding::line)).forEach(f -> {
                    b.append("**Line ").append(f.line()).append("** · ").append(f.severity())
                            .append(f.confidence() == CCRuleConfidence.LOW ? " (low confidence)" : "")
                            .append(" · `").append(f.ruleId()).append('`')
                            .append(f.source() == CCReviewFinding.Source.AI_CONFIRMED ? " · confirmed by the AI" : "")
                            .append("\n\n").append(escape(f.message())).append("\n\n");
                    snippet(b, sources.get(path), f.line(), path);
                });
            });
        }

        if (!r.unconfirmedCandidates().isEmpty()) {
            b.append("## Unconfirmed candidates\n\n")
                    .append("These look suspicious to a rule, but only the AI can judge them, and no AI was available. They are not findings.\n\n");
            Map<String, List<CCRuleCandidate>> byFile = new TreeMap<>();
            r.unconfirmedCandidates().forEach(c -> byFile.computeIfAbsent(c.path(), k -> new ArrayList<>()).add(c));
            byFile.forEach((path, list) -> {
                b.append("### `").append(path).append("`\n\n");
                list.stream().sorted(java.util.Comparator.comparingInt(CCRuleCandidate::line)).forEach(c -> {
                    b.append("**Line ").append(c.line()).append("** · `").append(c.ruleId()).append("`\n\n")
                            .append(escape(c.evidence())).append("\n\n");
                    snippet(b, sources.get(path), c.line(), path);
                });
            });
        }

        if (!r.suppressed().isEmpty()) {
            b.append("## Suppressed\n\nIgnored by a `CodeClear:` comment. Shown so that nothing is hidden; no code is quoted.\n\n");
            Map<String, List<com.codeclear.review.CCSuppressedFinding>> byFile = new TreeMap<>();
            r.suppressed().forEach(x -> byFile.computeIfAbsent(x.path(), k -> new ArrayList<>()).add(x));
            byFile.forEach((path, list) -> {
                b.append("### `").append(path).append("`\n\n");
                list.stream().sorted(java.util.Comparator.comparingInt(com.codeclear.review.CCSuppressedFinding::line))
                        .forEach(x -> b.append("**Line ").append(x.line()).append("** · `").append(x.ruleId())
                                .append("` · suppressed\n\n"));
            });
        }

        notChecked(b, r);

        if (!r.notes().isEmpty()) {
            b.append("## Notes\n\n");
            r.notes().forEach(n -> b.append("- ").append(escape(n)).append('\n'));
            b.append('\n');
        }
        return b.toString();
    }

    private static void summary(StringBuilder b, CCReviewResult r, int filesReviewed) {
        long notChecked = r.skippedRules().size();
        b.append("| | |\n|---|---|\n")
                .append("| Files reviewed | ").append(filesReviewed).append(" |\n")
                .append("| Rules run | ").append(r.rulesRun()).append(" |\n")
                .append("| Findings | ").append(r.findings().size()).append(" |\n")
                .append("| Unconfirmed candidates | ").append(r.unconfirmedCandidates().size()).append(" |\n")
                .append("| Suppressed | ").append(r.suppressed().size()).append(" |\n")
                .append("| Rules not checked | ").append(notChecked).append(" |\n")
                .append("| AI | ").append(escape(r.aiDriver() + " - " + r.aiStatus())).append(" |\n\n");
        if (r.findings().isEmpty()) {
            return;
        }
        Map<CCRuleSeverity, Integer> severities = new LinkedHashMap<>();
        for (CCRuleSeverity s : CCRuleSeverity.values()) {
            severities.put(s, 0);
        }
        Map<String, Integer> rules = new TreeMap<>();
        for (CCReviewFinding f : r.findings()) {
            severities.merge(f.severity(), 1, Integer::sum);
            rules.merge(f.ruleId(), 1, Integer::sum);
        }
        b.append("| Severity | Findings |\n|---|---|\n");
        severities.forEach((s, n) -> {
            if (n > 0) {
                b.append("| ").append(s).append(" | ").append(n).append(" |\n");
            }
        });
        b.append("\n| Rule | Findings |\n|---|---|\n");
        rules.forEach((rule, n) -> b.append("| `").append(rule).append("` | ").append(n).append(" |\n"));
        b.append('\n');
    }

    private static void notChecked(StringBuilder b, CCReviewResult r) {
        if (r.skippedRules().isEmpty()) {
            return;
        }
        b.append("## Not checked\n\nRules that apply to this change but did not run, so a clean result does not cover them.\n\n");
        for (CCSkippedRule.Reason reason : CCSkippedRule.Reason.values()) {
            List<CCSkippedRule> rules = r.skippedRules().stream().filter(s -> s.reason() == reason).toList();
            if (rules.isEmpty()) {
                continue;
            }
            b.append("<details><summary>").append(describe(reason)).append(" (").append(rules.size()).append(")</summary>\n\n");
            rules.forEach(s -> b.append("- `").append(s.ruleId()).append('`')
                    .append(reason == CCSkippedRule.Reason.FAILED ? ": " + escape(s.detail()) : "").append('\n'));
            b.append("\n</details>\n\n");
        }
    }

    private static String describe(CCSkippedRule.Reason reason) {
        return switch (reason) {
            case NEEDS_AI -> "Need the AI";
            case NO_DETECTOR -> "No detector written yet";
            case NO_ANALYZER -> "Need an analyzer for another language";
            case FAILED -> "Failed to run";
        };
    }

    /** The flagged line with {@link #CONTEXT} lines either side, numbered, the flagged one marked with {@code >}. */
    static void snippet(StringBuilder b, String source, int line, String path) {
        if (source == null) {
            return;
        }
        String[] lines = source.split("\r?\n", -1);
        int count = lines.length > 0 && lines[lines.length - 1].isEmpty() ? lines.length - 1 : lines.length;
        if (line < 1 || line > count) {
            return;
        }
        int from = Math.max(1, line - CONTEXT);
        int to = Math.min(count, line + CONTEXT);
        int width = String.valueOf(to).length();
        int longest = 0;
        for (int i = from; i <= to; i++) {
            Matcher m = BACKTICKS.matcher(lines[i - 1]);
            while (m.find()) {
                longest = Math.max(longest, m.group().length());
            }
        }
        String fence = "`".repeat(Math.max(3, longest + 1));
        b.append(fence).append(language(path)).append('\n');
        for (int i = from; i <= to; i++) {
            b.append(i == line ? "> " : "  ").append(String.format("%" + width + "d", i)).append(" | ")
                    .append(lines[i - 1].stripTrailing()).append('\n');
        }
        b.append(fence).append("\n\n");
    }

    private static String language(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        for (String[] known : new String[][]{{".java", "java"}, {".py", "python"}, {".ts", "typescript"}, {".tsx", "tsx"},
                {".js", "javascript"}, {".go", "go"}, {".sql", "sql"}, {".yml", "yaml"}, {".yaml", "yaml"}}) {
            if (p.endsWith(known[0])) {
                return known[1];
            }
        }
        return "";
    }

    /** Keeps a message from breaking the table or the page: one line, no raw HTML, no table separators. */
    static String escape(String text) {
        return text.replace('\n', ' ').replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;");
    }
}
