package com.codeclear.rules.dsl;

import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.dsl.RuleAst.Clause;
import com.codeclear.rules.dsl.RuleAst.Flag;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Evaluates a {@link RuleAst} over a parsed file. Total by construction: no loops or recursion in the language, so
 * evaluation is bounded by tree size times rule size. Type errors and unknown names throw rather than yielding null.
 */
final class DslInterpreter {

    record Finding(int line, String confidence, String message) { }

    record Atom(String name) { }

    record Pred(String param, Expr body, Map<String, Object> env) { }

    /** A lazily evaluated, memoised {@code let} value, so a binding is only computed where it is used. */
    private static final class Thunk {
        final Expr body;
        final Map<String, Object> env;
        Object value;
        boolean done;

        Thunk(Expr body, Map<String, Object> env) {
            this.body = body;
            this.env = env;
        }
    }

    private static final Map<String, Class<? extends Node>> KINDS = Map.of(
            "Catch", CatchClause.class,
            "Call", MethodCallExpr.class,
            "Name", NameExpr.class,
            "Field", FieldAccessExpr.class,
            "Param", Parameter.class,
            "ExprStmt", ExpressionStmt.class,
            "Method", MethodDeclaration.class);

    private static final Map<String, java.util.function.Predicate<Node>> KIND_TESTS = new HashMap<>();

    static {
        KINDS.forEach((k, c) -> KIND_TESTS.put(k, c::isInstance));
        KIND_TESTS.put("Loop", n -> n instanceof ForStmt || n instanceof ForEachStmt || n instanceof WhileStmt
                || n instanceof DoStmt);
    }

    private static final Set<String> ATOMS = Set.of("present", "default", "low", "medium", "high");

    /** @param ruleConfidence the confidence the rule's metadata declares, used for the atom {@code default} */
    List<Finding> run(RuleAst rule, CCFileAnalysis file, String ruleConfidence) {
        return run(rule, file, ruleConfidence, null);
    }

    List<Finding> run(RuleAst rule, CCFileAnalysis file, String ruleConfidence, DslProject project) {
        Class<? extends Node> kind = KINDS.get(rule.kind());
        if (kind == null) {
            throw new IllegalArgumentException("unknown node kind '" + rule.kind() + "'");
        }
        List<Finding> out = new ArrayList<>();
        for (Node n : file.ast().findAll(kind)) {
            Map<String, Object> env = new HashMap<>();
            env.put(rule.var(), n);
            env.put("file", file.file());
            if (project != null) {
                env.put("project", project);
            }
            Ctx ctx = new Ctx(file, ruleConfidence, project);
            if (!passes(rule, env, ctx)) {
                continue;
            }
            for (Flag f : rule.flags()) {
                if (f.when() != null && !bool(eval(f.when(), env, ctx))) {
                    continue;
                }
                Object at = eval(f.at(), env, ctx);
                if (!(at instanceof Node node) || node.getBegin().isEmpty()) {
                    throw new IllegalStateException("'at' must be a node with a position, got " + at);
                }
                String conf = ruleConfidence;
                if (f.confidence() != null) {
                    Object c = eval(f.confidence(), env, ctx);
                    if (!(c instanceof Atom a)) {
                        throw new IllegalStateException("confidence must be low, medium, high or default");
                    }
                    conf = a.name().equals("default") ? ruleConfidence : a.name().toUpperCase();
                }
                out.add(new Finding(node.getBegin().get().line, conf, render(f.message(), env, ctx)));
                break; // first matching flag wins for this node
            }
        }
        // Sorted by line then confidence, de-duplicated, as the Groovy detector does.
        TreeSet<Finding> sorted = new TreeSet<>(Comparator.comparingInt(Finding::line)
                .thenComparing(Finding::confidence).thenComparing(Finding::message));
        sorted.addAll(out);
        return List.copyOf(sorted);
    }

    private record Ctx(CCFileAnalysis file, String ruleConfidence, DslProject project) { }

    private boolean passes(RuleAst rule, Map<String, Object> env, Ctx ctx) {
        for (Clause c : rule.clauses()) {
            switch (c) {
                case Clause.Where w -> {
                    if (!bool(eval(w.e(), env, ctx))) {
                        return false;
                    }
                }
                case Clause.Require r -> {
                    if (!bool(eval(r.e(), env, ctx))) {
                        return false;
                    }
                }
                case Clause.Unless u -> {
                    if (bool(eval(u.e(), env, ctx))) {
                        return false;
                    }
                }
                case Clause.Let l -> env.put(l.name(),
                        l.param() == null ? new Thunk(l.body(), env) : new Pred(l.param(), l.body(), env));
            }
        }
        return true;
    }

