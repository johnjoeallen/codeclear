package com.codeclear.rules.dsl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Host facts for JPA and Spring: entities, lazy associations, transactions, controllers, fetch plans. */
final class DslJpaFacts {

    /** Returned when a name is not one of these facts, so the caller can fall through. */
    static final Object NOT_HANDLED = new Object();

    private static final Set<String> WRAPPERS =
            Set.of("ResponseEntity", "List", "Set", "Collection", "Optional", "Page", "Iterable", "Stream");

    private DslJpaFacts() { }

    static Object field(Node n, String name, DslProject p) {
        if (name.equals("inTransaction")) {
            return inTransaction(n, p, newSet());
        }
        if (name.equals("fetched") && n instanceof Expression e) {
            return p != null && fetched(e, p, newSet());
        }
        if (n instanceof MethodCallExpr c) {
            switch (name) {
                case "lazyAssociation": return lazyAssociation(c, p);
                case "dereferenced": return dereferenced(c);
                default: break;
            }
        }
        if (n instanceof MethodDeclaration m) {
            switch (name) {
                case "name": return m.getNameAsString();
                case "nameNode": return m.getName();
                case "inController": return inController(m);
                case "returnedEntity": return returnedEntity(m, p);
                case "lazyAssociations": return p == null ? null : lazyList(m, p);
                case "returnsFetched": return p != null && returnsFetched(m, p, newSet());
                default: break;
            }
        }
        return NOT_HANDLED;
    }

    private static List<String> lazyList(MethodDeclaration m, DslProject p) {
        String e = returnedEntity(m, p);
        return e == null ? List.of() : p.serialisedLazy(e);
    }

    // ---- transactions and controllers --------------------------------------------------------------------

    private static Set<Object> newSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * True when the node runs inside a transaction: its method or class is {@code @Transactional}, or the method has
     * callers in the project and every one of them runs inside a transaction (followed transitively through the
     * call graph). A method nobody calls, such as a controller entry point, is not inside one.
     */
    private static boolean inTransaction(Node n, DslProject p, Set<Object> visiting) {
        Optional<MethodDeclaration> m = n instanceof MethodDeclaration md ? Optional.of(md) : n.findAncestor(MethodDeclaration.class);
        if (m.isPresent() && DslProject.has(m.get(), "Transactional")) {
            return true;
        }
        Optional<ClassOrInterfaceDeclaration> c = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if (c.isPresent() && DslProject.has(c.get(), "Transactional")) {
            return true;
        }
        if (p == null || m.isEmpty()) {
            return false;
        }
        if (!visiting.add(m.get())) {
            return true; // a cycle in the call graph adds no evidence against
        }
        try {
            List<MethodCallExpr> calls = p.callsTo(m.get());
            return !calls.isEmpty() && calls.stream().allMatch(call -> inTransaction(call, p, visiting));
        } finally {
            visiting.remove(m.get());
        }
    }

    private static boolean inController(MethodDeclaration m) {
        return m.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(c -> DslProject.has(c, "RestController") || DslProject.has(c, "Controller")).orElse(false);
    }

    // ---- entity types ---------------------------------------------------------------------------------------

    /** Simple name of the entity a method returns, looking through ResponseEntity, List, Optional and friends. */
    private static String returnedEntity(MethodDeclaration m, DslProject p) {
        String t = unwrap(m.getType());
        return p != null && p.isEntity(t) ? t : null;
    }

    private static String unwrap(Type t) {
        if (t instanceof ClassOrInterfaceType c) {
            if (WRAPPERS.contains(c.getNameAsString()) && c.getTypeArguments().isPresent()
                    && !c.getTypeArguments().get().isEmpty()) {
                return unwrap(c.getTypeArguments().get().get(0));
            }
            return c.getNameAsString();
        }
        return null;
    }

