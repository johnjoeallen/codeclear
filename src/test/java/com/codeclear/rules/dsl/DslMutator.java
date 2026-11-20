package com.codeclear.rules.dsl;

import com.codeclear.rules.dsl.RuleAst.Clause;
import com.codeclear.rules.dsl.RuleAst.Flag;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates mutants of a rule automatically. Possible only because rules are data: no source patching, no
 * hand-written mutation list. A mutant that no test notices marks an untested clause (or an equivalent mutant).
 */
final class DslMutator {

    record Mutant(String description, RuleAst rule) { }

    private DslMutator() { }

    static List<Mutant> mutants(RuleAst r) {
        List<Mutant> out = new ArrayList<>();
        for (int i = 0; i < r.clauses().size(); i++) {
            Clause c = r.clauses().get(i);
            if (c instanceof Clause.Let) {
                continue;
            }
            out.add(new Mutant("delete clause " + i + " " + label(c), withClauses(r, i, null)));
            for (Expr m : exprMutants(exprOf(c))) {
                out.add(new Mutant("clause " + i + " " + label(c) + ": " + show(exprOf(c), m), withClauses(r, i, rebuild(c, m))));
            }
        }
        for (int i = 0; i < r.flags().size(); i++) {
            List<Flag> flags = new ArrayList<>(r.flags());
            flags.remove(i);
            if (!flags.isEmpty()) {
                out.add(new Mutant("delete flag " + i, new RuleAst(r.id(), r.var(), r.kind(), r.clauses(), flags)));
            }
            Flag f = r.flags().get(i);
            if (f.when() != null) {
                for (Expr m : exprMutants(f.when())) {
                    List<Flag> copy = new ArrayList<>(r.flags());
                    copy.set(i, new Flag(f.at(), m, f.confidence(), f.message()));
                    out.add(new Mutant("flag " + i + " when: " + show(f.when(), m),
                            new RuleAst(r.id(), r.var(), r.kind(), r.clauses(), copy)));
                }
            }
            if (f.confidence() != null) {
                List<Flag> copy = new ArrayList<>(r.flags());
                copy.set(i, new Flag(f.at(), f.when(), null, f.message()));
                out.add(new Mutant("flag " + i + ": drop confidence", new RuleAst(r.id(), r.var(), r.kind(), r.clauses(), copy)));
            }
        }
        return out;
    }

    private static String label(Clause c) {
        return c.getClass().getSimpleName().toLowerCase();
    }

    private static Expr exprOf(Clause c) {
        return switch (c) {
            case Clause.Where w -> w.e();
            case Clause.Unless u -> u.e();
            case Clause.Require q -> q.e();
            case Clause.Let l -> l.body();
        };
    }

    private static Clause rebuild(Clause c, Expr e) {
        return switch (c) {
            case Clause.Where w -> new Clause.Where(e);
            case Clause.Unless u -> new Clause.Unless(e);
            case Clause.Require q -> new Clause.Require(e);
            case Clause.Let l -> new Clause.Let(l.name(), l.param(), e);
        };
    }

    private static RuleAst withClauses(RuleAst r, int index, Clause replacement) {
        List<Clause> clauses = new ArrayList<>(r.clauses());
        if (replacement == null) {
            clauses.remove(index);
        } else {
            clauses.set(index, replacement);
        }
        return new RuleAst(r.id(), r.var(), r.kind(), clauses, r.flags());
    }

