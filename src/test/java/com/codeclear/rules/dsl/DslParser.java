package com.codeclear.rules.dsl;

import com.codeclear.rules.dsl.RuleAst.Clause;
import com.codeclear.rules.dsl.RuleAst.Flag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Lexer and recursive-descent parser. Layout-insensitive: clause keywords delimit clauses. */
final class DslParser {

    enum K { ID, STR, RX, SYM, EOF }

    record Tok(K k, String t, int pos) { }

    private static final Set<String> RESERVED =
            Set.of("rule", "for", "where", "unless", "require", "let", "flag", "at", "when", "confidence");

    private final String src;
    private final List<Tok> toks;
    private int i;

    private DslParser(String src) {
        this.src = src;
        this.toks = lex(src);
    }

    static RuleAst parse(String src) {
        return new DslParser(src).rule();
    }

    static Expr parseExpr(String src) {
        DslParser p = new DslParser(src);
        Expr e = p.expr();
        if (p.peek().k() != K.EOF) {
            throw p.err("unexpected '" + p.peek().t() + "'");
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
            } else if (c == '#') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '"') {
                int start = i++;
                StringBuilder b = new StringBuilder();
                while (i < n && s.charAt(i) != '"') {
                    if (s.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    b.append(s.charAt(i++));
                }
                if (i >= n) {
                    throw new IllegalArgumentException("unterminated string at " + start);
                }
                i++;
                out.add(new Tok(K.STR, b.toString(), start));
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
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_'
                        || (s.charAt(i) == '-' && i + 1 < n && Character.isLetter(s.charAt(i + 1))))) {
                    i++;
                }
                out.add(new Tok(K.ID, s.substring(start, i), start));
            } else {
                String two = i + 1 < n ? s.substring(i, i + 2) : "";
                if (two.equals("=>") || two.equals("==") || two.equals("!=")) {
                    out.add(new Tok(K.SYM, two, i));
                    i += 2;
                } else if ("()[],.:=?".indexOf(c) >= 0) {
                    out.add(new Tok(K.SYM, String.valueOf(c), i++));
                } else {
                    throw new IllegalArgumentException("unexpected character '" + c + "' at " + i);
                }
            }
        }
        out.add(new Tok(K.EOF, "", n));
        return out;
    }

    // ---- helpers --------------------------------------------------------------------------------

    private Tok peek() {
        return toks.get(i);
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
        if (!acceptId("rule")) {
            throw err("a rule starts with 'rule <id>'");
        }
        String id = ident();
        if (!acceptId("for")) {
            throw err("expected 'for <var>: <Kind>'");
        }
        String var = ident();
        expectSym(":");
        String kind = ident();
        List<Clause> clauses = new ArrayList<>();
        List<Flag> flags = new ArrayList<>();
        while (peek().k() != K.EOF) {
            if (acceptId("where")) {
                clauses.add(new Clause.Where(expr()));
            } else if (acceptId("unless")) {
                clauses.add(new Clause.Unless(expr()));
            } else if (acceptId("require")) {
                clauses.add(new Clause.Require(expr()));
            } else if (acceptId("let")) {
                String name = ident();
                String param = null;
                if (acceptSym("(")) {
                    param = ident();
                    expectSym(")");
                }
                expectSym("=");
                clauses.add(new Clause.Let(name, param, expr()));
            } else if (acceptId("flag")) {
                flags.add(flag());
            } else {
                throw err("expected a clause (where, unless, require, let, flag)");
            }
        }
        if (flags.isEmpty()) {
            throw err("a rule needs at least one flag");
        }
        return new RuleAst(id, var, kind, List.copyOf(clauses), List.copyOf(flags));
    }

    private Flag flag() {
        if (!acceptId("at")) {
            throw err("expected 'at <node>'");
        }
        Expr at = expr();
        Expr when = acceptId("when") ? expr() : null;
        Expr confidence = acceptId("confidence") ? expr() : null;
        if (peek().k() != K.STR) {
            throw err("expected a message string");
        }
        return new Flag(at, when, confidence, message(next().t()));
    }

    private static List<Object> message(String s) {
        List<Object> parts = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int open = s.indexOf('{', i);
            if (open < 0) {
                parts.add(s.substring(i));
                break;
            }
            int close = s.indexOf('}', open);
            if (close < 0) {
                throw new IllegalArgumentException("unterminated { in message: " + s);
            }
            if (open > i) {
                parts.add(s.substring(i, open));
            }
            parts.add(parseExpr(s.substring(open + 1, close)));
            i = close + 1;
        }
        return parts;
    }

    // ---- expressions ----------------------------------------------------------------------------

    private Expr expr() {
        Expr c = implies();
        if (acceptSym("?")) {
            Expr a = expr();
            expectSym(":");
            Expr b = expr();
            return new Expr.Ternary(c, a, b);
        }
        return c;
    }

    private Expr implies() {
        Expr l = or();
        return acceptSym("=>") ? new Expr.Implies(l, implies()) : l;
    }

    private Expr or() {
        Expr l = and();
        while (acceptId("or")) {
            l = new Expr.Or(l, and());
        }
        return l;
    }

    private Expr and() {
        Expr l = not();
        while (acceptId("and")) {
            l = new Expr.And(l, not());
        }
        return l;
    }

    private Expr not() {
        return acceptId("not") ? new Expr.Not(not()) : cmp();
    }

    private Expr cmp() {
        Expr l = postfix();
        if (acceptId("in")) {
            return new Expr.In(l, postfix());
        }
        if (acceptId("matches")) {
            if (peek().k() != K.RX) {
                throw err("expected /regex/ after 'matches'");
            }
            String src = next().t();
            return new Expr.Matches(l, new Expr.Rx(src, Pattern.compile(src)));
        }
        if (acceptId("is")) {
            boolean neg = acceptId("not");
            if (acceptId("empty")) {
                return new Expr.IsEmpty(l, neg);
            }
            return new Expr.IsKind(l, ident(), neg);
        }
        if (acceptId("exists")) {
            return new Expr.Exists(l);
        }
        if (acceptId("inside")) {
            return new Expr.Inside(l, ident());
        }
        if (isId("all") || isId("any")) {
            boolean all = next().t().equals("all");
            return new Expr.Quant(all, l, ident());
        }
        if (isSym("==") || isSym("!=")) {
            boolean neg = next().t().equals("!=");
            return new Expr.Eq(l, postfix(), neg);
        }
        return l;
    }

    private Expr postfix() {
        Expr e = primary();
        while (acceptSym(".")) {
            String name = ident();
            e = isSym("(") ? new Expr.FactCall(e, name, args()) : new Expr.Field(e, name);
        }
        return e;
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
                if (RESERVED.contains(t.t())) {
                    throw err("'" + t.t() + "' is a keyword");
                }
                i++;
                if (t.t().equals("true") || t.t().equals("false")) {
                    return new Expr.Bool(t.t().equals("true"));
                }
                return isSym("(") ? new Expr.Apply(t.t(), args()) : new Expr.Id(t.t());
            }
            default -> throw err("expected an expression");
        }
    }
}
