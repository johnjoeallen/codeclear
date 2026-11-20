package com.codeclear.cli;

import com.codeclear.review.CCReviewFile;
import com.codeclear.review.CCReviewFinding;
import com.codeclear.review.CCReviewRequest;
import com.codeclear.review.CCReviewResult;
import com.codeclear.review.CCReviewService;
import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCRuleSeverity;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The {@code review} command: reads files, runs a review and prints the result. Kept free of Spring so it can be tested
 * with a plain {@link CCReviewService}.
 *
 * <p>Exit codes: 0 reviewed (and nothing at or above {@code --fail-on}), 1 findings at or above {@code --fail-on},
 * 2 bad usage or an I/O problem.
 */
public final class CCReviewCli {

    public static final int OK = 0;
    public static final int FINDINGS = 1;
    public static final int USAGE = 2;

    static final String USAGE_TEXT = """
            Usage: codeclear [options] [path ...]

            Reviews Java and SQL files with the rules in the rules folder. A path is a file or a folder; a folder is
            searched for .java and .sql files. The default is the current folder.

            Options (written --name=value):
              --diff=<rev>        review only the lines changed since a git revision (for example main or HEAD~1)
              --format=<text|json>  output format (default text)
              --fail-on=<level>   exit 1 when a finding has this severity or worse: critical, high, medium, low, info
                                  (default never)
              --config=<key=value>  a project configuration fact for cross-file rules, repeatable
                                  (for example --config=spring.jpa.open-in-view=false)
              --rules=<dir>       the rules folder (default: ./review; the launcher passes the one next to it)
              --show-skipped      list every rule that did not run, with the reason
              --report=<file>     write a Markdown report with the code around each finding
                                  (default target/codeclear/report.md inside the scanned folder;
                                  --report=none turns it off)
              --help              show this text

            Exit codes: 0 done, 1 findings at or above --fail-on, 2 bad usage or an error.

            To start the web application instead, run: codeclear serve
            """;

    private static final Set<String> SKIPPED_DIRS = Set.of(".git", "target", "build", "node_modules", "out", ".idea", ".gradle");

    /** Builds the review service for a rules folder. Called once the options are valid, so bad usage never loads rules. */
    @FunctionalInterface
    public interface ServiceFactory {
        CCReviewService create(Path rulesDir);
    }

    private final ServiceFactory factory;

    public CCReviewCli(ServiceFactory factory) {
        this.factory = factory;
    }

