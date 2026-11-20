package com.codeclear.rules.dsl;

import com.codeclear.rules.dsl.RuleAst.Clause;
import com.codeclear.rules.dsl.RuleAst.Flag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lexer and recursive-descent parser for the rule language, whose surface syntax follows Groovy so that a developer
 * can read a rule without learning a new notation. It is a custom parser: the text is never handed to Groovy, and
 * only the constructs below exist.
 *
 * <pre>
 * rule 'empty-catch-block'
 *
 * def quiet = { s -> s instanceof ExprStmt &amp;&amp; s.expression.name in ['debug', 'trace'] }
 *
 * Catch.each { c ->
 *     only c.param.name in ['ignored']          // keep the node when true
 *     skip file.isTest || c.body.hasComment     // drop the node when true
 *     def n = c.body.statements                 // a per-node value
 *     report c, "Empty catch hides ${c.param.type}", c.body.statements.isEmpty()
 * }
 * </pre>
 *
 * <p>Layout does not matter: keywords delimit statements, and an expression continues over line breaks. Filters
 * ({@code only}, {@code skip}, {@code def}) must come before the first {@code report}. A {@code report} or
 * {@code candidate} line is {@code <node>, "<message>"} with an optional condition and an optional
 * {@code confidence: <LOW|MEDIUM|HIGH|DEFAULT>}; the first one whose condition holds fires, at most once per node.
 */
final class DslParser {

    enum K { ID, STR, GSTR, RX, SYM, NUM, EOF }

    record Tok(K k, String t, int pos) { }

    /** Constructs of general-purpose languages that rules deliberately do not have. */
    private static final Set<String> UNSUPPORTED = Set.of("if", "else", "for", "while", "do", "return", "new", "class",
            "import", "switch", "try", "catch", "throw", "break", "continue");

    private static final Set<String> CONSTANTS = Set.of("DEFAULT", "LOW", "MEDIUM", "HIGH", "PRESENT");

    /** The literal {@code null}, only meaningful on one side of {@code ==} or {@code !=}. */
    private static final Expr NULL = new Expr.Id("null");

    private final String src;
    private final List<Tok> toks;
    private int i;
    private int closures;
    private final List<Clause> hoisted = new ArrayList<>();

    private DslParser(String src) {
        this.src = src;
        this.toks = lex(src);
    }

    static RuleAst parse(String src) {
        return new DslParser(src).rule();
    }

    /** Parses one expression, for the {@code ${...}} parts of a message. */
    static Expr parseExpr(String src) {
        DslParser p = new DslParser(src);
        Expr e = p.expr();
        if (p.peek().k() != K.EOF) {
            throw p.err("unexpected '" + p.peek().t() + "'");
        }
        if (!p.hoisted.isEmpty()) {
            throw p.err("a closure cannot appear inside a message");
        }
        return e;
    }

    // ---- lexer ----------------------------------------------------------------------------------

