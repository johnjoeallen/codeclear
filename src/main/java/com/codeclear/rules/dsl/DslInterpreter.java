package com.codeclear.rules.dsl;

import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.dsl.RuleAst.Clause;
import com.codeclear.rules.dsl.RuleAst.Flag;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
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

    record Finding(int line, String confidence, String message, boolean candidate) { }

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

    private static final Map<String, Class<? extends Node>> KINDS = Map.ofEntries(
            Map.entry("Catch", CatchClause.class),
            Map.entry("Call", MethodCallExpr.class),
            Map.entry("Name", NameExpr.class),
            Map.entry("Field", FieldAccessExpr.class),
            Map.entry("Param", Parameter.class),
            Map.entry("ExprStmt", ExpressionStmt.class),
            Map.entry("Method", MethodDeclaration.class),
            Map.entry("Binary", BinaryExpr.class),
            Map.entry("StringLit", StringLiteralExpr.class),
            Map.entry("New", ObjectCreationExpr.class),
            Map.entry("Throw", ThrowStmt.class),
            Map.entry("Return", ReturnStmt.class),
            Map.entry("Break", BreakStmt.class),
            Map.entry("Continue", ContinueStmt.class),
            Map.entry("FieldDecl", FieldDeclaration.class),
            Map.entry("DoubleLit", DoubleLiteralExpr.class),
            Map.entry("Var", com.github.javaparser.ast.body.VariableDeclarator.class),
            Map.entry("Lambda", com.github.javaparser.ast.expr.LambdaExpr.class),
            Map.entry("MethodRef", com.github.javaparser.ast.expr.MethodReferenceExpr.class),
            Map.entry("Annotation", com.github.javaparser.ast.expr.AnnotationExpr.class),
            Map.entry("If", IfStmt.class));

    private static final Map<String, java.util.function.Predicate<Node>> KIND_TESTS = new HashMap<>();

    static {
        KINDS.forEach((k, c) -> KIND_TESTS.put(k, c::isInstance));
        KIND_TESTS.put("Jump", DslSyntaxFacts::isJump);
        KIND_TESTS.put("Creation", n -> n instanceof MethodCallExpr || n instanceof ObjectCreationExpr);
        KIND_TESTS.put("Text", n -> n instanceof com.github.javaparser.ast.expr.StringLiteralExpr
                || n instanceof com.github.javaparser.ast.comments.Comment);
        KIND_TESTS.put("Comment", n -> n instanceof com.github.javaparser.ast.comments.Comment);
        KIND_TESTS.put("Loop", n -> n instanceof ForStmt || n instanceof ForEachStmt || n instanceof WhileStmt
                || n instanceof DoStmt);
    }

    /** The kinds of a SQL file ({@code Statement}, {@code Column} added by an ALTER TABLE, {@code Line}) and of a config file ({@code Setting}). */
    private static final Set<String> SQL_KINDS = Set.of("Statement", "Column", "Line", "Setting", "Removed");

    /** The nodes of a tree-sitter tree with this grammar type, the root included. */
    private static List<com.codeclear.rules.CCTsNode> treeNodes(com.codeclear.rules.CCTsTree tree, String type) {
        List<com.codeclear.rules.CCTsNode> out = new ArrayList<>();
        com.codeclear.rules.CCTsNode root = tree.root();
        boolean any = type.equals("Node"); // every named node
        if (any || root.type().equals(type)) {
            out.add(root);
        }
        out.addAll(root.descendants(any ? null : type));
        return out;
    }

    private List<Finding> runSql(RuleAst rule, CCFileAnalysis file, String ruleConfidence, DslProject project) {
        List<? extends com.codeclear.rules.CCModelNode> nodes = switch (rule.kind()) {
            case "Statement" -> file.sql() == null ? List.of() : file.sql().statements();
            case "Column" -> file.sql() == null ? List.of() : file.sql().columns();
            case "Setting" -> file.config() == null ? List.of() : file.config().settings();
            case "Removed" -> file.config() == null ? List.of() : file.config().removed();
            case "Line" -> file.sql() == null ? List.of() : file.sql().lines();
            default -> file.tree() == null ? List.of() : treeNodes(file.tree(), rule.kind());
        };
        List<Finding> out = new ArrayList<>();
        Ctx ctx = new Ctx(file, ruleConfidence, project);
        for (com.codeclear.rules.CCModelNode n : nodes) {
            Map<String, Object> env = new HashMap<>();
            env.put(rule.var(), n);
            env.put("file", file.file());
            if (project != null) {
                env.put("project", project);
            }
            if (!passes(rule, env, ctx)) {
                continue;
            }
            for (Flag f : rule.flags()) {
                if (f.when() != null && !bool(eval(f.when(), env, ctx))) {
                    continue;
                }
                Object at = eval(f.at(), env, ctx);
                if (!(at instanceof com.codeclear.rules.CCModelNode target)) {
                    throw new IllegalStateException("'at' must be a statement, column, line or setting of the file, got " + at);
                }
                String conf = ruleConfidence;
                if (f.confidence() != null) {
                    Object c = eval(f.confidence(), env, ctx);
                    if (!(c instanceof Atom a)) {
                        throw new IllegalStateException("confidence must be low, medium, high or default");
                    }
                    conf = a.name().equals("default") ? ruleConfidence : a.name().toUpperCase();
                }
                out.add(new Finding(target.line(), conf, render(f.message(), env, ctx), f.candidate()));
                break;
            }
        }
        TreeSet<Finding> sorted = new TreeSet<>(Comparator.comparingInt(Finding::line)
                .thenComparing(Finding::confidence).thenComparing(Finding::message).thenComparing(Finding::candidate));
        sorted.addAll(out);
        return List.copyOf(sorted);
    }

    private static final Set<String> ATOMS = Set.of("present", "default", "low", "medium", "high");

    /** @param ruleConfidence the confidence the rule's metadata declares, used for the atom {@code default} */
    List<Finding> run(RuleAst rule, CCFileAnalysis file, String ruleConfidence) {
        return run(rule, file, ruleConfidence, null);
    }

    List<Finding> run(RuleAst rule, CCFileAnalysis file, String ruleConfidence, DslProject project) {
        // A kind that is not a Java one is a tree-sitter node type when the file is Python or TypeScript.
        boolean treeKind = file.tree() != null && !KINDS.containsKey(rule.kind()) && !KIND_TESTS.containsKey(rule.kind())
                && !SQL_KINDS.contains(rule.kind());
        if (SQL_KINDS.contains(rule.kind()) || treeKind) {
            return runSql(rule, file, ruleConfidence, project);
        }
        Class<? extends Node> kind = KINDS.get(rule.kind());
        java.util.function.Predicate<Node> pseudo = KIND_TESTS.get(rule.kind());
        // A rule written for a tree-sitter grammar can apply to a file of another language (a .json next to the .ts): nothing to see.
        if (kind == null && pseudo == null && file.tree() == null && !"java".equals(file.file().getLanguage())) {
            return List.of();
        }
        if (kind == null && pseudo == null) {
            throw new IllegalArgumentException("unknown node kind '" + rule.kind() + "'");
        }
        List<Finding> out = new ArrayList<>();
        List<Node> nodes;
        if ((rule.kind().equals("Comment") || rule.kind().equals("Text"))
                && file.ast() instanceof com.github.javaparser.ast.CompilationUnit cu) {
            nodes = new ArrayList<Node>(cu.getAllComments()); // comments hang off the tree, so a walk of the nodes misses them
            if (rule.kind().equals("Text")) {
                nodes.addAll(cu.findAll(com.github.javaparser.ast.expr.StringLiteralExpr.class));
            }
        } else {
            nodes = kind != null ? new ArrayList<Node>(file.ast().findAll(kind))
                    : file.ast().findAll(Node.class).stream().filter(pseudo).toList();
        }
        for (Node n : nodes) {
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
                out.add(new Finding(node.getBegin().get().line, conf, render(f.message(), env, ctx), f.candidate()));
                break; // first matching flag wins for this node
            }
        }
        // Sorted by line then confidence, de-duplicated, as the Groovy detector does.
        TreeSet<Finding> sorted = new TreeSet<>(Comparator.comparingInt(Finding::line)
                .thenComparing(Finding::confidence).thenComparing(Finding::message).thenComparing(Finding::candidate));
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
            case Expr.IsKind k when eval(k.l(), env, ctx) instanceof com.codeclear.rules.CCTsNode t ->
                    t.type().equals(k.kind()) != k.neg();
            case Expr.IsKind k -> {
                var test = KIND_TESTS.get(k.kind());
                if (test == null) {
                    throw new IllegalArgumentException("unknown node kind '" + k.kind() + "'");
                }
                yield (eval(k.l(), env, ctx) instanceof Node nd && test.test(nd)) != k.neg();
            }
            case Expr.Inside x when eval(x.l(), env, ctx) instanceof com.codeclear.rules.CCTsNode t ->
                    t.ancestor(x.kind()) != null;
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
            case Expr.Num n -> n.v();
            case Expr.Size s -> switch (eval(s.l(), env, ctx)) {
                case List<?> l -> (long) l.size();
                case String str -> (long) str.length();
                case null -> 0L;
                default -> throw new IllegalStateException("'size()' needs a list or string");
            };
            case Expr.Cmp c -> {
                // A missing value, or a value that is not a number, is never equal to, greater than or less than anything.
                if (!(eval(c.l(), env, ctx) instanceof Number a) || !(eval(c.r(), env, ctx) instanceof Number b)) {
                    yield false;
                }
                int d = Long.compare(a.longValue(), b.longValue());
                yield switch (c.op()) {
                    case "==" -> d == 0;
                    case "!=" -> d != 0;
                    case ">" -> d > 0;
                    case ">=" -> d >= 0;
                    case "<" -> d < 0;
                    default -> d <= 0;
                };
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
