package com.codeclear.rules.dsl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * The taint engine: reviewed Java that knows how values flow, while the rule supplies the policy (what is a source,
 * what neutralises a value) as two named predicates. A value is tainted when it may derive from a source node
 * without passing through a sanitiser node.
 *
 * <p>Flow handled: string concatenation, ternaries, casts, local variables with strong updates (a later assignment
 * in the same or an enclosing block kills earlier ones), compound assignment, a small set of pass-through string
 * methods, getters on a tainted object, parameters (any caller may pass a tainted argument) and return values of
 * project methods. Matching of calls to methods is by name and arity.
 */
final class DslTaint {

    private static final Set<String> PASS_THROUGH = Set.of("trim", "strip", "toLowerCase", "toUpperCase", "substring",
            "concat", "replace", "replaceAll", "replaceFirst", "toString", "intern", "stripLeading", "stripTrailing");
    private static final Set<String> STATIC_STRING = Set.of("valueOf", "format", "join", "formatted");

    private final DslInterpreter.Pred source;
    private final DslInterpreter.Pred sanitizer;
    private final BiPredicate<DslInterpreter.Pred, Object> apply;
    private final DslProject project;
    private final Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
    private final java.util.Map<MethodDeclaration, Boolean> returnMemo = new IdentityHashMap<>();
    private final java.util.Map<Parameter, Boolean> paramMemo = new IdentityHashMap<>();
    private final java.util.Map<Node, Boolean> sourceMemo = new IdentityHashMap<>();
    private final java.util.Map<Node, Boolean> sanitizerMemo = new IdentityHashMap<>();

    /** Work allowed for one query. A pathological flow ends as "not proven tainted" rather than running for minutes. */
    static final int STEP_BUDGET = 5_000;
    /** Interprocedural hops followed (a call's return value, a parameter's callers) before giving up. */
    static final int MAX_HOPS = 3;
    private int steps;
    private int hops;

    DslTaint(DslInterpreter.Pred source, DslInterpreter.Pred sanitizer,
             BiPredicate<DslInterpreter.Pred, Object> apply, DslProject project) {
        this.source = source;
        this.sanitizer = sanitizer;
        this.apply = apply;
        this.project = project;
    }

    boolean tainted(Expression e) {
        if (++steps > STEP_BUDGET) {
            return false;
        }
        if (sanitizerMemo.computeIfAbsent(e, n -> apply.test(sanitizer, n))) {
            return false;
        }
        if (sourceMemo.computeIfAbsent(e, n -> apply.test(source, n))) {
            return true;
        }
        if (e instanceof EnclosedExpr x) {
            return tainted(x.getInner());
        }
        if (e instanceof CastExpr x) {
            return tainted(x.getExpression());
        }
        if (e instanceof BinaryExpr b) {
            return b.getOperator() == BinaryExpr.Operator.PLUS && (tainted(b.getLeft()) || tainted(b.getRight()));
        }
        if (e instanceof ConditionalExpr c) {
            return tainted(c.getThenExpr()) || tainted(c.getElseExpr());
        }
        if (e instanceof NameExpr n) {
            return nameTainted(n);
        }
        if (e instanceof MethodCallExpr c) {
            return callTainted(c);
        }
        return false;
    }

    // ---- calls ----------------------------------------------------------------------------------------------

    private boolean callTainted(MethodCallExpr c) {
        String name = c.getNameAsString();
        if (c.getScope().isPresent()) {
            Expression scope = c.getScope().get();
            if ((PASS_THROUGH.contains(name) || name.startsWith("get")) && tainted(scope)) {
                return true;
            }
            if (scope.toString().equals("String") && STATIC_STRING.contains(name)
                    && c.getArguments().stream().anyMatch(this::tainted)) {
                return true;
            }
        }
        if (name.equals("concat") && c.getArguments().stream().anyMatch(this::tainted)) {
            return true;
        }
        for (MethodDeclaration m : project.targets(c)) {
            if (returnsTainted(m)) {
                return true;
            }
        }
        return false;
    }