    private String render(List<Object> parts, Map<String, Object> env, Ctx ctx) {
        StringBuilder b = new StringBuilder();
        for (Object p : parts) {
            b.append(p instanceof Expr e ? String.valueOf(eval(e, env, ctx)) : p);
        }
        return b.toString();
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        throw new IllegalStateException("expected a boolean, got " + o);
    }

    private Object eval(Expr e, Map<String, Object> env, Ctx ctx) {
        return switch (e) {
            case Expr.Str s -> s.v();
            case Expr.Rx r -> throw new IllegalStateException("a regex is only valid after 'matches'");
            case Expr.Bool b -> b.v();
            case Expr.Lst l -> l.items().stream().map(i -> eval(i, env, ctx)).toList();
            case Expr.Id id -> {
                if (env.containsKey(id.name())) {
                    Object v = env.get(id.name());
                    if (v instanceof Thunk t) {
                        if (!t.done) {
                            t.value = eval(t.body, t.env, ctx);
                            t.done = true;
                        }
                        yield t.value;
                    }
                    yield v;
                }
                if (ATOMS.contains(id.name())) {
                    yield new Atom(id.name());
                }
                throw new IllegalArgumentException("unknown name '" + id.name() + "'");
            }
            case Expr.Field f -> DslFacts.field(eval(f.target(), env, ctx), f.name(), ctx.file().model(), ctx.project());
            case Expr.FactCall c -> DslFacts.fact(eval(c.target(), env, ctx), c.name(),
                    c.args().stream().map(a -> eval(a, env, ctx)).toList(), ctx.project(),
                    (pred, arg) -> bool(applyPred(pred, arg, ctx)));
            case Expr.Apply a -> {
                if (!(env.get(a.fn()) instanceof Pred p) || a.args().size() != 1) {
                    throw new IllegalArgumentException("'" + a.fn() + "' is not a one-argument predicate");
                }
                yield applyPred(p, eval(a.args().get(0), env, ctx), ctx);
            }
            case Expr.Not n -> !bool(eval(n.e(), env, ctx));
            case Expr.And a -> bool(eval(a.l(), env, ctx)) && bool(eval(a.r(), env, ctx));
            case Expr.Or o -> bool(eval(o.l(), env, ctx)) || bool(eval(o.r(), env, ctx));
            case Expr.Implies i -> !bool(eval(i.l(), env, ctx)) || bool(eval(i.r(), env, ctx));
            case Expr.Ternary t -> bool(eval(t.c(), env, ctx)) ? eval(t.a(), env, ctx) : eval(t.b(), env, ctx);
            case Expr.In i -> {
                Object l = eval(i.l(), env, ctx);
                Object r = eval(i.r(), env, ctx);
                yield l != null && r instanceof List<?> list && list.contains(l);
            }
            case Expr.Matches m -> eval(m.l(), env, ctx) instanceof String s && m.r().p().matcher(s).matches();
            case Expr.IsKind k -> {
                var test = KIND_TESTS.get(k.kind());
                if (test == null) {
                    throw new IllegalArgumentException("unknown node kind '" + k.kind() + "'");
                }
                yield (eval(k.l(), env, ctx) instanceof Node nd && test.test(nd)) != k.neg();
            }
            case Expr.Inside x -> {
                var test = KIND_TESTS.get(x.kind());
                if (test == null) {
                    throw new IllegalArgumentException("unknown node kind '" + x.kind() + "'");
                }
                boolean found = false;
                if (eval(x.l(), env, ctx) instanceof Node nd) {
                    for (Optional<Node> a = nd.getParentNode(); a.isPresent() && !found; a = a.get().getParentNode()) {
                        found = test.test(a.get());
                    }
                }
                yield found;
            }
            case Expr.IsEmpty x -> {
                Object v = eval(x.l(), env, ctx);
                boolean empty = switch (v) {
                    case List<?> l -> l.isEmpty();
                    case String s -> s.isEmpty();
                    case null -> true;
                    default -> throw new IllegalStateException("'is empty' needs a list or string, got " + v);
                };
                yield empty != x.neg();
            }
            case Expr.Exists x -> eval(x.l(), env, ctx) != null;
            case Expr.Quant q -> {
                if (!(eval(q.l(), env, ctx) instanceof List<?> list) || !(env.get(q.pred()) instanceof Pred p)) {
                    throw new IllegalStateException("'all/any' needs a list and a named predicate");
                }
                yield q.all()
                        ? list.stream().allMatch(item -> bool(applyPred(p, item, ctx)))
                        : list.stream().anyMatch(item -> bool(applyPred(p, item, ctx)));
            }
            case Expr.Eq q -> Objects.equals(eval(q.l(), env, ctx), eval(q.r(), env, ctx)) != q.neg();
        };
    }

    private Object applyPred(Pred p, Object arg, Ctx ctx) {
        Map<String, Object> inner = new HashMap<>(p.env());
        inner.put(p.param(), arg);
        return eval(p.body(), inner, ctx);
    }
}
