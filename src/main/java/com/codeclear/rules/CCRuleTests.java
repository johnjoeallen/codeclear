package com.codeclear.rules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a rule's {@code tests.md}: test cases written as Markdown that a person can review and an AI can generate.
 *
 * <pre>
 * # Tests: java/empty-catch-block
 *
 * ## Prelude                                   optional; declarations placed before every source
 * ```java
 * import java.util.*;
 * ```
 *
 * ## Case: empty block                         one case per heading
 * covers: example:flag, required:empty-block   labels tying the case to a section of rule.md
 * config: spring.jpa.open-in-view=false        optional, repeatable
 * ```java path=src/main/java/A.java            a source file; repeat for a project of several files
 * class A { void f() { try { x(); } catch (Exception e) {} } }   // FLAG
 * ```
 * ```java prelude                              optional; replaces the shared prelude for this case
 * ...
 * ```
 *
 * ## Mutations                                 optional bullets: `flagged case` -&gt; `fixed case`
 * </pre>
 *
 * <p>A source line ending in {@code // FLAG} must produce a finding on that line; {@code // FLAG LOW} also states the
 * confidence (otherwise the rule's own). A line ending in {@code // CANDIDATE} must produce a candidate for the AI to
 * confirm (what a hybrid rule reports). Lines without a marker must produce nothing. A case may say which lines the change
 * touches with {@code changed: 3,5-7}; findings elsewhere are dropped, as in a real review. Markers are stripped before the
 * source is analysed, so they never change what the rule sees.
 */
public final class CCRuleTests {

    /**
     * What the case requires: file, line within the source, confidence ({@code null}: the rule's own) and whether it is a
     * candidate for the AI instead of a finding.
     */
    public record Expect(String path, int line, String confidence, boolean candidate) { }

    public record SourceFile(String path, String source) { }

    /**
     * @param prelude      {@code null} to use the shared prelude
     * @param changedLines the lines the change touches, or {@code null} when every line counts as changed
     */
    public record Case(String name, List<String> covers, Map<String, String> config, String prelude,
                       List<SourceFile> files, List<Expect> expect, Set<Integer> changedLines) { }

    public record Mutation(String from, String to) { }

    public record Tests(String title, String sharedPrelude, List<Case> cases, List<Mutation> mutations) { }

    public static final String DEFAULT_PATH = "src/main/java/Test.java";

    private static final Pattern MARKER = Pattern.compile("\\s*(?://|--|#)\\s*(FLAG|CANDIDATE)(?:\\s+(LOW|MEDIUM|HIGH))?\\s*$");
    private static final Pattern MUTATION = Pattern.compile("^\\s*[-*]\\s*`(.+?)`\\s*(?:->|→)\\s*`(.+?)`\\s*$");

    private CCRuleTests() { }

    public static Tests load(Path ruleDir) {
        try {
            return parse(Files.readString(ruleDir.resolve("tests.md")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Tests parse(String markdown) {
        String title = "";
        String shared = "";
        List<Case> cases = new ArrayList<>();
        List<Mutation> mutations = new ArrayList<>();

        String section = "";            // "prelude", "case", "mutations" or ""
        CaseBuilder current = null;
        String[] lines = markdown.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("```")) {
                String info = line.substring(3).trim();
                StringBuilder body = new StringBuilder();
                List<String> bodyLines = new ArrayList<>();
                i++;
                while (i < lines.length && !lines[i].startsWith("```")) {
                    bodyLines.add(lines[i++]);
                }
                if (section.equals("prelude")) {
                    shared = String.join("\n", bodyLines);
                } else if (section.equals("case") && current != null) {
                    if (info.matches(".*\\bprelude\\b.*")) {
                        current.prelude = String.join("\n", bodyLines);
                    } else {
                        current.addFile(attribute(info, "path"), bodyLines);
                    }
                }
                continue;
            }
            if (line.startsWith("# ") && title.isEmpty()) {
                title = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                if (current != null) {
                    cases.add(current.build());
                    current = null;
                }
                String heading = line.substring(3).trim();
                if (heading.equalsIgnoreCase("Prelude")) {
                    section = "prelude";
                } else if (heading.equalsIgnoreCase("Mutations")) {
                    section = "mutations";
                } else if (heading.regionMatches(true, 0, "Case:", 0, 5)) {
                    section = "case";
                    current = new CaseBuilder(heading.substring(5).trim());
                } else {
                    section = "";
                }
            } else if (section.equals("case") && current != null) {
                if (line.startsWith("covers:")) {
                    for (String c : line.substring(7).split(",")) {
                        if (!c.isBlank()) {
                            current.covers.add(c.trim());
                        }
                    }
                } else if (line.startsWith("changed:")) {
                    current.changed = lines(line.substring(8));
                } else if (line.startsWith("config:")) {
                    String kv = line.substring(7).trim();
                    int eq = kv.indexOf('=');
                    if (eq > 0) {
                        current.config.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
                    }
                }
            } else if (section.equals("mutations")) {
                Matcher m = MUTATION.matcher(line);
                if (m.matches()) {
                    mutations.add(new Mutation(m.group(1), m.group(2)));
                }
            }
        }
        if (current != null) {
            cases.add(current.build());
        }
        return new Tests(title, shared, List.copyOf(cases), List.copyOf(mutations));
    }

    /** Line numbers written as {@code 3,5-7}. */
    private static Set<Integer> lines(String text) {
        Set<Integer> out = new java.util.TreeSet<>();
        for (String part : text.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int dash = p.indexOf('-');
            if (dash < 0) {
                out.add(Integer.parseInt(p));
            } else {
                for (int i = Integer.parseInt(p.substring(0, dash).trim()); i <= Integer.parseInt(p.substring(dash + 1).trim()); i++) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    private static String attribute(String info, String name) {
        Matcher m = Pattern.compile("\\b" + name + "=(\\S+)").matcher(info);
        return m.find() ? m.group(1) : DEFAULT_PATH;
    }

    /** Text handed to the analyser for one file: the prelude on its own line(s), then the source. */
    public static String analysisText(String prelude, String source) {
        return prelude + "\n" + source;
    }

    /** Number of lines the prelude adds, to subtract from reported line numbers. */
    public static int lineOffset(String prelude) {
        return (int) prelude.chars().filter(ch -> ch == '\n').count() + 1;
    }

    private static final class CaseBuilder {
        final String name;
        final List<String> covers = new ArrayList<>();
        final Map<String, String> config = new LinkedHashMap<>();
        String prelude;
        Set<Integer> changed;
        final List<SourceFile> files = new ArrayList<>();
        final List<Expect> expect = new ArrayList<>();

        CaseBuilder(String name) {
            this.name = name;
        }

        void addFile(String path, List<String> lines) {
            StringBuilder source = new StringBuilder();
            for (int n = 1; n <= lines.size(); n++) {
                String text = lines.get(n - 1);
                Matcher m = MARKER.matcher(text);
                if (m.find()) {
                    expect.add(new Expect(path, n, m.group(2), m.group(1).equals("CANDIDATE")));
                    text = text.substring(0, m.start());
                }
                source.append(text).append('\n');
            }
            files.add(new SourceFile(path, source.toString()));
        }

        Case build() {
            return new Case(name, List.copyOf(covers), Map.copyOf(config), prelude, List.copyOf(files),
                    List.copyOf(expect), changed);
        }
    }
}