    /**
     * @param args first element is {@code review}
     * @param dir  the working directory: relative paths and the git repository are resolved against it
     */
    public int run(String[] args, Path dir, PrintStream out, PrintStream err) {
        Map<String, List<String>> options = new LinkedHashMap<>();
        List<String> paths = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-h")) {
                options.computeIfAbsent("help", k -> new ArrayList<>());
            } else if (a.startsWith("--")) {
                int eq = a.indexOf('=');
                String name = eq < 0 ? a.substring(2) : a.substring(2, eq);
                options.computeIfAbsent(name, k -> new ArrayList<>()).add(eq < 0 ? "" : a.substring(eq + 1));
            } else {
                paths.add(a);
            }
        }
        if (options.containsKey("help")) {
            out.print(USAGE_TEXT);
            return OK;
        }
        for (String name : options.keySet()) {
            if (!Set.of("diff", "format", "fail-on", "config", "rules", "show-skipped", "report").contains(name)) {
                err.println("error: unknown option --" + name + "\n\n" + USAGE_TEXT);
                return USAGE;
            }
        }
        String format = last(options, "format", "text");
        if (!format.equals("text") && !format.equals("json")) {
            err.println("error: --format must be text or json");
            return USAGE;
        }
        CCRuleSeverity failOn = null;
        String failOption = last(options, "fail-on", "never");
        if (!failOption.equals("never")) {
            try {
                failOn = CCRuleSeverity.valueOf(failOption.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                err.println("error: --fail-on must be never, critical, high, medium, low or info");
                return USAGE;
            }
        }
        Map<String, String> config = new HashMap<>();
        for (String kv : options.getOrDefault("config", List.of())) {
            int eq = kv.indexOf('=');
            if (eq <= 0) {
                err.println("error: --config needs key=value, got '" + kv + "'");
                return USAGE;
            }
            config.put(kv.substring(0, eq), kv.substring(eq + 1));
        }

        List<CCReviewFile> files;
        try {
            files = collect(dir, paths.isEmpty() ? List.of(".") : paths, options.containsKey("diff") ? last(options, "diff", "") : null);
        } catch (IOException | IllegalStateException e) {
            err.println("error: " + e.getMessage());
            return USAGE;
        }
        if (files.isEmpty()) {
            err.println("error: no files to review" + (options.containsKey("diff") ? " (nothing changed since " + last(options, "diff", "") + ")" : ""));
            return USAGE;
        }

        Path rulesDir = dir.resolve(last(options, "rules", "review")).normalize();
        CCReviewResult result;
        try (CCReviewService service = factory.create(rulesDir)) {
            result = service.review(new CCReviewRequest(files, config));
        } catch (IllegalArgumentException | IllegalStateException | java.io.UncheckedIOException e) {
            err.println("error: " + e.getMessage());
            return USAGE;
        }
        out.print(format.equals("json") ? CCReviewReport.json(result) + "\n" : CCReviewReport.text(result, options.containsKey("show-skipped")));
        writeReport(options.containsKey("report") ? dir.resolve(last(options, "report", "")).normalize() : scannedFolder(dir, paths).resolve(DEFAULT_REPORT).normalize(),
                dir, result, files, format.equals("json") ? err : out, err);
        if (failOn != null) {
            for (CCReviewFinding f : result.findings()) {
                if (f.severity().ordinal() <= failOn.ordinal()) {
                    return FINDINGS;
                }
            }
        }
        return OK;
    }

    /** Where the Markdown report goes unless --report says otherwise, relative to the folder that was scanned. */
    static final String DEFAULT_REPORT = "target/codeclear/report.md";

    /**
     * The folder the scanned sources are in: the named folder, the folder of a named file, or the closest folder that holds
     * everything when several are named. With no path, the working directory.
     */
    static Path scannedFolder(Path dir, List<String> paths) {
        Path common = null;
        for (String p : paths.isEmpty() ? List.of(".") : paths) {
            Path path = dir.resolve(p).toAbsolutePath().normalize();
            Path folder = Files.isDirectory(path) || path.getParent() == null ? path : path.getParent();
            if (common == null) {
                common = folder;
                continue;
            }
            while (!folder.startsWith(common) && common.getParent() != null) {
                common = common.getParent();
            }
        }
        return common;
    }

    /**
     * Writes the Markdown report and tells the reader where it is. A report that cannot be written is a warning, not a
     * failed review: the findings were already printed.
     */
    private static void writeReport(Path file, Path dir, CCReviewResult result, List<CCReviewFile> files,
                                    PrintStream notice, PrintStream err) {
        if (file.getFileName().toString().equals("none")) {
            return;
        }
        Map<String, String> sources = new HashMap<>();
        files.forEach(f -> sources.put(f.path(), f.content()));
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, CCMarkdownReport.render(result, sources, files.size()), StandardCharsets.UTF_8);
            Path shown = file.startsWith(dir.toAbsolutePath().normalize()) ? dir.toAbsolutePath().normalize().relativize(file) : file;
            notice.println("Report: " + shown.toString().replace('\\', '/'));
        } catch (IOException e) {
            err.println("warning: could not write the report to " + file + ": " + e.getMessage());
        }
    }

    private static String last(Map<String, List<String>> options, String name, String fallback) {
        List<String> values = options.get(name);
        return values == null || values.isEmpty() ? fallback : values.get(values.size() - 1);
    }

    /** The files to review: the named files, and the .java and .sql files under the named folders, limited to changed lines with a diff. */
    private static List<CCReviewFile> collect(Path dir, List<String> paths, String diffRev) throws IOException {
        Map<String, Set<Integer>> changed = diffRev == null ? null : CCGitDiff.changedLines(dir, diffRev);
        Map<String, Path> selected = new LinkedHashMap<>();
        for (String p : paths) {
            Path root = dir.resolve(p).normalize();
            if (Files.isRegularFile(root)) {
                selected.put(relative(dir, root), root);
            } else if (Files.isDirectory(root)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    for (Path f : walk.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".java") || f.toString().toLowerCase().endsWith(".sql"))
                            .filter(f -> !skipped(root, f)).sorted().toList()) {
                        selected.put(relative(dir, f), f);
                    }
                }
            } else {
                throw new IOException("no such file or folder: " + p);
            }
        }
        List<CCReviewFile> files = new ArrayList<>();
        for (Map.Entry<String, Path> e : selected.entrySet()) {
            CCChangedLines lines = CCChangedLines.all();
            if (changed != null) {
                Set<Integer> set = changed.get(e.getKey());
                if (set == null || set.isEmpty()) {
                    continue;
                }
                lines = CCChangedLines.of(set);
            }
            files.add(new CCReviewFile(e.getKey(), Files.readString(e.getValue(), StandardCharsets.UTF_8), lines));
        }
        return files;
    }

    private static boolean skipped(Path root, Path file) {
        for (Path part : root.relativize(file)) {
            if (SKIPPED_DIRS.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static String relative(Path dir, Path file) {
        return dir.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