    private static List<Tok> lex(String s) {
        List<Tok> out = new ArrayList<>();
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '#' || (c == '/' && i + 1 < n && s.charAt(i + 1) == '/')) {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end < 0) {
                    throw new IllegalArgumentException("unterminated comment at " + i);
                }
                i = end + 2;
            } else if (c == '"' || c == '\'') {
                int start = i++;
                StringBuilder b = new StringBuilder();
                while (i < n && s.charAt(i) != c) {
                    if (s.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    b.append(s.charAt(i++));
                }
                if (i >= n) {
                    throw new IllegalArgumentException("unterminated string at " + start);
                }
                i++;
                out.add(new Tok(c == '"' ? K.GSTR : K.STR, b.toString(), start));
            } else if (c == '/') {
                int start = i++;
                StringBuilder b = new StringBuilder();
                while (i < n && s.charAt(i) != '/') {
                    if (s.charAt(i) == '\\' && i + 1 < n && s.charAt(i + 1) == '/') {
                        i++;
                    } else if (s.charAt(i) == '\\' && i + 1 < n) {
                        b.append(s.charAt(i++));
                    }
                    b.append(s.charAt(i++));
                }
                if (i >= n) {
                    throw new IllegalArgumentException("unterminated regex at " + start);
                }
                i++;
                out.add(new Tok(K.RX, b.toString(), start));
            } else if (Character.isDigit(c)) {
                int start = i;
                while (i < n && Character.isDigit(s.charAt(i))) {
                    i++;
                }
                out.add(new Tok(K.NUM, s.substring(start, i), start));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
                    i++;
                }
                out.add(new Tok(K.ID, s.substring(start, i), start));
            } else if (s.startsWith("==~", i)) {
                out.add(new Tok(K.SYM, "==~", i));
                i += 3;
            } else if (i + 1 < n && List.of("&&", "||", "==", "!=", "->", "?.").contains(s.substring(i, i + 2))) {
                out.add(new Tok(K.SYM, s.substring(i, i + 2), i));
                i += 2;
            } else if ("()[]{},.:?!=".indexOf(c) >= 0) {
                out.add(new Tok(K.SYM, String.valueOf(c), i++));
            } else {
                throw new IllegalArgumentException("unexpected character '" + c + "' at " + i);
            }
        }
        out.add(new Tok(K.EOF, "", n));
        return out;
    }

    // ---- helpers --------------------------------------------------------------------------------

    private Tok peek() {
        return toks.get(i);
    }

    private Tok peekAt(int ahead) {
        return toks.get(Math.min(i + ahead, toks.size() - 1));
    }

    private Tok next() {
        return toks.get(i++);
    }

    private boolean isId(String w) {
        return peek().k() == K.ID && peek().t().equals(w);
    }

    private boolean isSym(String s) {
        return peek().k() == K.SYM && peek().t().equals(s);
    }

    private boolean acceptId(String w) {
        if (isId(w)) {
            i++;
            return true;
        }
        return false;
    }

    private boolean acceptSym(String s) {
        if (isSym(s)) {
            i++;
            return true;
        }
        return false;
    }

    private void expectSym(String s) {
        if (!acceptSym(s)) {
            throw err("expected '" + s + "'");
        }
    }

    private void expectId(String w) {
        if (!acceptId(w)) {
            throw err("expected '" + w + "'");
        }
    }

    private String ident() {
        if (peek().k() != K.ID) {
            throw err("expected a name");
        }
        return next().t();
    }

    private IllegalArgumentException err(String msg) {
        Tok t = peek();
        int line = (int) src.substring(0, Math.min(t.pos(), src.length())).chars().filter(c -> c == '\n').count() + 1;
        return new IllegalArgumentException("line " + line + ": " + msg + " (at '" + t.t() + "')");
    }

    // ---- rule -----------------------------------------------------------------------------------

    private RuleAst rule() {
        expectId("rule");
        if (peek().k() != K.STR && peek().k() != K.GSTR) {
            throw err("a rule starts with rule 'name'");
        }
        String id = next().t();
        List<Clause> clauses = new ArrayList<>();
        while (isId("def")) {
            clauses.add(def());
        }
        String kind = ident();
        expectSym(".");
        expectId("each");
        expectSym("{");
        String var = ident();
        expectSym("->");

        List<Flag> flags = new ArrayList<>();
        while (!isSym("}")) {
            if (acceptId("only")) {
                requireBeforeReports(flags, "only");
                clauses.add(new Clause.Where(expr()));
            } else if (acceptId("skip")) {
                requireBeforeReports(flags, "skip");
                clauses.add(new Clause.Unless(expr()));
            } else if (isId("def")) {
                requireBeforeReports(flags, "def");
                clauses.add(def());
            } else if (acceptId("report")) {
                flags.add(report(false));
            } else if (acceptId("candidate")) {
                flags.add(report(true));
            } else if (peek().k() == K.ID && UNSUPPORTED.contains(peek().t())) {
                throw err("'" + peek().t() + "' is not supported: a rule has no loops, conditionals or code blocks;"
                        + " use only, skip, any { } or every { }");
            } else if (peek().k() == K.EOF) {
                throw err("expected '}' to close the rule");
            } else {
                throw err("expected only, skip, def, report or candidate");
            }
        }
        expectSym("}");
        if (peek().k() != K.EOF) {
            throw err("nothing may follow the rule");
        }
        if (flags.isEmpty()) {
            throw err("a rule needs at least one report");
        }
        List<Clause> all = new ArrayList<>(hoisted);
        all.addAll(clauses);
        return new RuleAst(id, var, kind, List.copyOf(all), List.copyOf(flags));
    }

    private void requireBeforeReports(List<Flag> flags, String keyword) {
        if (!flags.isEmpty()) {
            throw err("'" + keyword + "' must come before the first report");
        }
    }

    /** {@code def name = value} or {@code def name = { p -> body }}. */
    private Clause def() {
        expectId("def");
        String name = ident();
        expectSym("=");
        if (isSym("{")) {
            Closure c = closure();
            return new Clause.Let(name, c.param(), c.body());
        }
        return new Clause.Let(name, null, expr());
    }

    private record Closure(String param, Expr body) { }

    /** {@code { p -> expression }}, or {@code { expression }} with the implicit parameter {@code it}. */
    private Closure closure() {
        expectSym("{");
        String param = "it";
        if (peek().k() == K.ID && peekAt(1).k() == K.SYM && peekAt(1).t().equals("->")) {
            param = next().t();
            next();
        }
        Expr body = expr();
        expectSym("}");
        return new Closure(param, body);
    }

    /** report &lt;node&gt;, "message" [, condition] [, confidence: X], with optional parentheses. */
    private Flag report(boolean candidate) {
        boolean paren = acceptSym("(");
        Expr at = expr();
        expectSym(",");
        if (peek().k() != K.STR && peek().k() != K.GSTR) {
            throw err("expected a message string");
        }
        Tok msg = next();
        Expr when = null;
        Expr confidence = null;
        while (acceptSym(",")) {
            if (isId("confidence") && peekAt(1).k() == K.SYM && peekAt(1).t().equals(":")) {
                next();
                next();
                confidence = expr();
            } else {
                if (when != null) {
                    throw err("a report has one condition");
                }
                when = expr();
            }
        }
        if (paren) {
            expectSym(")");
        }
        return new Flag(at, when, confidence, message(msg), candidate);
    }

    private static List<Object> message(Tok msg) {
        String s = msg.t();
        List<Object> parts = new ArrayList<>();
        if (msg.k() == K.STR) {
            parts.add(s);
            return parts;
        }
        int i = 0;
        while (i < s.length()) {
            int open = s.indexOf("${", i);
            if (open < 0) {
                parts.add(s.substring(i));
                break;
            }
            int close = s.indexOf('}', open);
            if (close < 0) {
                throw new IllegalArgumentException("unterminated ${ in message: " + s);
            }
            if (open > i) {
                parts.add(s.substring(i, open));
            }
            parts.add(parseExpr(s.substring(open + 2, close)));
            i = close + 1;
        }
        return parts;
    }

    // ---- expressions ----------------------------------------------------------------------------

    private Expr expr() {
        Expr c = or();
        if (acceptSym("?")) {
            Expr a = expr();
            expectSym(":");
            Expr b = expr();
            return new Expr.Ternary(c, a, b);
        }
        return c;
    }

    private Expr or() {
        Expr l = and();
        while (acceptSym("||")) {
            l = new Expr.Or(l, and());
        }
        return l;
    }

    private Expr and() {
        Expr l = not();
        while (acceptSym("&&")) {
            l = new Expr.And(l, not());
        }
        return l;
    }

    private Expr not() {
        return acceptSym("!") ? new Expr.Not(not()) : cmp();
    }

    private Expr cmp() {
        Expr l = postfix();
        if (acceptId("in")) {
            return new Expr.In(l, postfix());
        }
        if (acceptSym("==~")) {
            if (peek().k() != K.RX) {
                throw err("expected /regex/ after ==~");
            }
            String re = next().t();
            return new Expr.Matches(l, new Expr.Rx(re, Pattern.compile(re)));
        }
        if (acceptId("instanceof")) {
            return new Expr.IsKind(l, ident(), false);
        }
        if (isSym("==") || isSym("!=")) {
            boolean neg = next().t().equals("!=");
            Expr r = postfix();
            if (r == NULL) {
                return neg ? new Expr.Exists(l) : new Expr.Not(new Expr.Exists(l));
            }
            if (l == NULL) {
                throw err("write the value first: x == null");
            }
            return new Expr.Eq(l, r, neg);
        }
        return l;
    }

    /** Comparison methods that follow {@code x.size()} or a number: {@code xs.size().isGreaterThan(2)}. */
    private static final java.util.Map<String, String> COMPARISONS = java.util.Map.of(
            "isEqualTo", "==", "isNotEqualTo", "!=", "isNotEqual", "!=",
            "isGreaterThan", ">", "isGreaterThanOrEqualTo", ">=",
            "isLessThan", "<", "isLessThanOrEqualTo", "<=");

    private Expr postfix() {
        Expr e = primary();
        while (isSym(".") || isSym("?.")) {
            next();
            String name = ident();
            if (name.equals("isEmpty") && isSym("(")) {
                expectSym("(");
                expectSym(")");
                e = new Expr.IsEmpty(e, false);
            } else if (name.equals("size") && isSym("(")) {
                expectSym("(");
                expectSym(")");
                e = new Expr.Size(e);
            } else if (COMPARISONS.containsKey(name) && isSym("(")) {
                expectSym("(");
                Expr n = expr();
                expectSym(")");
                e = new Expr.Cmp(e, COMPARISONS.get(name), n);
            } else if (name.equals("every") || name.equals("any")) {
                e = quantifier(e, name.equals("every"));
            } else if (name.equals("inside") && isSym("(")) {
                expectSym("(");
                String kind = ident();
                expectSym(")");
                e = new Expr.Inside(e, kind);
            } else if (isSym("(")) {
                e = new Expr.FactCall(e, name, args());
            } else {
                e = new Expr.Field(e, name);
            }
        }
        return e;
    }

    /** {@code xs.any(pred)}, {@code xs.every(pred)} or the same with a closure; an inline closure becomes a named let. */
    private Expr quantifier(Expr list, boolean all) {
        String pred;
        if (isSym("{")) {
            pred = hoist(closure());
        } else {
            expectSym("(");
            pred = isSym("{") ? hoist(closure()) : ident();
            expectSym(")");
        }
        return new Expr.Quant(all, list, pred);
    }

    private String hoist(Closure c) {
        String name = "$closure" + (++closures);
        hoisted.add(new Clause.Let(name, c.param(), c.body()));
        return name;
    }

    private List<Expr> args() {
        expectSym("(");
        List<Expr> out = new ArrayList<>();
        if (!isSym(")")) {
            do {
                out.add(expr());
            } while (acceptSym(","));
        }
        expectSym(")");
        return out;
    }

    private Expr primary() {
        Tok t = peek();
        switch (t.k()) {
            case STR -> {
                i++;
                return new Expr.Str(t.t());
            }
            case NUM -> {
                i++;
                try {
                    return new Expr.Num(Long.parseLong(t.t()));
                } catch (NumberFormatException e) {
                    throw err("the number " + t.t() + " is too large");
                }
            }
            case GSTR -> {
                i++;
                if (t.t().contains("${")) {
                    throw err("${...} is only allowed in a report message");
                }
                return new Expr.Str(t.t());
            }
            case SYM -> {
                if (acceptSym("(")) {
                    Expr e = expr();
                    expectSym(")");
                    return e;
                }
                if (acceptSym("[")) {
                    List<Expr> items = new ArrayList<>();
                    if (!isSym("]")) {
                        do {
                            items.add(expr());
                        } while (acceptSym(","));
                    }
                    expectSym("]");
                    return new Expr.Lst(items);
                }
                throw err("unexpected symbol");
            }
            case ID -> {
                if (UNSUPPORTED.contains(t.t())) {
                    throw err("'" + t.t() + "' is not supported: a rule has no loops, conditionals or code blocks");
                }
                i++;
                switch (t.t()) {
                    case "true", "false" -> {
                        return new Expr.Bool(t.t().equals("true"));
                    }
                    case "null" -> {
                        return NULL;
                    }
                    default -> { }
                }
                if (CONSTANTS.contains(t.t())) {
                    return new Expr.Id(t.t().toLowerCase(java.util.Locale.ROOT));
                }
                return isSym("(") ? new Expr.Apply(t.t(), args()) : new Expr.Id(t.t());
            }
            default -> throw err("expected an expression");
        }
    }
}
