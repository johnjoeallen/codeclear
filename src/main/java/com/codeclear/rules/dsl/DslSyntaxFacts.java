package com.codeclear.rules.dsl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Host facts about plain Java syntax: operators, constructor calls, throws, field declarations, enclosing scopes and
 * jumps out of {@code finally}. Reviewed Java so that rules stay short.
 */
final class DslSyntaxFacts {

    /** Returned when a name is not one of these facts, so the caller can fall through. */
    static final Object NOT_HANDLED = DslJpaFacts.NOT_HANDLED;

    private static final Pattern IDENTITY_SENTINEL = Pattern.compile("(?i)sentinel|identity");

    private DslSyntaxFacts() { }

    static Object field(Node n, String name, com.codeclear.rules.CCCodeModel model) {
        switch (name) {
            case "calls": return List.copyOf(n.findAll(MethodCallExpr.class));
            case "throws": return List.copyOf(n.findAll(ThrowStmt.class));
            case "assignments": return List.copyOf(n.findAll(com.github.javaparser.ast.expr.AssignExpr.class));
            case "callsAfter": return callsAfter(n);
            case "enclosingCatch": return enclosingCatch(n);
            case "enclosingClassMethods": return enclosingClassMethods(n);
            case "enclosingMethodName": return n.findAncestor(MethodDeclaration.class).map(MethodDeclaration::getNameAsString).orElse(null);
            default: break;
        }
        if (n instanceof BinaryExpr b) {
            switch (name) {
                case "operator": return b.getOperator().asString();
                case "left": return b.getLeft();
                case "right": return b.getRight();
                default: break;
            }
        } else if (n instanceof ObjectCreationExpr o) {
            switch (name) {
                case "args": return List.copyOf(o.getArguments());
                case "firstArg": return o.getArguments().isEmpty() ? null : o.getArgument(0);
                case "typeName": return o.getType().getNameAsString();
                case "causeUnsupported": return causeUnsupported(o, model);
                default: break;
            }
        } else if (n instanceof ThrowStmt t) {
            if (name.equals("expression")) {
                return t.getExpression();
            }
        } else if (n instanceof FieldDeclaration f) {
            switch (name) {
                case "name": return f.getVariable(0).getNameAsString();
                case "typeName": return simpleName(f.getVariable(0).getType());
                case "initializerTypeName":
                    return f.getVariable(0).getInitializer().filter(i -> i instanceof ObjectCreationExpr)
                            .map(i -> ((ObjectCreationExpr) i).getType().getNameAsString()).orElse(null);
                case "isStatic": return f.isStatic();
                case "isFinal": return f.isFinal();
                case "isPrivate": return f.isPrivate();
                case "isVolatile": return f.isVolatile();
                case "classAnnotations":
                    return f.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(c -> c.getAnnotations().stream().map(a -> a.getNameAsString()).toList()).orElse(List.of());
                case "usedOnlyInSynchronized": return usedOnlyInSynchronized(f);
                default: break;
            }
        }
        if (name.equals("doubleCheckedField") && n instanceof com.github.javaparser.ast.stmt.IfStmt i) {
            return doubleCheckedField(i);
        }
        if (name.equals("isIdentitySentinel")) {
            return n instanceof NameExpr x && identitySentinel(x);
        }
        if (name.equals("escapesFinally") && isJump(n)) {
            return escapesFinally(n);
        }
        return NOT_HANDLED;
    }

    static Object fact(Object target, String name, List<Object> args) {
        if (name.equals("mentions") && target instanceof Node n && args.size() == 1 && args.get(0) instanceof String id) {
            return mentions(n, id);
        }
        if (name.equals("releasedInNextFinally") && target instanceof MethodCallExpr c && args.size() == 1 && args.get(0) instanceof List<?> names) {
            return releasedInNextFinally(c, names);
        }
        if (name.equals("releasedInEnclosingFinally") && target instanceof MethodCallExpr c && args.size() == 1 && args.get(0) instanceof List<?> names) {
            return releasedInEnclosingFinally(c, names);
        }
        if (name.equals("releasedElsewhere") && target instanceof MethodCallExpr c && args.size() == 1 && args.get(0) instanceof List<?> names) {
            return releasedElsewhere(c, names);
        }
        if (name.equals("carries") && target instanceof Node n && args.size() == 1 && args.get(0) instanceof String id) {
            return carries(n, id);
        }
        return NOT_HANDLED;
    }