    /** The entity an expression evaluates to: a variable of an entity type, or a call to a project method returning one. */
    private static String entityOf(Expression e, DslProject p) {
        if (p == null) {
            return null;
        }
        if (e instanceof NameExpr n) {
            String type = p.declaredSimpleType(n);
            return p.isEntity(type) ? type : null;
        }
        if (e instanceof MethodCallExpr c) {
            List<MethodDeclaration> targets = p.methods(c.getNameAsString()).stream()
                    .filter(m -> m.getParameters().size() == c.getArguments().size()).toList();
            if (targets.size() == 1) {
                String t = unwrap(targets.get(0).getType());
                return p.isEntity(t) ? t : null;
            }
        }
        return null;
    }

    // ---- associations ---------------------------------------------------------------------------------------

    /** The lazy association this getter call reads, or null. */
    private static String lazyAssociation(MethodCallExpr c, DslProject p) {
        String name = c.getNameAsString();
        if (p == null || c.getScope().isEmpty() || !name.startsWith("get") || name.length() < 4) {
            return null;
        }
        String entity = entityOf(c.getScope().get(), p);
        String assoc = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return entity != null && p.isLazy(entity, assoc) ? assoc : null;
    }

    /** The result is used: a method is called on it, or it is iterated. Merely fetching a lazy proxy is harmless. */
    private static boolean dereferenced(MethodCallExpr c) {
        Optional<Node> parent = c.getParentNode();
        if (parent.isEmpty()) {
            return false;
        }
        Node p = parent.get();
        return (p instanceof MethodCallExpr m && m.getScope().map(s -> s == c).orElse(false))
                || (p instanceof ForEachStmt f && f.getIterable() == c);
    }

    // ---- provenance: was this value produced by a query that fetches its associations? ----------------------------

    private static final Set<String> OPTIONAL_UNWRAP = Set.of("orElseThrow", "orElse", "orElseGet", "get");

    /** The method is annotated as a fetch plan, or returns a value that {@link #fetched} proves came from one. */
    private static boolean returnsFetched(MethodDeclaration m, DslProject p, Set<Object> visiting) {
        if (DslProject.has(m, "EntityGraph")
                || m.getAnnotationByName("Query").map(a -> a.toString().toLowerCase().contains("join fetch")).orElse(false)) {
            return true;
        }
        if (!visiting.add(m)) {
            return false;
        }
        return m.findAll(ReturnStmt.class).stream()
                .anyMatch(r -> r.getExpression().map(e -> fetched(e, p, visiting)).orElse(false));
    }

    /**
     * Interprocedural, deterministic: follows a call through the project's methods, through local variables, and
     * back through every caller's argument when the value is a parameter. Name-and-arity matching stands in for
     * the symbol resolution the real engine would use.
     */
    private static boolean fetched(Expression e, DslProject p, Set<Object> visiting) {
        if (e instanceof EnclosedExpr x) {
            return fetched(x.getInner(), p, visiting);
        }
        if (e instanceof CastExpr x) {
            return fetched(x.getExpression(), p, visiting);
        }
        if (e instanceof MethodCallExpr c) {
            if (OPTIONAL_UNWRAP.contains(c.getNameAsString()) && c.getScope().isPresent()
                    && fetched(c.getScope().get(), p, visiting)) {
                return true;
            }
            return p.targets(c).stream().anyMatch(m -> returnsFetched(m, p, visiting));
        }
        if (e instanceof NameExpr n) {
            Optional<MethodDeclaration> m = n.findAncestor(MethodDeclaration.class);
            if (m.isEmpty()) {
                return false;
            }
            for (VariableDeclarator d : m.get().findAll(VariableDeclarator.class)) {
                if (d.getNameAsString().equals(n.getNameAsString()) && d.getInitializer().isPresent()
                        && fetched(d.getInitializer().get(), p, visiting)) {
                    return true;
                }
            }
            var params = m.get().getParameters();
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i).getNameAsString().equals(n.getNameAsString()) && visiting.add(params.get(i))) {
                    final int index = i;
                    List<MethodCallExpr> calls = p.callsTo(m.get());
                    return !calls.isEmpty() && calls.stream()
                            .allMatch(call -> index < call.getArguments().size()
                                    && fetched(call.getArgument(index), p, visiting));
                }
            }
        }
        return false;
    }
}