    private boolean returnsTainted(MethodDeclaration m) {
        Boolean known = returnMemo.get(m);
        if (known != null) {
            return known;
        }
        if (hops >= MAX_HOPS || !visiting.add(m)) {
            return false;
        }
        hops++;
        try {
            boolean result = m.findAll(ReturnStmt.class).stream()
                    .anyMatch(r -> r.getExpression().map(this::tainted).orElse(false));
            returnMemo.put(m, result);
            return result;
        } finally {
            hops--;
            visiting.remove(m);
        }
    }

    // ---- variables ------------------------------------------------------------------------------------------

    private record Def(Node node, Expression value, boolean strong) { }

    private boolean nameTainted(NameExpr use) {
        Optional<MethodDeclaration> method = use.findAncestor(MethodDeclaration.class);
        if (method.isEmpty() || !visiting.add(use)) {
            return false; // no method, or already resolving this very use (a cycle)
        }
        try {
            return resolveName(use, method.get());
        } finally {
            visiting.remove(use);
        }
    }

    private boolean resolveName(NameExpr use, MethodDeclaration m) {
        String name = use.getNameAsString();
        List<Def> defs = new ArrayList<>();
        for (VariableDeclarator d : m.findAll(VariableDeclarator.class)) {
            if (d.getNameAsString().equals(name) && before(d, use) && !contains(d, use)) {
                Optional<Expression> init = d.getInitializer();
                if (init.isEmpty() && d.getParentNode().flatMap(Node::getParentNode).orElse(null) instanceof ForEachStmt f) {
                    init = Optional.of(f.getIterable());
                }
                init.ifPresent(v -> defs.add(new Def(d, v, dominates(d, use))));
            }
        }
        for (AssignExpr a : m.findAll(AssignExpr.class)) {
            if (a.getTarget() instanceof NameExpr t && t.getNameAsString().equals(name) && before(a, use)
                    && !contains(a, use)) {
                boolean plain = a.getOperator() == AssignExpr.Operator.ASSIGN;
                defs.add(new Def(a, a.getValue(), plain && dominates(a, use)));
            }
        }
        defs.sort(Comparator.comparing((Def d) -> d.node().getBegin().get()).reversed());
        for (Def d : defs) {
            if (tainted(d.value())) {
                return true;
            }
            if (d.strong()) {
                return false; // a dominating assignment replaces everything before it
            }
        }
        // no dominating definition: the value may be the one the method was entered with
        var params = m.getParameters();
        for (int i = 0; i < params.size(); i++) {
            Parameter p = params.get(i);
            if (p.getNameAsString().equals(name)) {
                return paramTainted(m, p, i);
            }
        }
        return false;
    }

    private boolean paramTainted(MethodDeclaration m, Parameter p, int index) {
        if (apply.test(source, p)) {
            return true;
        }
        Boolean known = paramMemo.get(p);
        if (known != null) {
            return known;
        }
        if (hops >= MAX_HOPS || !visiting.add(p)) {
            return false;
        }
        hops++;
        try {
            boolean result = false;
            for (MethodCallExpr call : project.callsTo(m)) {
                if (index < call.getArguments().size() && tainted(call.getArgument(index))) {
                    result = true;
                    break;
                }
            }
            paramMemo.put(p, result);
            return result;
        } finally {
            hops--;
            visiting.remove(p);
        }
    }

    /** The definition's own value mentions the use, as in {@code s = s + x}: it refers to the previous definition. */
    private static boolean contains(Node def, Node use) {
        for (Optional<Node> a = use.getParentNode(); a.isPresent(); a = a.get().getParentNode()) {
            if (a.get() == def) {
                return true;
            }
        }
        return false;
    }

    private static boolean before(Node def, Node use) {
        return def.getBegin().isPresent() && use.getBegin().isPresent()
                && def.getBegin().get().isBefore(use.getBegin().get());
    }

    /** The definition's statement sits in a block that also contains the use, so it replaces earlier definitions. */
    private static boolean dominates(Node def, Node use) {
        Optional<Statement> stmt = def.findAncestor(Statement.class);
        if (stmt.isEmpty()) {
            return false;
        }
        Optional<Node> parent = stmt.get().getParentNode();
        if (parent.isEmpty() || !(parent.get() instanceof BlockStmt block)) {
            return false;
        }
        for (Optional<Node> a = use.getParentNode(); a.isPresent(); a = a.get().getParentNode()) {
            if (a.get() == block) {
                return true;
            }
        }
        return false;
    }
}