    static boolean isJump(Node n) {
        return n instanceof ReturnStmt || n instanceof ThrowStmt || n instanceof BreakStmt || n instanceof ContinueStmt;
    }

    private static String simpleName(Type t) {
        return t.isClassOrInterfaceType() ? t.asClassOrInterfaceType().getNameAsString() : t.toString();
    }

    /** The node is, or contains, a simple name with this identifier (a use of that variable). */
    private static boolean mentions(Node n, String id) {
        return (n instanceof NameExpr x && x.getNameAsString().equals(id))
                || n.findAll(NameExpr.class).stream().anyMatch(x -> x.getNameAsString().equals(id));
    }

    // ---- locks ------------------------------------------------------------------------------------------------

    /** The acquire is a statement, the very next statement is a try, and its finally calls one of the release methods on the same lock. */
    private static boolean releasedInNextFinally(MethodCallExpr acquire, List<?> releaseNames) {
        Optional<com.github.javaparser.ast.stmt.ExpressionStmt> stmt = acquire.findAncestor(com.github.javaparser.ast.stmt.ExpressionStmt.class);
        if (stmt.isEmpty() || stmt.get().getExpression() != acquire || acquire.getScope().isEmpty()) {
            return false;
        }
        Optional<Node> parent = stmt.get().getParentNode();
        if (parent.isEmpty() || !(parent.get() instanceof com.github.javaparser.ast.stmt.BlockStmt block)) {
            return false;
        }
        int at = block.getStatements().indexOf(stmt.get());
        if (at < 0 || at + 1 >= block.getStatements().size()
                || !(block.getStatement(at + 1) instanceof TryStmt t) || t.getFinallyBlock().isEmpty()) {
            return false;
        }
        String lock = acquire.getScope().get().toString();
        return t.getFinallyBlock().get().findAll(MethodCallExpr.class).stream()
                .anyMatch(c -> releaseNames.contains(c.getNameAsString())
                        && c.getScope().map(sc -> sc.toString().equals(lock)).orElse(false));
    }

    /** The acquire is inside the try block of a try whose finally releases the same lock: {@code try { lock.lock(); ... } finally { lock.unlock(); }}. */
    private static boolean releasedInEnclosingFinally(MethodCallExpr acquire, List<?> releaseNames) {
        if (acquire.getScope().isEmpty()) {
            return false;
        }
        String lock = acquire.getScope().get().toString();
        Node below = acquire;
        for (Optional<Node> parent = acquire.getParentNode(); parent.isPresent(); parent = parent.get().getParentNode()) {
            Node a = parent.get();
            if (boundary(a)) {
                return false;
            }
            final Node child = below;
            if (a instanceof TryStmt t && t.getTryBlock() == child && t.getFinallyBlock().isPresent()
                    && t.getFinallyBlock().get().findAll(MethodCallExpr.class).stream()
                    .anyMatch(c -> releaseNames.contains(c.getNameAsString())
                            && c.getScope().map(sc -> sc.toString().equals(lock)).orElse(false))) {
                return true;
            }
            below = a;
        }
        return false;
    }

    /** The names of the methods of every class the node is inside, nearest first: {@code 'main' in n.enclosingClassMethods}. */
    private static List<String> enclosingClassMethods(Node n) {
        List<String> out = new java.util.ArrayList<>();
        for (Optional<Node> a = n.getParentNode(); a.isPresent(); a = a.get().getParentNode()) {
            if (a.get() instanceof com.github.javaparser.ast.body.TypeDeclaration<?> td) {
                td.getMethods().forEach(m -> out.add(m.getNameAsString()));
            }
        }
        return out;
    }

