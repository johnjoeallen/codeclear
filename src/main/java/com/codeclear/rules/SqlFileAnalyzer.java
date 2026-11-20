package com.codeclear.rules;

import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a SQL file into the {@link CCSqlModel} the rules receive. It is a tokenizer, not a parser: it separates comments,
 * string literals, quoted identifiers and dollar-quoted bodies from code, splits the file into statements at {@code ;} and
 * at a {@code GO} line, and cuts an {@code ALTER TABLE} into the column definitions it adds. It does not know a dialect.
 */
public final class SqlFileAnalyzer {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:(?:GLOBAL\\s+|LOCAL\\s+)?(?:TEMP|TEMPORARY)\\s+|UNLOGGED\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([^\\s(]+)");
    private static final Pattern DML_TARGET = Pattern.compile(
            "(?is)^\\s*(?:UPDATE\\s+(?:ONLY\\s+)?|DELETE\\s+FROM\\s+(?:ONLY\\s+)?)([^\\s(]+)");
    private static final Pattern ALTER_TABLE = Pattern.compile(
            "(?is)^\\s*ALTER\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?(?:ONLY\\s+)?([^\\s(]+)\\s+(.*)$");
    private static final Pattern ADD = Pattern.compile(
            "(?is)^\\s*ADD\\s+(?:COLUMN\\s+)?(?:IF\\s+NOT\\s+EXISTS\\s+)?(.*)$");
    private static final Pattern NOT_A_COLUMN = Pattern.compile(
            "(?is)^\\s*(?:CONSTRAINT|PRIMARY|FOREIGN|UNIQUE|INDEX|KEY|CHECK|EXCLUDE|FULLTEXT|SPATIAL|PARTITION)\\b.*");
    private static final Pattern EXECUTE_VARIABLE = Pattern.compile("(?is)\\bEXEC(?:UTE)?\\s*(?:\\(\\s*)?(@\\w+)\\s*\\)?\\s*;?\\s*$");

    /** @throws IllegalArgumentException never today: any text is a SQL file of some kind */
    public CCFileAnalysis analyze(String path, String source, CCChangedLines changedLines) {
        Scan scan = new Scan(source);
        CCSqlModel model = build(scan);
        return new CCFileAnalysis(new CCFileInfo(path, "sql"), new CompilationUnit(), null, changedLines, model);
    }

    // ---- scanning --------------------------------------------------------------------------------------

    /** The file with comments blanked ({@code text}) and also string contents blanked ({@code code}), same length, same lines. */
    private static final class Scan {
        final String raw;
        final char[] text;
        final char[] code;
        final List<int[]> statements = new ArrayList<>();   // start, end (exclusive) offsets of each statement
        final List<String> commentBefore = new ArrayList<>();
        final int[] lineOf;

        Scan(String raw) {
            this.raw = raw.replace("\r\n", "\n").replace('\r', '\n');
            int n = this.raw.length();
            text = this.raw.toCharArray();
            code = this.raw.toCharArray();
            lineOf = new int[n + 1];
            int line = 1;
            for (int i = 0; i < n; i++) {
                lineOf[i] = line;
                if (this.raw.charAt(i) == '\n') {
                    line++;
                }
            }
            lineOf[n] = line;
            run();
        }

        private void blank(char[] a, int from, int to) {
            for (int i = from; i < to; i++) {
                if (a[i] != '\n') {
                    a[i] = ' ';
                }
            }
        }

        private void run() {
            int n = raw.length();
            int i = 0;
            int stmtStart = -1;
            String openTag = null;
            StringBuilder comments = new StringBuilder();
            while (i < n) {
                char c = raw.charAt(i);
                if (c == '-' && raw.startsWith("--", i)) {
                    int end = raw.indexOf('\n', i);
                    end = end < 0 ? n : end;
                    comments.append(raw, i + 2, end).append('\n');
                    blank(text, i, end);
                    blank(code, i, end);
                    i = end;
                } else if (c == '/' && raw.startsWith("/*", i)) {
                    int end = raw.indexOf("*/", i + 2);
                    end = end < 0 ? n : end + 2;
                    comments.append(raw, i + 2, Math.max(i + 2, end - 2)).append('\n');
                    blank(text, i, end);
                    blank(code, i, end);
                    i = end;
                } else if (c == '\'' || c == '"' || c == '`') {
                    stmtStart = begin(stmtStart, i, comments);
                    i = quoted(i, c);
                } else if (c == '[' && stmtStart >= 0) {
                    int end = raw.indexOf(']', i);
                    int stop = end < 0 ? n : end + 1;
                    blank(code, i + 1, Math.max(i + 1, stop - 1));
                    i = stop;
                } else if (c == '$' && dollarTag(i) != null) {
                    stmtStart = begin(stmtStart, i, comments);
                    // A body between two equal tags is code too, but its semicolons do not end the statement.
                    String tag = dollarTag(i);
                    if (tag.equals(openTag)) {
                        openTag = null;
                    } else if (openTag == null) {
                        openTag = tag;
                    }
                    i += tag.length();
                } else if (c == ';' && openTag == null) {
                    if (stmtStart >= 0) {
                        statements.add(new int[] {stmtStart, i});
                        stmtStart = -1;
                    }
                    i++;
                } else if (openTag == null && isGoLine(i)) {
                    if (stmtStart >= 0) {
                        statements.add(new int[] {stmtStart, i});
                        stmtStart = -1;
                    }
                    i = raw.indexOf('\n', i) < 0 ? n : raw.indexOf('\n', i);
                } else {
                    if (!Character.isWhitespace(c)) {
                        stmtStart = begin(stmtStart, i, comments);
                    }
                    i++;
                }
            }
            if (stmtStart >= 0) {
                statements.add(new int[] {stmtStart, n});
            }
        }

        /** The start of a statement: remember the comments seen since the last one. */
        private int begin(int stmtStart, int at, StringBuilder comments) {
            if (stmtStart >= 0) {
                return stmtStart;
            }
            commentBefore.add(comments.toString());
            comments.setLength(0);
            return at;
        }

        private int quoted(int from, char q) {
            int n = raw.length();
            int i = from + 1;
            while (i < n) {
                if (raw.charAt(i) == q) {
                    if (i + 1 < n && raw.charAt(i + 1) == q) {
                        i += 2;
                        continue;
                    }
                    break;
                }
                i++;
            }
            blank(code, from + 1, Math.min(i, n));
            return Math.min(i + 1, n);
        }

        /** {@code $$} or {@code $tag$} that opens a dollar-quoted body, else null. */
        private String dollarTag(int at) {
            Matcher m = Pattern.compile("\\$(?:[A-Za-z_][A-Za-z_0-9]*)?\\$").matcher(raw);
            m.region(at, raw.length());
            if (m.lookingAt() && (at == 0 || !Character.isLetterOrDigit(raw.charAt(at - 1)))) {
                return m.group();
            }
            return null;
        }

        /** A line that holds only {@code GO}: the T-SQL batch separator. */
        private boolean isGoLine(int at) {
            if (at > 0 && raw.charAt(at - 1) != '\n' && !raw.substring(raw.lastIndexOf('\n', at - 1) + 1, at).isBlank()) {
                return false;
            }
            int end = raw.indexOf('\n', at);
            String line = raw.substring(at, end < 0 ? raw.length() : end);
            return line.strip().equalsIgnoreCase("GO");
        }
    }

    // ---- model -----------------------------------------------------------------------------------------

    private static CCSqlModel build(Scan s) {
        Set<String> created = new HashSet<>();
        for (int[] range : s.statements) {
            Matcher m = CREATE_TABLE.matcher(new String(s.code, range[0], range[1] - range[0]));
            if (m.find()) {
                created.add(normalise(m.group(1)));
                created.add(unqualified(normalise(m.group(1))));
            }
        }
        List<CCSqlModel.Statement> statements = new ArrayList<>();
        List<CCSqlModel.Column> columns = new ArrayList<>();
        List<CCSqlModel.Line> lines = new ArrayList<>();
        String fileCode = new String(s.code);
        for (int k = 0; k < s.statements.size(); k++) {
            int start = s.statements.get(k)[0];
            int end = s.statements.get(k)[1];
            String text = new String(s.text, start, end - start).strip();
            String code = new String(s.code, start, end - start).strip();
            String target = target(code);
            boolean createdHere = target != null && (created.contains(target) || created.contains(unqualified(target)));
            statements.add(new CCSqlModel.Statement(s.lineOf[start], text, code, s.commentBefore.get(k).strip(),
                    topLevelWhere(code), createdHere));
            columns.addAll(columns(s, start, end, created));
            addLines(s, start, end, code, fileCode, lines);
        }
        return new CCSqlModel(List.copyOf(statements), List.copyOf(columns), List.copyOf(lines));
    }

    private static String target(String code) {
        Matcher m = DML_TARGET.matcher(code);
        if (m.find()) {
            return normalise(m.group(1));
        }
        Matcher a = ALTER_TABLE.matcher(code);
        return a.matches() ? normalise(a.group(1)) : null;
    }

    private static String normalise(String name) {
        return name.replaceAll("[\"`\\[\\]]", "").toLowerCase(Locale.ROOT);
    }

    private static String unqualified(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    /** {@code WHERE} as a word outside parentheses. */
    private static boolean topLevelWhere(String code) {
        int depth = 0;
        Matcher words = Pattern.compile("[()]|\\b[A-Za-z_]+\\b").matcher(code);
        while (words.find()) {
            String w = words.group();
            if (w.equals("(")) {
                depth++;
            } else if (w.equals(")")) {
                depth--;
            } else if (depth == 0 && w.equalsIgnoreCase("WHERE")) {
                return true;
            }
        }
        return false;
    }

    /** The definitions added by one {@code ALTER TABLE}: its top-level comma-separated parts that start with {@code ADD}. */
    private static List<CCSqlModel.Column> columns(Scan s, int start, int end, Set<String> created) {
        String code = new String(s.code, start, end - start);
        Matcher alter = ALTER_TABLE.matcher(code.strip());
        if (!alter.matches()) {
            return List.of();
        }
        String table = normalise(alter.group(1));
        boolean createdHere = created.contains(table) || created.contains(unqualified(table));
        int bodyStart = start + (code.length() - code.stripLeading().length()) + alter.start(2);
        List<CCSqlModel.Column> out = new ArrayList<>();
        int depth = 0;
        int partStart = bodyStart;
        for (int i = bodyStart; i <= end; i++) {
            char c = i < end ? s.code[i] : ',';
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                addColumn(s, partStart, i, createdHere, out);
                partStart = i + 1;
            }
        }
        return out;
    }

    private static void addColumn(Scan s, int from, int to, boolean createdHere, List<CCSqlModel.Column> out) {
        String code = new String(s.code, from, to - from);
        Matcher add = ADD.matcher(code);
        if (!add.matches() || NOT_A_COLUMN.matcher(add.group(1)).matches()) {
            return;
        }
        int offset = from + code.indexOf(add.group(1));
        String definition = new String(s.text, offset, Math.max(0, to - offset)).strip();
        String name = definition.split("[\\s(]+", 2)[0].replaceAll("[\"`\\[\\]]", "");
        int first = offset;
        while (first < to && Character.isWhitespace(s.code[first])) {
            first++;
        }
        out.add(new CCSqlModel.Column(s.lineOf[Math.min(first, s.lineOf.length - 1)], name, definition,
                new String(s.code, offset, Math.max(0, to - offset)).strip(), createdHere));
    }

    private static void addLines(Scan s, int start, int end, String statementCode, String fileCode, List<CCSqlModel.Line> out) {
        int i = start;
        while (i < end) {
            int nl = s.raw.indexOf('\n', i);
            int stop = nl < 0 || nl > end ? end : nl;
            String text = new String(s.text, i, stop - i).strip();
            String code = new String(s.code, i, stop - i).strip();
            if (!text.isEmpty()) {
                out.add(new CCSqlModel.Line(s.lineOf[i], text, code, statementCode, executesBuiltString(code, fileCode)));
            }
            i = stop + 1;
        }
    }

    /** {@code EXEC @sql} or {@code EXEC(@sql)} where the file assigns {@code @sql} from a concatenation. */
    private static boolean executesBuiltString(String code, String fileCode) {
        Matcher m = EXECUTE_VARIABLE.matcher(code);
        if (!m.find()) {
            return false;
        }
        String v = Pattern.quote(m.group(1));
        // Another variable on either side of a + or ||: the variable's own name does not count, so appending constants is fine.
        String other = "@(?!" + Pattern.quote(m.group(1).substring(1)) + "\\b)\\w+";
        return Pattern.compile("(?is)" + v + "\\s*\\+?=\\s*[^;=]*(?:(?:\\+|\\|\\|)[^;=]*" + other + "|" + other + "[^;=]*(?:\\+|\\|\\|))")
                .matcher(fileCode).find();
    }
}