    /** One-step mutants of a boolean expression: drop an and/or operand, drop a negation, negate a leaf test. */
    static List<Expr> exprMutants(Expr e) {
        List<Expr> out = new ArrayList<>();
        switch (e) {
            case Expr.And a -> {
                out.add(a.r());
                out.add(a.l());
                exprMutants(a.l()).forEach(m -> out.add(new Expr.And(m, a.r())));
                exprMutants(a.r()).forEach(m -> out.add(new Expr.And(a.l(), m)));
            }
            case Expr.Or o -> {
                out.add(o.r());
                out.add(o.l());
                exprMutants(o.l()).forEach(m -> out.add(new Expr.Or(m, o.r())));
                exprMutants(o.r()).forEach(m -> out.add(new Expr.Or(o.l(), m)));
            }
            case Expr.Implies i -> {
                out.add(i.r());
                exprMutants(i.l()).forEach(m -> out.add(new Expr.Implies(m, i.r())));
                exprMutants(i.r()).forEach(m -> out.add(new Expr.Implies(i.l(), m)));
            }
            case Expr.Not n -> {
                out.add(n.e());
                exprMutants(n.e()).forEach(m -> out.add(new Expr.Not(m)));
            }
            case Expr.In x -> {
                out.add(new Expr.Not(x));
                if (x.r() instanceof Expr.Lst l && l.items().size() > 1) {
                    for (int i = 0; i < l.items().size(); i++) {
                        List<Expr> items = new ArrayList<>(l.items());
                        items.remove(i);
                        out.add(new Expr.In(x.l(), new Expr.Lst(items)));
                    }
                }
            }
            case Expr.Matches m -> out.add(new Expr.Not(m));
            case Expr.IsKind k -> out.add(new Expr.IsKind(k.l(), k.kind(), !k.neg()));
            case Expr.IsEmpty x -> out.add(new Expr.IsEmpty(x.l(), !x.neg()));
            case Expr.Exists x -> out.add(new Expr.Not(x));
            case Expr.Inside x -> out.add(new Expr.Not(x));
            case Expr.Quant q -> out.add(new Expr.Quant(!q.all(), q.l(), q.pred()));
            case Expr.FactCall f -> out.add(new Expr.Not(f));
            default -> { }
        }
        return out;
    }

    private static String show(Expr before, Expr after) {
        return summarize(after);
    }

    static String summarize(Expr e) {
        String s = unparse(e);
        return s.length() > 150 ? s.substring(0, 147) + "..." : s;
    }

    static String unparse(Expr e) {
        return switch (e) {
            case Expr.Str s -> "\"" + s.v() + "\"";
            case Expr.Rx r -> "/" + r.src() + "/";
            case Expr.Bool b -> String.valueOf(b.v());
            case Expr.Lst l -> l.items().stream().map(DslMutator::unparse).toList().toString();
            case Expr.Id i -> i.name();
            case Expr.Field f -> unparse(f.target()) + "." + f.name();
            case Expr.FactCall c -> unparse(c.target()) + "." + c.name() + c.args().stream().map(DslMutator::unparse).toList();
            case Expr.Apply a -> a.fn() + a.args().stream().map(DslMutator::unparse).toList();
            case Expr.Not n -> "not (" + unparse(n.e()) + ")";
            case Expr.And a -> "(" + unparse(a.l()) + " and " + unparse(a.r()) + ")";
            case Expr.Or o -> "(" + unparse(o.l()) + " or " + unparse(o.r()) + ")";
            case Expr.Implies i -> unparse(i.l()) + " => " + unparse(i.r());
            case Expr.Ternary t -> unparse(t.c()) + " ? " + unparse(t.a()) + " : " + unparse(t.b());
            case Expr.In i -> unparse(i.l()) + " in " + unparse(i.r());
            case Expr.Matches m -> unparse(m.l()) + " matches /" + m.r().src() + "/";
            case Expr.IsKind k -> unparse(k.l()) + " is " + (k.neg() ? "not " : "") + k.kind();
            case Expr.IsEmpty x -> unparse(x.l()) + " is " + (x.neg() ? "not " : "") + "empty";
            case Expr.Exists x -> unparse(x.l()) + " exists";
            case Expr.Inside x -> unparse(x.l()) + " inside " + x.kind();
            case Expr.Quant q -> unparse(q.l()) + (q.all() ? " all " : " any ") + q.pred();
            case Expr.Eq q -> unparse(q.l()) + (q.neg() ? " != " : " == ") + unparse(q.r());
        };
    }
}