    /**
     * Another method of the class releases the same lock without acquiring it itself: acquire and release are split
     * across methods, as in a lock wrapper. A second method that also acquires and releases the lock is an ordinary
     * user of it, not the other half of a split.
     */
    private static boolean releasedElsewhere(MethodCallExpr acquire, List<?> releaseNames) {
        Optional<ClassOrInterfaceDeclaration> cls = acquire.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty() || acquire.getScope().isEmpty()) {
            return false;
        }
        String lock = acquire.getScope().get().toString();
        Optional<MethodDeclaration> mine = acquire.findAncestor(MethodDeclaration.class);
        for (MethodDeclaration m : cls.get().getMethods()) {
            if (Optional.of(m).equals(mine)) {
                continue;
            }
            boolean releases = m.findAll(MethodCallExpr.class).stream().anyMatch(c -> releaseNames.contains(c.getNameAsString())
                    && c.getScope().map(sc -> sc.toString().equals(lock)).orElse(false));
            boolean acquiresToo = m.findAll(MethodCallExpr.class).stream().anyMatch(c -> c.getNameAsString().equals(acquire.getNameAsString())
                    && c.getScope().map(sc -> sc.toString().equals(lock)).orElse(false));
            if (releases && !acquiresToo) {
                return true;
            }
        }
        return false;
    }

    // ---- double-checked locking --------------------------------------------------------------------------------

    /**
     * For {@code if (f == null) { synchronized (...) { if (f == null) { f = ...; } } }} the declaration of the field
     * {@code f} in the enclosing class, otherwise null. A name that is not a field of the class (a local) gives null.
     */
    private static FieldDeclaration doubleCheckedField(com.github.javaparser.ast.stmt.IfStmt outer) {
        String f = nullCheckedName(outer.getCondition());
        if (f == null || outer.getElseStmt().isPresent()) {
            return null;
        }
        for (SynchronizedStmt sync : outer.getThenStmt().findAll(SynchronizedStmt.class)) {
            for (com.github.javaparser.ast.stmt.IfStmt inner : sync.getBody().findAll(com.github.javaparser.ast.stmt.IfStmt.class)) {
                if (f.equals(nullCheckedName(inner.getCondition()))
                        && inner.getThenStmt().findAll(com.github.javaparser.ast.expr.AssignExpr.class).stream()
                        .anyMatch(a -> f.equals(assignedName(a.getTarget())))) {
                    return outer.findAncestor(ClassOrInterfaceDeclaration.class).flatMap(c -> c.getFields().stream()
                            .filter(d -> d.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(f))).findFirst()).orElse(null);
                }
            }
        }
        return null;
    }

    /** The name in {@code x == null}, {@code null == x} and the same with {@code this.x}; null for anything else. */
    private static String nullCheckedName(com.github.javaparser.ast.expr.Expression cond) {
        while (cond instanceof com.github.javaparser.ast.expr.EnclosedExpr e) {
            cond = e.getInner();
        }
        if (!(cond instanceof BinaryExpr b) || b.getOperator() != BinaryExpr.Operator.EQUALS) {
            return null;
        }
        if (b.getRight() instanceof com.github.javaparser.ast.expr.NullLiteralExpr) {
            return assignedName(b.getLeft());
        }
        return b.getLeft() instanceof com.github.javaparser.ast.expr.NullLiteralExpr ? assignedName(b.getRight()) : null;
    }

    private static String assignedName(com.github.javaparser.ast.expr.Expression e) {
        if (e instanceof NameExpr n) {
            return n.getNameAsString();
        }
        return e instanceof FieldAccessExpr f && f.getScope() instanceof com.github.javaparser.ast.expr.ThisExpr ? f.getNameAsString() : null;
    }

    /** Methods that read only a description of an exception, not the exception itself. */
    private static final java.util.Set<String> MESSAGE_ONLY = java.util.Set.of("getMessage", "getLocalizedMessage",
            "toString", "getClass", "getStackTrace", "printStackTrace", "hashCode");

    /**
     * The node passes the variable on: it is the variable (or a local variable assigned from it), or contains one as
     * something other than the receiver of a message-reading call. {@code e}, {@code e.getCause()} and {@code wrap(e)}
     * carry {@code e}; {@code e.getMessage()} does not. A local variable assigned from an expression that carries
     * {@code e} carries it too, followed through a few assignments.
     */
    private static boolean carries(Node n, String id) {
        java.util.Set<String> names = new java.util.HashSet<>();
        names.add(id);
        Node scope = n.findAncestor(MethodDeclaration.class).map(m -> (Node) m)
                .or(() -> n.findAncestor(ConstructorDeclaration.class).map(c -> (Node) c)).orElse(null);
        if (scope != null) {
            for (int round = 0; round < 4; round++) {
                boolean grew = false;
                for (VariableDeclarator d : scope.findAll(VariableDeclarator.class)) {
                    if (!names.contains(d.getNameAsString()) && d.getInitializer().map(i -> carriesAny(i, names)).orElse(false)) {
                        names.add(d.getNameAsString());
                        grew = true;
                    }
                }
                if (!grew) {
                    break;
                }
            }
        }
        return carriesAny(n, names);
    }

    private static boolean carriesAny(Node n, java.util.Set<String> ids) {
        List<NameExpr> uses = new java.util.ArrayList<>(n.findAll(NameExpr.class));
        if (n instanceof NameExpr self) {
            uses.add(self);
        }
        for (NameExpr use : uses) {
            if (!ids.contains(use.getNameAsString())) {
                continue;
            }
            boolean readsMessageOnly = use.getParentNode().filter(p -> p instanceof MethodCallExpr m
                    && m.getScope().map(sc -> sc == use).orElse(false) && MESSAGE_ONLY.contains(m.getNameAsString())).isPresent();
            if (!readsMessageOnly) {
                return true;
            }
        }
        return false;
    }

    /** The calls in the enclosing method that start after the node ends. */
    private static List<MethodCallExpr> callsAfter(Node n) {
        Node scope = n.findAncestor(MethodDeclaration.class).map(m -> (Node) m)
                .or(() -> n.findAncestor(ConstructorDeclaration.class).map(c -> (Node) c)).orElse(null);
        if (scope == null || n.getEnd().isEmpty()) {
            return List.of();
        }
        var end = n.getEnd().get();
        return scope.findAll(MethodCallExpr.class).stream()
                .filter(c -> c.getBegin().map(b -> b.isAfter(end)).orElse(false)).toList();
    }

    /** True for a node that starts a new scope of code: a lambda, a class, a method, a constructor or an initialiser. */
    private static boolean boundary(Node n) {
        return n instanceof LambdaExpr || n instanceof ClassOrInterfaceDeclaration || n instanceof MethodDeclaration
                || n instanceof ConstructorDeclaration || n instanceof InitializerDeclaration
                || n instanceof ObjectCreationExpr o && o.getAnonymousClassBody().isPresent();
    }

    /** The nearest catch clause the node is inside, without leaving its method, lambda or class. */
    private static CatchClause enclosingCatch(Node n) {
        for (Optional<Node> a = n.getParentNode(); a.isPresent(); a = a.get().getParentNode()) {
            if (a.get() instanceof CatchClause c) {
                return c;
            }
            if (boundary(a.get())) {
                return null;
            }
        }
        return null;
    }

    /**
     * True when the exception class is known and none of its constructors take anything that could be a cause: a class
     * declared in this file, or a JDK class (checked by reflection). False when the class is not known, so a project
     * class from another file is assumed to be able to take one.
     */
    private static boolean causeUnsupported(ObjectCreationExpr o, com.codeclear.rules.CCCodeModel model) {
        String type = o.getType().getNameAsString();
        Optional<ClassOrInterfaceDeclaration> decl = o.findCompilationUnit().flatMap(cu -> cu.findAll(ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals(type)).findFirst());
        if (decl.isPresent()) {
            if (decl.get().getConstructors().isEmpty()) {
                return false;
            }
            for (ConstructorDeclaration c : decl.get().getConstructors()) {
                for (Parameter p : c.getParameters()) {
                    String t = simpleName(p.getType());
                    if (t.endsWith("Throwable") || t.endsWith("Exception") || t.endsWith("Error") || t.equals("Object")) {
                        return false;
                    }
                }
            }
            return true;
        }
        String qualified = model == null ? null : model.typeOf(o);
        if (qualified != null && (qualified.startsWith("java.") || qualified.startsWith("javax."))) {
            try {
                for (java.lang.reflect.Constructor<?> c : Class.forName(qualified).getConstructors()) {
                    for (Class<?> p : c.getParameterTypes()) {
                        if (Throwable.class.isAssignableFrom(p) || p.isAssignableFrom(Throwable.class)) {
                            return false;
                        }
                    }
                }
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                return false;
            }
        }
        return false;
    }

    /** A static final field of the enclosing classes whose comment calls it an identity sentinel. */
    private static boolean identitySentinel(NameExpr use) {
        for (Optional<ClassOrInterfaceDeclaration> c = use.findAncestor(ClassOrInterfaceDeclaration.class); c.isPresent();
             c = c.get().findAncestor(ClassOrInterfaceDeclaration.class)) {
            for (FieldDeclaration f : c.get().getFields()) {
                for (VariableDeclarator v : f.getVariables()) {
                    if (v.getNameAsString().equals(use.getNameAsString()) && f.isStatic() && f.isFinal()) {
                        Optional<Comment> comment = f.getComment();
                        return comment.isPresent() && IDENTITY_SENTINEL.matcher(comment.get().getContent()).find();
                    }
                }
            }
        }
        return false;
    }

    /** True when the field is private and final and every use of its name in the class is inside a synchronized block or method. */
    private static boolean usedOnlyInSynchronized(FieldDeclaration f) {
        if (!f.isPrivate() || !f.isFinal()) {
            return false;
        }
        String name = f.getVariable(0).getNameAsString();
        Optional<ClassOrInterfaceDeclaration> cls = f.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty()) {
            return false;
        }
        int uses = 0;
        for (NameExpr use : cls.get().findAll(NameExpr.class)) {
            if (use.getNameAsString().equals(name)) {
                uses++;
                if (!inSynchronized(use)) {
                    return false;
                }
            }
        }
        for (FieldAccessExpr use : cls.get().findAll(FieldAccessExpr.class)) {
            if (use.getNameAsString().equals(name)) {
                uses++;
                if (!inSynchronized(use)) {
                    return false;
                }
            }
        }
        return uses > 0;
    }

    private static boolean inSynchronized(Node n) {
        for (Optional<Node> a = n.getParentNode(); a.isPresent(); a = a.get().getParentNode()) {
            if (a.get() instanceof SynchronizedStmt) {
                return true;
            }
            if (a.get() instanceof MethodDeclaration m) {
                return m.isSynchronized();
            }
        }
        return false;
    }

    /**
     * A return, throw, break or continue that leaves a {@code finally} block, which discards the result or exception the
     * {@code try} was about to deliver. Not counted: anything inside a nested lambda, class or method; a break or
     * continue whose loop (or, for break, switch) is inside the finally; a throw inside a nested try block that has a
     * catch clause (taken as caught, whatever it catches).
     */
    private static boolean escapesFinally(Node jump) {
        Node below = jump;
        for (Optional<Node> parent = jump.getParentNode(); parent.isPresent(); parent = parent.get().getParentNode()) {
            Node a = parent.get();
            final Node child = below;
            if (boundary(a)) {
                return false;
            }
            if (a instanceof TryStmt t) {
                if (t.getFinallyBlock().map(fb -> fb == child).orElse(false)) {
                    return true;
                }
                if (jump instanceof ThrowStmt && child == t.getTryBlock() && !t.getCatchClauses().isEmpty()) {
                    return false;
                }
            }
            if ((jump instanceof BreakStmt || jump instanceof ContinueStmt)
                    && (a instanceof ForStmt || a instanceof ForEachStmt || a instanceof WhileStmt || a instanceof DoStmt
                    || (jump instanceof BreakStmt && a instanceof SwitchStmt))) {
                return false;
            }
            below = a;
        }
        return false;
    }
}
