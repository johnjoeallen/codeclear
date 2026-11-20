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
import com.github.javaparser.ast.stmt.Statement;
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
            case "catches": return List.copyOf(n.findAll(CatchClause.class));
            case "assignments": return List.copyOf(n.findAll(com.github.javaparser.ast.expr.AssignExpr.class));
            case "callsAfter": return callsAfter(n);
            case "enclosingCatch": return enclosingCatch(n);
            case "enclosingClassMethods": return enclosingClassMethods(n);
            case "enclosingMethod": return n.findAncestor(MethodDeclaration.class).orElse(null);
            case "enclosingClassAnnotations":
                return n.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(c -> c.getAnnotations().stream().map(a -> a.getNameAsString()).toList()).orElse(List.of());
            case "isDiscarded": return n instanceof MethodCallExpr c && isDiscarded(c);
            case "sameClassTransactionalTarget": return n instanceof MethodCallExpr c ? sameClassTransactionalTarget(c) : null;
            case "enclosingIterations": return enclosingIterations(n);
            case "iteratedCollection": return iteratedCollection(n);
            case "iteratedType": return iteratedType(n, model);
            case "followedByExit": return n instanceof MethodCallExpr c && followedByExit(c);
            case "updatesCurrentKey": return n instanceof MethodCallExpr c && updatesCurrentKey(c);
            case "isThrowable": return n instanceof com.github.javaparser.ast.expr.Expression e && isThrowable(e, model);
            case "inDataClass": return inDataClass(n);
            case "chainCalls": return n instanceof MethodCallExpr c ? chainCalls(c) : List.of();
            case "chainStartsStream": return n instanceof MethodCallExpr c && chainStartsStream(c);
            case "hasSideEffectStage": return n instanceof MethodCallExpr c && hasSideEffectStage(c);
            case "holderInitializer": return n instanceof MethodCallExpr c ? holderInitializer(c) : null;
            case "enclosingCallName": return n.findAncestor(MethodCallExpr.class).map(MethodCallExpr::getNameAsString).orElse(null);
            case "enclosingCalls": {
                List<String> names = new java.util.ArrayList<>();
                for (Optional<Node> p = n.getParentNode(); p.isPresent(); p = p.get().getParentNode()) {
                    if (p.get() instanceof MethodCallExpr c) {
                        names.add(c.getNameAsString());
                    }
                }
                return names;
            }
            case "parent": return n.getParentNode().orElse(null);
            case "restatesCode": return n instanceof Comment c && restates(c, false);
            case "restatingFirst": return n instanceof Comment c && restates(c, true);
            case "restatingInFile": return n instanceof Comment c ? (long) restatingComments(c).size() : 0L;
            case "blockSize": case "codeLines": case "codeScore": case "blockText":
                return n instanceof Comment c ? commentBlock(c, name) : null;
            case "identifiers": {
                // Names used in the code, never text inside a string: variables, fields, and the methods that are called.
                List<String> ids = new java.util.ArrayList<>();
                n.findAll(NameExpr.class).forEach(x -> ids.add(x.getNameAsString()));
                n.findAll(FieldAccessExpr.class).forEach(x -> ids.add(x.getNameAsString()));
                n.findAll(MethodCallExpr.class).forEach(x -> ids.add(x.getNameAsString()));
                return ids;
            }
            case "enclosingLoop": {
                for (Optional<Node> p = n.getParentNode(); p.isPresent(); p = p.get().getParentNode()) {
                    if (p.get() instanceof ForStmt || p.get() instanceof ForEachStmt || p.get() instanceof WhileStmt || p.get() instanceof DoStmt) {
                        return p.get();
                    }
                    if (p.get() instanceof MethodDeclaration || p.get() instanceof LambdaExpr) {
                        return null;
                    }
                }
                return null;
            }
            case "fieldDeclText": {
                if (!(n instanceof NameExpr x)) {
                    return null;
                }
                return n.findAncestor(ClassOrInterfaceDeclaration.class).flatMap(c -> c.getFields().stream()
                        .filter(f -> f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(x.getNameAsString())))
                        .map(Object::toString).findFirst()).orElse(null);
            }
            case "guardedField": return n instanceof com.github.javaparser.ast.stmt.IfStmt i ? guardedField(i) : null;
            case "enclosingStatement": return n.findAncestor(Statement.class).orElse(null);
            case "enclosingClass": return n.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
            case "enclosingMethodName": return n.findAncestor(MethodDeclaration.class).map(MethodDeclaration::getNameAsString).orElse(null);
            default: break;
        }
        if (n instanceof MethodDeclaration m) {
            switch (name) {
                case "isPublic": return m.isPublic() || m.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
                case "classIsPublic": return m.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::isPublic).orElse(false);
                case "returnType": return m.getType().toString();
                case "throwsTypes": return m.getThrownExceptions().stream().map(Type::toString).toList();
                case "annotations": return m.getAnnotations().stream().map(a -> a.getNameAsString()).toList();
                case "isPrivate": return m.isPrivate();
                case "isFinal": return m.isFinal();
                case "assertions": return assertions(m);
                case "usesGeneratedId": return usesGeneratedId(m);
                default: break;
            }
        } else if (n instanceof com.github.javaparser.ast.expr.AnnotationExpr a && name.equals("name")) {
            return a.getNameAsString();
        } else if (name.equals("inSynchronized")) {
            return inSynchronized(n);
        } else if ((n instanceof MethodCallExpr || n instanceof ObjectCreationExpr) && name.equals("closedOrHandedOn")) {
            return closedOrHandedOn(n);
        } else if ((n instanceof MethodCallExpr || n instanceof ObjectCreationExpr) && name.equals("assignedToLocal")) {
            return n.getParentNode().orElse(null) instanceof VariableDeclarator v
                    && v.getParentNode().orElse(null) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr;
        } else if ((n instanceof MethodCallExpr || n instanceof ObjectCreationExpr) && name.equals("inTryResource")) {
            return inTryResource(n);
        } else if (name.equals("inLevelGuard")) {
            return inLevelGuard(n);
        } else if (name.equals("inReadOnlyTransaction")) {
            return inReadOnlyTransaction(n);
        } else if (n instanceof FieldDeclaration f && name.equals("mutatedAfterInit")) {
            return mutatedAfterInit(f);
        } else if (n instanceof FieldDeclaration f && name.equals("assignedAfterInit")) {
            return assignedAfterInit(f);
        } else if (n instanceof Comment c && (name.equals("marker") || name.equals("markerText"))) {
            return commentMarker(c, name.equals("marker"));
        } else if (n instanceof VariableDeclarator v) {
            switch (name) {
                case "name": return v.getNameAsString();
                case "typeName": return simpleName(v.getType());
                case "isLocal": return v.getParentNode().orElse(null) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr;
                case "usedInArithmetic": return usedInArithmetic(v, v.getNameAsString(), v.getParentNode().orElse(null) instanceof FieldDeclaration);
                case "neverReferenced": return neverReferenced(v);
                default: break;
            }
        } else if (n instanceof Parameter p && name.equals("usedInArithmetic")) {
            return usedInArithmetic(p, p.getNameAsString(), false);
        } else if (n instanceof Parameter p && name.equals("inPublicMethod")) {
            return p.getParentNode().orElse(null) instanceof MethodDeclaration m
                    && (m.isPublic() || m.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::isInterface).orElse(false));
        } else if (n instanceof CatchClause c && name.equals("tryCalls")) {
            return c.findAncestor(TryStmt.class).map(t -> (Object) List.copyOf(t.getTryBlock().findAll(MethodCallExpr.class))).orElse(List.of());
        } else if (n instanceof com.github.javaparser.ast.stmt.ForEachStmt f && name.equals("iterable")) {
            return f.getIterable();
        } else if (n instanceof com.github.javaparser.ast.expr.StringLiteralExpr && name.equals("boundName")) {
            return boundName(n);
        } else if (n instanceof com.github.javaparser.ast.expr.StringLiteralExpr s && name.equals("placeholders")) {
            return placeholders(s.asString());
        } else if (n instanceof MethodCallExpr c && name.equals("logArguments")) {
            return logArguments(c, model);
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
        } else if (n instanceof ReturnStmt r) {
            if (name.equals("expression")) {
                return r.getExpression().orElse(null);
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
                case "hasInitializer": return f.getVariable(0).getInitializer().isPresent();
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
        if (name.equals("hasAnnotationMember") && target instanceof MethodDeclaration m && args.size() == 2
                && args.get(0) instanceof String annotation && args.get(1) instanceof String member) {
            return m.getAnnotationByName(annotation).map(a -> a instanceof com.github.javaparser.ast.expr.NormalAnnotationExpr na
                    && na.getPairs().stream().anyMatch(p -> p.getNameAsString().equals(member))).orElse(false);
        }
        if (name.equals("pairedRelease") && target instanceof MethodCallExpr c && args.size() == 1 && args.get(0) instanceof List<?> names) {
            return pairedRelease(c, names);
        }
        if (name.equals("releasedInMethodFinally") && target instanceof MethodCallExpr c && args.size() == 1 && args.get(0) instanceof List<?> names) {
            return releasedInMethodFinally(c, names);
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

    /**
     * The call is a statement of its own. The body of an expression lambda ({@code x -> f(x)}) is parsed as a statement too,
     * but its value is the lambda's result, so it is not discarded.
     */
    private static boolean isDiscarded(MethodCallExpr c) {
        return c.getParentNode().orElse(null) instanceof com.github.javaparser.ast.stmt.ExpressionStmt s && s.getExpression() == c
                && !(s.getParentNode().orElse(null) instanceof LambdaExpr);
    }

    private static final Pattern TODO_MARKER = Pattern.compile("\\b(TODO|FIXME|HACK|XXX)\\b");

    /**
     * The first TODO-style marker inside a comment. As a node ({@code node = true}) it is positioned on the line of the marker,
     * so a long comment is reported where its TODO is and not where the comment starts; as text it is that line, without the
     * comment decoration ({@code //}, {@code *}).
     */
    private static Object commentMarker(Comment comment, boolean node) {
        java.util.regex.Matcher m = TODO_MARKER.matcher(comment.getContent());
        if (!m.find() || comment.getBegin().isEmpty()) {
            return null;
        }
        String content = comment.getContent();
        int line = comment.getBegin().get().line + (int) content.substring(0, m.start()).chars().filter(ch -> ch == '\n').count();
        if (node) {
            com.github.javaparser.ast.comments.LineComment at = new com.github.javaparser.ast.comments.LineComment();
            at.setRange(new com.github.javaparser.Range(new com.github.javaparser.Position(line, 1), new com.github.javaparser.Position(line, 1)));
            return at;
        }
        int start = content.lastIndexOf('\n', m.start()) + 1;
        int end = content.indexOf('\n', m.start());
        String text = content.substring(start, end < 0 ? content.length() : end).strip().replaceFirst("^[*/\\s]+", "").strip();
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    // ---- data classes, arithmetic, streams, per-thread holders ---------------------------------------------------

    private static final Pattern DATA_CLASS_NAME = Pattern.compile(".*(Dto|DTO|Request|Response|Event|Payload|Message|Entity|Properties|Command|Resource)$");
    private static final List<String> DATA_ANNOTATIONS = List.of("Entity", "Embeddable", "MappedSuperclass", "Data", "Value",
            "ConfigurationProperties", "Document", "Table", "JsonInclude", "JsonIgnoreProperties");

    /**
     * The class around the node holds data that is exchanged or stored: an entity or embeddable, a Lombok data class, a
     * configuration properties class, a serializable class, or a class named like a DTO, request, response, event or message.
     */
    private static boolean inDataClass(Node n) {
        Optional<ClassOrInterfaceDeclaration> cls = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty() || cls.get().isInterface()) {
            return false;
        }
        ClassOrInterfaceDeclaration c = cls.get();
        return c.getAnnotations().stream().anyMatch(a -> DATA_ANNOTATIONS.contains(a.getNameAsString()))
                || c.getImplementedTypes().stream().anyMatch(t -> t.getNameAsString().equals("Serializable"))
                || DATA_CLASS_NAME.matcher(c.getNameAsString()).matches();
    }

    private static boolean arithmetic(BinaryExpr.Operator op) {
        return op == BinaryExpr.Operator.PLUS || op == BinaryExpr.Operator.MINUS || op == BinaryExpr.Operator.MULTIPLY
                || op == BinaryExpr.Operator.DIVIDE || op == BinaryExpr.Operator.REMAINDER;
    }

    /** {@code "a" + b + c}: a plus whose left side, followed down through pluses, is a string literal, or whose right side is one. */
    private static boolean isConcatenation(BinaryExpr b) {
        if (b.getOperator() != BinaryExpr.Operator.PLUS) {
            return false;
        }
        if (b.getRight() instanceof com.github.javaparser.ast.expr.StringLiteralExpr) {
            return true;
        }
        com.github.javaparser.ast.expr.Expression left = b.getLeft();
        while (left instanceof BinaryExpr l && l.getOperator() == BinaryExpr.Operator.PLUS) {
            if (l.getRight() instanceof com.github.javaparser.ast.expr.StringLiteralExpr) {
                return true;
            }
            left = l.getLeft();
        }
        return left instanceof com.github.javaparser.ast.expr.StringLiteralExpr;
    }

    /**
     * The variable (a field, a local or a parameter) is used in arithmetic: an operand of {@code + - * / %} that is not a
     * string concatenation, the target of a compound assignment, or the argument of {@code Math.round/ceil/floor/rint}.
     */
    private static boolean usedInArithmetic(Node declaration, String name, boolean field) {
        Node scope = field ? declaration.findAncestor(ClassOrInterfaceDeclaration.class).map(c -> (Node) c).orElse(null)
                : declaration.findAncestor(com.github.javaparser.ast.body.CallableDeclaration.class).map(c -> (Node) c)
                        .or(() -> declaration.findAncestor(ClassOrInterfaceDeclaration.class)).orElse(null);
        if (scope == null) {
            return false;
        }
        for (NameExpr use : scope.findAll(NameExpr.class)) {
            if (!use.getNameAsString().equals(name) || !(use.getParentNode().orElse(null) instanceof Node parent)) {
                continue;
            }
            if (parent instanceof BinaryExpr b && arithmetic(b.getOperator()) && !isConcatenation(b)) {
                return true;
            }
            if (parent instanceof com.github.javaparser.ast.expr.AssignExpr a && a.getTarget() == use
                    && a.getOperator() != com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN) {
                return true;
            }
            if (parent instanceof MethodCallExpr m && m.getScope().map(s -> s.toString().equals("Math")).orElse(false)
                    && List.of("round", "ceil", "floor", "rint").contains(m.getNameAsString())) {
                return true;
            }
        }
        return false;
    }

    /** A local variable that no later code mentions. */
    private static boolean neverReferenced(VariableDeclarator v) {
        Node scope = v.findAncestor(com.github.javaparser.ast.body.CallableDeclaration.class).map(c -> (Node) c).orElse(null);
        return scope != null && v.getParentNode().orElse(null) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr
                && scope.findAll(NameExpr.class).stream().noneMatch(x -> x.getNameAsString().equals(v.getNameAsString()))
                // walk::iterator names the variable as the scope of a method reference, which is not a NameExpr
                && scope.findAll(com.github.javaparser.ast.expr.MethodReferenceExpr.class).stream()
                        .noneMatch(r -> r.getScope().toString().equals(v.getNameAsString()));
    }

    /** The calls this one is made on, nearest first: {@code a.stream().map(f).count()} gives {@code map}, {@code stream}. */
    private static List<MethodCallExpr> chainCalls(MethodCallExpr call) {
        List<MethodCallExpr> out = new java.util.ArrayList<>();
        for (com.github.javaparser.ast.expr.Expression s = call.getScope().orElse(null); s instanceof MethodCallExpr m;
             s = m.getScope().orElse(null)) {
            out.add(m);
        }
        return out;
    }

    private static final List<String> STREAM_TYPES = List.of("Stream", "IntStream", "LongStream", "DoubleStream");

    /** The call chain starts a stream: {@code x.stream()}, {@code parallelStream()}, {@code Stream.of(...)}, {@code Arrays.stream(...)}. */
    private static boolean chainStartsStream(MethodCallExpr call) {
        List<MethodCallExpr> chain = new java.util.ArrayList<>(chainCalls(call));
        chain.add(0, call);
        return chain.stream().anyMatch(c -> List.of("stream", "parallelStream", "chars").contains(c.getNameAsString()) && c.getArguments().isEmpty()
                || c.getScope().map(s -> STREAM_TYPES.contains(s.toString()) || s.toString().equals("Arrays") && c.getNameAsString().equals("stream")).orElse(false));
    }

    private static final List<String> SIDE_EFFECT_CALLS = List.of("add", "addAll", "put", "putAll", "save", "saveAll", "send", "delete",
            "deleteAll", "deleteById", "remove", "publish", "write", "increment", "incrementAndGet", "getAndIncrement");

    /** A lambda that changes something outside itself: an assignment to a variable it did not declare, or a call that adds, saves or sends. */
    private static boolean lambdaHasSideEffect(LambdaExpr l) {
        java.util.Set<String> own = new java.util.HashSet<>();
        l.getParameters().forEach(p -> own.add(p.getNameAsString()));
        l.findAll(VariableDeclarator.class).forEach(d -> own.add(d.getNameAsString()));
        boolean assigns = l.findAll(com.github.javaparser.ast.expr.AssignExpr.class).stream().anyMatch(a ->
                !(a.getTarget() instanceof NameExpr t && own.contains(t.getNameAsString())));
        boolean calls = l.findAll(MethodCallExpr.class).stream().anyMatch(c -> SIDE_EFFECT_CALLS.contains(c.getNameAsString())
                && !(c.getScope().orElse(null) instanceof NameExpr s && own.contains(s.getNameAsString())));
        return assigns || calls;
    }

    /** Some {@code peek} or {@code map} stage earlier in the chain has a lambda that changes something outside itself. */
    private static boolean hasSideEffectStage(MethodCallExpr call) {
        return chainCalls(call).stream().anyMatch(c -> List.of("peek", "map").contains(c.getNameAsString())
                && c.getArguments().stream().anyMatch(a -> a instanceof LambdaExpr l && lambdaHasSideEffect(l)
                || a instanceof com.github.javaparser.ast.expr.MethodReferenceExpr r && SIDE_EFFECT_CALLS.contains(r.getIdentifier())));
    }

    /** For {@code holder.set(...)} where {@code holder} is a field of the class, the text of the field's initializer, else null. */
    private static String holderInitializer(MethodCallExpr call) {
        String holder = call.getScope().map(s -> s instanceof FieldAccessExpr f && f.getScope() instanceof com.github.javaparser.ast.expr.ThisExpr
                ? f.getNameAsString() : s instanceof NameExpr n ? n.getNameAsString() : null).orElse(null);
        if (holder == null) {
            return null;
        }
        return call.findAncestor(ClassOrInterfaceDeclaration.class).stream()
                .flatMap(c -> c.getFields().stream()).flatMap(f -> f.getVariables().stream())
                .filter(d -> d.getNameAsString().equals(holder)).findFirst()
                .flatMap(VariableDeclarator::getInitializer).map(Object::toString).orElse(null);
    }

    /**
     * Some finally block in the method of the call, or a {@code doFinally(...)} callback, makes one of these calls on the same
     * receiver, directly or through a helper of the same class that does.
     */
    private static boolean releasedInMethodFinally(MethodCallExpr acquire, List<?> releaseNames) {
        if (acquire.getScope().isEmpty()) {
            return false;
        }
        String holder = acquire.getScope().get().toString();
        Node scope = acquire.findAncestor(com.github.javaparser.ast.body.CallableDeclaration.class).map(c -> (Node) c)
                .or(() -> acquire.findAncestor(InitializerDeclaration.class)).orElse(null);
        if (scope == null) {
            return false;
        }
        List<Node> cleanups = new java.util.ArrayList<>();
        scope.findAll(TryStmt.class).forEach(t -> t.getFinallyBlock().ifPresent(cleanups::add));
        scope.findAll(MethodCallExpr.class).stream().filter(c -> c.getNameAsString().equals("doFinally")).forEach(cleanups::add);
        return cleanups.stream().anyMatch(block -> releases(block, holder, releaseNames, acquire, 2));
    }

    /** The block makes a release call on the holder, or calls a method of the same class that does (to the given depth). */
    private static boolean releases(Node block, String holder, List<?> releaseNames, Node context, int depth) {
        for (MethodCallExpr c : block.findAll(MethodCallExpr.class)) {
            if (releaseNames.contains(c.getNameAsString()) && c.getScope().map(s -> s.toString().equals(holder)).orElse(false)) {
                return true;
            }
            if (depth > 1 && c.getScope().isEmpty()) {
                for (MethodDeclaration helper : context.findAncestor(ClassOrInterfaceDeclaration.class).stream()
                        .flatMap(k -> k.getMethodsByName(c.getNameAsString()).stream()).toList()) {
                    if (releases(helper, holder, releaseNames, context, depth - 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The acquire and a release of the same holder are the two branches of one {@code if}: {@code if (previous == null)
     * MDC.remove(k); else MDC.put(k, previous);}. That puts a value back and does not leak one.
     */
    private static boolean pairedRelease(MethodCallExpr acquire, List<?> releaseNames) {
        if (acquire.getScope().isEmpty()) {
            return false;
        }
        String holder = acquire.getScope().get().toString();
        for (Optional<Node> p = acquire.getParentNode(); p.isPresent(); p = p.get().getParentNode()) {
            if (p.get() instanceof com.github.javaparser.ast.stmt.IfStmt i) {
                Node mine = i.getThenStmt().isAncestorOf(acquire) ? i.getThenStmt() : i.getElseStmt().orElse(null);
                Node other = mine == i.getThenStmt() ? i.getElseStmt().orElse(null) : i.getThenStmt();
                if (other != null && other.findAll(MethodCallExpr.class).stream().anyMatch(c -> releaseNames.contains(c.getNameAsString())
                        && c.getScope().map(s -> s.toString().equals(holder)).orElse(false))) {
                    return true;
                }
            }
            if (p.get() instanceof MethodDeclaration) {
                break;
            }
        }
        return false;
    }

    // ---- tests ----------------------------------------------------------------------------------------------------

    private static boolean assertionName(String n) {
        return n.startsWith("assert") || n.startsWith("verify") || n.startsWith("check") || n.startsWith("expect")
                || n.startsWith("andExpect") || n.equals("fail") || n.equals("then") || n.equals("should");
    }

    /**
     * The assertion calls in a test, as the calls that decide whether it can fail. Any call named {@code assert...},
     * {@code verify...}, {@code check...}, {@code expect...}, {@code andExpect...}, {@code fail}, {@code then} or
     * {@code should} counts, which includes helpers that delegate to assertions. {@code assertThat(x).isNotNull()} is
     * reported as the {@code isNotNull} call, because that is all it asserts; any other {@code assertThat(...)} as itself.
     */
    private static List<MethodCallExpr> assertions(MethodDeclaration m) {
        List<MethodCallExpr> out = new java.util.ArrayList<>();
        for (MethodCallExpr call : m.findAll(MethodCallExpr.class)) {
            if (!assertionName(call.getNameAsString())) {
                // A call to a helper of the same class that asserts, however it is named: testAdding(1, 2), parse("...").
                if (delegatesAssertions(call, 2, new java.util.HashSet<>())) {
                    out.add(call);
                }
                continue;
            }
            if (call.getNameAsString().equals("assertThat")) {
                MethodCallExpr next = chained(call);
                if (next != null && next.getNameAsString().equals("isNotNull") && chained(next) == null) {
                    out.add(next);
                    continue;
                }
            }
            out.add(call);
        }
        return out;
    }

    /**
     * The call is unqualified (or on {@code this}) and resolves by name and parameter count to a method of the same class whose
     * body asserts, directly or through another such helper, to the given depth.
     */
    private static boolean delegatesAssertions(MethodCallExpr call, int depth, java.util.Set<MethodDeclaration> seen) {
        if (depth == 0 || call.getScope().isPresent() && !(call.getScope().get() instanceof com.github.javaparser.ast.expr.ThisExpr)) {
            return false;
        }
        return call.findAncestor(ClassOrInterfaceDeclaration.class).stream()
                .flatMap(c -> c.getMethodsByName(call.getNameAsString()).stream())
                .filter(h -> acceptsArguments(h, call.getArguments().size()) && h.getBody().isPresent() && seen.add(h))
                .anyMatch(h -> h.findAll(MethodCallExpr.class).stream().anyMatch(inner ->
                        assertionName(inner.getNameAsString()) || delegatesAssertions(inner, depth - 1, seen)));
    }

    /** The method takes this many arguments: exactly, or at least all but the last with a varargs parameter. */
    private static boolean acceptsArguments(MethodDeclaration method, int count) {
        int declared = method.getParameters().size();
        return declared == count || declared > 0 && method.getParameter(declared - 1).isVarArgs() && count >= declared - 1;
    }

    /** The call made directly on the result of this one: {@code a.b()} for {@code a}. */
    private static MethodCallExpr chained(MethodCallExpr call) {
        return call.getParentNode().filter(p -> p instanceof MethodCallExpr outer && outer.getScope().map(s -> s == call).orElse(false))
                .map(p -> (MethodCallExpr) p).orElse(null);
    }

    // ---- transactions -------------------------------------------------------------------------------------------

    /**
     * For an unqualified call, or one on {@code this}, a method of the same class with that name and parameter count
     * that is annotated {@code @Transactional}: calling it from inside the class bypasses the Spring proxy. Null otherwise.
     */
    private static MethodDeclaration sameClassTransactionalTarget(MethodCallExpr call) {
        if (call.getScope().isPresent() && !(call.getScope().get() instanceof com.github.javaparser.ast.expr.ThisExpr)) {
            return null;
        }
        return call.findAncestor(ClassOrInterfaceDeclaration.class).flatMap(c -> c.getMethodsByName(call.getNameAsString()).stream()
                .filter(m -> m.getParameters().size() == call.getArguments().size() && m.getAnnotationByName("Transactional").isPresent())
                .findFirst()).orElse(null);
    }

    // ---- logging ------------------------------------------------------------------------------------------------

    /** The number of unescaped {@code {}} placeholders in a log format. */
    private static int placeholders(String format) {
        int count = 0;
        for (int i = 0; i + 1 < format.length(); i++) {
            if (format.charAt(i) == '{' && format.charAt(i + 1) == '}' && (i == 0 || format.charAt(i - 1) != '\\')) {
                count++;
                i++;
            }
        }
        return count;
    }

    /**
     * The arguments a log call passes for its placeholders: everything after the format, except a trailing exception that
     * is not matched by a placeholder (SLF4J treats it as the throwable, not as an argument).
     */
    private static int logArguments(MethodCallExpr call, com.codeclear.rules.CCCodeModel model) {
        List<com.github.javaparser.ast.expr.Expression> args = call.getArguments();
        if (args.isEmpty() || !(args.get(0) instanceof com.github.javaparser.ast.expr.StringLiteralExpr format)) {
            return 0;
        }
        int after = args.size() - 1;
        if (after == 1 && args.get(1) instanceof com.github.javaparser.ast.expr.ArrayCreationExpr array && array.getInitializer().isPresent()) {
            return array.getInitializer().get().getValues().size(); // SLF4J takes one Object[] as the argument array
        }
        if (after > placeholders(format.asString()) && isThrowable(args.get(args.size() - 1), model)) {
            after--;
        }
        return after;
    }

    private static final Pattern THROWABLE_NAME = Pattern.compile("(?i)(e|ex|exc|exception|err|error|t|th|throwable|cause)");

    /** The expression is a Throwable: by its resolved type, or when that is unknown by the name a caught exception usually has. */
    private static boolean isThrowable(com.github.javaparser.ast.expr.Expression e, com.codeclear.rules.CCCodeModel model) {
        if (e instanceof NameExpr n && declaredAsThrowable(n)) {
            return true;
        }
        String qualified = model == null ? null : model.typeOf(e);
        if (qualified != null) {
            try {
                return Throwable.class.isAssignableFrom(Class.forName(qualified));
            } catch (ClassNotFoundException | LinkageError ex) {
                return THROWABLE_NAME.matcher(e.toString()).matches() || qualified.endsWith("Exception") || qualified.endsWith("Error");
            }
        }
        return e instanceof NameExpr n && THROWABLE_NAME.matcher(n.getNameAsString()).matches();
    }

    private static boolean throwableName(String type) {
        return type.endsWith("Exception") || type.endsWith("Error") || type.equals("Throwable");
    }

    /**
     * The name is a catch parameter, or a variable or parameter declared with an exception type, or with a type variable
     * bounded by one ({@code <E extends Exception> ... E e}). Needed when the exception class itself cannot be resolved.
     */
    private static boolean declaredAsThrowable(NameExpr use) {
        String name = use.getNameAsString();
        for (Optional<Node> a = use.getParentNode(); a.isPresent(); a = a.get().getParentNode()) {
            if (a.get() instanceof CatchClause c && c.getParameter().getNameAsString().equals(name)) {
                return true;
            }
        }
        Optional<MethodDeclaration> method = use.findAncestor(MethodDeclaration.class);
        Type declared = null;
        if (method.isPresent()) {
            for (Parameter p : method.get().getParameters()) {
                if (p.getNameAsString().equals(name)) {
                    declared = p.getType();
                }
            }
            for (VariableDeclarator d : method.get().findAll(VariableDeclarator.class)) {
                if (d.getNameAsString().equals(name)) {
                    declared = d.getType();
                }
            }
        }
        if (declared == null) {
            return false;
        }
        String type = simpleName(declared);
        if (throwableName(type)) {
            return true;
        }
        return method.get().getTypeParameters().stream().anyMatch(tp -> tp.getNameAsString().equals(type)
                && tp.getTypeBound().stream().anyMatch(b -> throwableName(b.getNameAsString())));
    }

    // ---- iteration ----------------------------------------------------------------------------------------------

    /** For-each statements, and {@code x.forEach(lambda)} calls, that the node is inside, nearest first. */
    private static List<Node> enclosingIterations(Node n) {
        List<Node> out = new java.util.ArrayList<>();
        Node below = n;
        for (Optional<Node> parent = n.getParentNode(); parent.isPresent(); parent = parent.get().getParentNode()) {
            Node a = parent.get();
            if (a instanceof MethodDeclaration || a instanceof ConstructorDeclaration || a instanceof InitializerDeclaration
                    || a instanceof ClassOrInterfaceDeclaration) {
                break;
            }
            if (a instanceof com.github.javaparser.ast.stmt.ForEachStmt f && f.getBody() == below) {
                out.add(f);
            } else if (a instanceof MethodCallExpr c && c.getNameAsString().equals("forEach") && c.getScope().isPresent()
                    && !c.getArguments().isEmpty() && c.getArgument(0) == below && below instanceof LambdaExpr) {
                out.add(c);
            }
            below = a;
        }
        return out;
    }

    /**
     * The collection an iteration walks, as text: the variable in {@code for (x : c)} and {@code c.forEach(...)}, and
     * {@code c} in {@code c.keySet()}, {@code c.values()}, {@code c.entrySet()} and {@code c.stream()} (views of {@code c},
     * so changing {@code c} still breaks the loop). A copy such as {@code new ArrayList<>(c)} gives null.
     */
    private static String iteratedCollection(Node n) {
        com.github.javaparser.ast.expr.Expression source = iteratedExpression(n);
        if (source instanceof MethodCallExpr c && c.getScope().isPresent()
                && List.of("keySet", "values", "entrySet", "stream").contains(c.getNameAsString())) {
            source = c.getScope().get();
        }
        return source instanceof NameExpr || (source instanceof FieldAccessExpr f && f.getScope() instanceof com.github.javaparser.ast.expr.ThisExpr)
                ? source.toString() : null;
    }

    private static String iteratedType(Node n, com.codeclear.rules.CCCodeModel model) {
        com.github.javaparser.ast.expr.Expression source = iteratedExpression(n);
        if (source instanceof MethodCallExpr view && view.getScope().isPresent()
                && List.of("keySet", "values", "entrySet", "stream").contains(view.getNameAsString())) {
            source = view.getScope().get(); // the type of a view is the type of the collection behind it
        }
        String resolved = source == null || model == null ? null : model.typeOf(source);
        if (resolved != null && resolved.startsWith("java.util.concurrent.")) {
            return resolved;
        }
        // A variable declared as the interface (Map, List) but created as a concurrent class is a concurrent collection.
        String created = source == null ? null : createdAs(source);
        return created != null && (created.startsWith("Concurrent") || created.startsWith("CopyOnWrite"))
                ? "java.util.concurrent." + created : resolved;
    }

    /** The class a variable or field is created as, from its initializer or an assignment: {@code x = new ConcurrentHashMap<>()}. */
    private static String createdAs(com.github.javaparser.ast.expr.Expression variable) {
        String name = variable instanceof NameExpr n ? n.getNameAsString()
                : variable instanceof FieldAccessExpr f && f.getScope() instanceof com.github.javaparser.ast.expr.ThisExpr ? f.getNameAsString() : null;
        if (name == null) {
            return null;
        }
        Node scope = variable.findAncestor(ClassOrInterfaceDeclaration.class).map(c -> (Node) c).orElse(null);
        if (scope == null) {
            return null;
        }
        for (VariableDeclarator d : scope.findAll(VariableDeclarator.class)) {
            if (d.getNameAsString().equals(name) && d.getInitializer().orElse(null) instanceof ObjectCreationExpr o) {
                return o.getType().getNameAsString();
            }
        }
        for (com.github.javaparser.ast.expr.AssignExpr a : scope.findAll(com.github.javaparser.ast.expr.AssignExpr.class)) {
            String target = a.getTarget() instanceof NameExpr t ? t.getNameAsString()
                    : a.getTarget() instanceof FieldAccessExpr f ? f.getNameAsString() : null;
            if (name.equals(target) && a.getValue() instanceof ObjectCreationExpr o) {
                return o.getType().getNameAsString();
            }
        }
        return null;
    }

    /**
     * {@code put(k, v)} where {@code k} is the key being visited, so the call only replaces the value of an existing entry
     * and does not change the map's structure: {@code for (k : m.keySet()) m.put(k, ...)}, {@code e.getKey()} for an entry
     * {@code e}, and the key parameter of {@code m.forEach((k, v) -> ...)}.
     */
    private static boolean updatesCurrentKey(MethodCallExpr call) {
        if (!call.getNameAsString().equals("put") || call.getArguments().isEmpty()) {
            return false;
        }
        com.github.javaparser.ast.expr.Expression key = call.getArgument(0);
        for (Node it : enclosingIterations(call)) {
            String variable = null;
            if (it instanceof com.github.javaparser.ast.stmt.ForEachStmt f) {
                variable = f.getVariable().getVariable(0).getNameAsString();
            } else if (it instanceof MethodCallExpr c && c.getArgument(0) instanceof LambdaExpr l && !l.getParameters().isEmpty()) {
                variable = l.getParameter(0).getNameAsString();
            }
            final String visited = variable;
            if (visited != null && (key instanceof NameExpr k && k.getNameAsString().equals(visited)
                    || key instanceof MethodCallExpr g && g.getNameAsString().equals("getKey")
                    && g.getScope().map(sc -> sc.toString().equals(visited)).orElse(false))) {
                return true;
            }
        }
        return false;
    }

    private static com.github.javaparser.ast.expr.Expression iteratedExpression(Node n) {
        if (n instanceof com.github.javaparser.ast.stmt.ForEachStmt f) {
            return f.getIterable();
        }
        return n instanceof MethodCallExpr c && c.getNameAsString().equals("forEach") ? c.getScope().orElse(null) : null;
    }

    /** The call is a statement and the next statement in its block is a break, continue or return. */
    private static boolean followedByExit(MethodCallExpr call) {
        Optional<com.github.javaparser.ast.stmt.ExpressionStmt> stmt = call.findAncestor(com.github.javaparser.ast.stmt.ExpressionStmt.class);
        if (stmt.isEmpty() || stmt.get().getExpression() != call
                || !(stmt.get().getParentNode().orElse(null) instanceof com.github.javaparser.ast.stmt.BlockStmt block)) {
            return false;
        }
        int at = block.getStatements().indexOf(stmt.get());
        if (at < 0 || at + 1 >= block.getStatements().size()) {
            return false;
        }
        Statement next = block.getStatement(at + 1);
        return next instanceof BreakStmt || next instanceof ReturnStmt || next instanceof ContinueStmt;
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

    private static final Pattern LEVEL_GUARD = Pattern.compile("(?s).*\\b(isDebugEnabled|isTraceEnabled|isEnabledFor|isLoggable|isEnabled)\\b.*");

    /** The node is inside the then-branch of an {@code if} whose condition asks whether a log level is enabled. */
    private static boolean inLevelGuard(Node n) {
        Node child = n;
        for (Optional<Node> a = n.getParentNode(); a.isPresent(); child = a.get(), a = a.get().getParentNode()) {
            if (a.get() instanceof MethodDeclaration) {
                return false;
            }
            if (a.get() instanceof com.github.javaparser.ast.stmt.IfStmt i && i.getThenStmt() == child
                    && LEVEL_GUARD.matcher(i.getCondition().toString()).matches()) {
                return true;
            }
        }
        return false;
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

    /** The call is, or is inside, a resource of a try-with-resources header (not its body). */
    /**
     * The value initialises a local variable that the method then closes ({@code v.close()}), returns, stores somewhere
     * other than a local, or hands to another call: someone else owns it, or it is released.
     */
    private static boolean closedOrHandedOn(Node n) {
        if (!(n.getParentNode().orElse(null) instanceof VariableDeclarator v)) {
            return false;
        }
        String local = v.getNameAsString();
        Optional<MethodDeclaration> m = n.findAncestor(MethodDeclaration.class);
        Node scope = m.isPresent() ? m.get() : n.findAncestor(com.github.javaparser.ast.body.CallableDeclaration.class).orElse(null);
        if (scope == null) {
            return false;
        }
        java.util.function.Predicate<com.github.javaparser.ast.expr.Expression> isLocal =
                e -> e instanceof NameExpr x && x.getNameAsString().equals(local);
        for (MethodCallExpr c : scope.findAll(MethodCallExpr.class)) {
            if (c.getScope().filter(isLocal).isPresent() && c.getNameAsString().equals("close")) {
                return true;
            }
            if (c.getArguments().stream().anyMatch(isLocal)) {
                return true;
            }
        }
        for (ReturnStmt r : scope.findAll(ReturnStmt.class)) {
            if (r.getExpression().filter(isLocal).isPresent()) {
                return true;
            }
        }
        for (com.github.javaparser.ast.expr.AssignExpr a : scope.findAll(com.github.javaparser.ast.expr.AssignExpr.class)) {
            if (isLocal.test(a.getValue())) {
                return true;
            }
        }
        return scope.findAll(ObjectCreationExpr.class).stream().anyMatch(o -> o.getArguments().stream().anyMatch(isLocal));
    }

    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of("a", "an", "the", "of", "to", "in", "on", "for", "and",
            "or", "is", "are", "be", "this", "that", "it", "we", "with", "by", "at", "as", "from", "new", "set", "get", "if", "then");
    private static final Pattern REASON = Pattern.compile("(?is).*(because|so that|since|why|due to|to avoid|workaround|note:|todo|fixme|hack|must|should|unit|seconds|millis|ms\\b|bytes|otherwise|except|only if|never|always|careful|beware).*");
    private static final Pattern DIRECTIVE = Pattern.compile("(?is).*(eslint|noqa|nolint|checkstyle|nopmd|suppresswarnings|codeclear|copyright|license|generated|auto-generated|spotbugs|sonar|istanbul|prettier|@formatter|fmt:).*");

    private static List<String> tokens(String text) {
        List<String> out = new java.util.ArrayList<>();
        for (String w : text.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (!w.isEmpty() && !STOP_WORDS.contains(w)) {
                out.add(w.endsWith("s") && w.length() > 3 ? w.substring(0, w.length() - 1) : w);
            }
        }
        return out;
    }

    /** The line comments of the file that say again what the statement next to them says. */
    private static List<Comment> restatingComments(Comment from) {
        List<Comment> out = new java.util.ArrayList<>();
        for (Comment c : from.findCompilationUnit().map(cu -> cu.getAllComments()).orElse(List.of())) {
            if (restates(c, false)) {
                out.add(c);
            }
        }
        out.sort(java.util.Comparator.comparingInt(x -> x.getBegin().map(p -> p.line).orElse(0)));
        return out;
    }

    /** A line comment whose words (80 percent or more) are all in the code on the next line, or the line it trails. */
    private static boolean restates(Comment c, boolean firstOnly) {
        if (!(c instanceof com.github.javaparser.ast.comments.LineComment) || c.getBegin().isEmpty()) {
            return false;
        }
        String content = c.getContent().strip();
        if (content.isEmpty() || DIRECTIVE.matcher(content).matches() || REASON.matcher(content).matches()) {
            return false;
        }
        List<String> words = tokens(content);
        if (words.isEmpty()) {
            return false;
        }
        int line = c.getBegin().get().line;
        String code = c.findCompilationUnit().map(cu -> cu.findAll(Statement.class).stream()
                .filter(s -> s.getBegin().isPresent() && (s.getBegin().get().line == line + 1 || s.getEnd().map(e -> e.line == line).orElse(false)
                        && s.getBegin().get().line == line))
                .filter(s -> !(s instanceof com.github.javaparser.ast.stmt.BlockStmt))
                .map(s -> {
                    // The statement as code: a comment attached to it is not part of what the comment is compared with.
                    Statement copy = s.clone();
                    copy.getAllContainedComments().forEach(Comment::remove);
                    copy.removeComment();
                    return copy.toString().split("\\R")[0];
                }).findFirst().orElse("")).orElse("");
        if (code.isBlank()) {
            return false;
        }
        List<String> codeWords = new java.util.ArrayList<>(tokens(code));
        // Operators say a word too: i++ is "increment", i-- is "decrement".
        if (code.contains("++")) {
            codeWords.add("increment");
        }
        if (code.contains("--")) {
            codeWords.add("decrement");
        }
        long hits = words.stream().filter(w -> codeWords.stream().anyMatch(cw -> cw.equals(w) || (w.length() > 4 && cw.startsWith(w)))).count();
        boolean restates = hits * 100 >= 80L * words.size();
        if (!restates || !firstOnly) {
            return restates;
        }
        List<Comment> all = restatingComments(c);
        return !all.isEmpty() && all.get(0) == c;
    }

    private static final Pattern CODE_LIKE = Pattern.compile(
            "^\\s*(?:(?:if|for|while|return|import|def|else|try|catch|switch|case|throw|var|let|const|public|private|class)\\b.*|.*[;{}]\\s*|.*\\w\\s*=[^=].*|.*\\w+\\([^)]*\\).*)$");

    /**
     * For the first comment of a run of line comments on consecutive lines, or for a block comment: how many lines the run
     * has ({@code blockSize}), how many look like code ({@code codeLines}) and their text ({@code blockText}). Zero or empty
     * for a line comment that continues a run that started above it, so the run is reported once.
     */
    private static Object commentBlock(Comment c, String what) {
        List<String> lines = new java.util.ArrayList<>();
        if (c instanceof com.github.javaparser.ast.comments.LineComment) {
            java.util.List<Comment> all = c.findCompilationUnit().map(cu -> cu.getAllComments().stream()
                    .filter(x -> x instanceof com.github.javaparser.ast.comments.LineComment)
                    .sorted(java.util.Comparator.comparingInt(x -> x.getBegin().map(p -> p.line).orElse(0))).toList()).orElse(List.of());
            int line = c.getBegin().map(p -> p.line).orElse(0);
            if (all.stream().anyMatch(x -> x.getBegin().map(p -> p.line).orElse(-1) == line - 1)) {
                return what.equals("blockText") ? "" : 0L;
            }
            int next = line;
            for (Comment x : all) {
                if (x.getBegin().map(p -> p.line).orElse(-1) == next) {
                    lines.add(x.getContent());
                    next++;
                }
            }
        } else {
            lines.addAll(java.util.Arrays.stream(c.getContent().split("\\R")).map(s -> s.replaceFirst("^\\s*\\*", "")).toList());
        }
        lines = lines.stream().filter(s -> !s.isBlank()).toList();
        switch (what) {
            case "blockSize": return (long) lines.size();
            case "codeLines": return lines.stream().filter(s -> CODE_LIKE.matcher(s).matches()).count();
            case "codeScore": return lines.isEmpty() ? 0L : lines.stream().filter(s -> CODE_LIKE.matcher(s).matches()).count() * 100 / lines.size();
            default: return String.join("\n", lines);
        }
    }

    /** The name a string literal is stored under: the variable or field it initialises, or the target it is assigned to. */
    private static String boundName(Node n) {
        Node parent = n.getParentNode().orElse(null);
        if (parent instanceof VariableDeclarator v && v.getInitializer().orElse(null) == n) {
            return v.getNameAsString();
        }
        if (parent instanceof com.github.javaparser.ast.expr.AssignExpr a && a.getValue() == n) {
            return a.getTarget() instanceof NameExpr x ? x.getNameAsString()
                    : a.getTarget() instanceof FieldAccessExpr f ? f.getNameAsString() : null;
        }
        return null;
    }

    /**
     * The field of the enclosing class that the condition of an {@code if} reads: a name that is not a local variable or a
     * parameter of the method and that the class declares as a field, or {@code this.x}. Null when there is none.
     */
    private static FieldDeclaration guardedField(com.github.javaparser.ast.stmt.IfStmt i) {
        Optional<ClassOrInterfaceDeclaration> cls = i.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty()) {
            return null;
        }
        Optional<MethodDeclaration> method = i.findAncestor(MethodDeclaration.class);
        java.util.Set<String> locals = new java.util.HashSet<>();
        method.ifPresent(m -> {
            m.getParameters().forEach(p -> locals.add(p.getNameAsString()));
            m.findAll(VariableDeclarator.class).forEach(v -> locals.add(v.getNameAsString()));
        });
        List<String> names = new java.util.ArrayList<>();
        i.getCondition().findAll(NameExpr.class).forEach(x -> names.add(x.getNameAsString()));
        i.getCondition().findAll(FieldAccessExpr.class).stream().filter(f -> f.getScope().isThisExpr())
                .forEach(f -> names.add(f.getNameAsString()));
        for (String name : names) {
            if (locals.contains(name) && i.getCondition().findAll(FieldAccessExpr.class).stream().noneMatch(f -> f.getScope().isThisExpr() && f.getNameAsString().equals(name))) {
                continue;
            }
            for (FieldDeclaration f : cls.get().getFields()) {
                if (f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(name))) {
                    return f;
                }
            }
        }
        return null;
    }

    private static boolean inTryResource(Node n) {
        Node below = n;
        for (Optional<Node> parent = n.getParentNode(); parent.isPresent(); parent = parent.get().getParentNode()) {
            if (parent.get() instanceof com.github.javaparser.ast.stmt.TryStmt t) {
                return t.getResources().contains(below);
            }
            below = parent.get();
        }
        return false;
    }

    private static boolean isTrueMember(com.github.javaparser.ast.expr.AnnotationExpr a, String member) {
        return a instanceof com.github.javaparser.ast.expr.NormalAnnotationExpr na
                && na.getPairs().stream().anyMatch(p -> p.getNameAsString().equals(member)
                        && p.getValue() instanceof com.github.javaparser.ast.expr.BooleanLiteralExpr b && b.getValue());
    }

    /**
     * The node runs in a method whose {@code @Transactional} says {@code readOnly = true}, or in a method without its
     * own {@code @Transactional} whose class says so. A method with its own {@code @Transactional} decides for itself.
     */
    private static boolean inReadOnlyTransaction(Node n) {
        Optional<MethodDeclaration> m = n.findAncestor(MethodDeclaration.class);
        if (m.isEmpty()) {
            return false;
        }
        Optional<com.github.javaparser.ast.expr.AnnotationExpr> own = m.get().getAnnotationByName("Transactional");
        if (own.isPresent()) {
            return isTrueMember(own.get(), "readOnly");
        }
        return m.get().findAncestor(ClassOrInterfaceDeclaration.class)
                .flatMap(c -> c.getAnnotationByName("Transactional")).map(a -> isTrueMember(a, "readOnly")).orElse(false);
    }

    private static final java.util.Set<String> MUTATORS = java.util.Set.of("put", "putAll", "putIfAbsent", "add", "addAll",
            "addFirst", "addLast", "push", "pop", "poll", "offer", "remove", "removeIf", "clear", "computeIfAbsent", "merge", "set");

    /**
     * A mutating call on the field's name in a method of its class that is neither a constructor, an initializer nor a
     * {@code @PostConstruct} method, and not inside {@code synchronized} code.
     */
    private static boolean mutatedAfterInit(FieldDeclaration f) {
        String name = f.getVariable(0).getNameAsString();
        Optional<ClassOrInterfaceDeclaration> cls = f.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty()) {
            return false;
        }
        for (MethodCallExpr c : cls.get().findAll(MethodCallExpr.class)) {
            if (!MUTATORS.contains(c.getNameAsString()) || c.getScope().isEmpty() || !refersTo(c.getScope().get(), name)) {
                continue;
            }
            Optional<MethodDeclaration> m = c.findAncestor(MethodDeclaration.class);
            if (m.isPresent() && m.get().getAnnotationByName("PostConstruct").isEmpty() && !inSynchronized(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The field is assigned ({@code x = ...}, {@code this.x = ...}, {@code x++}) in a method that is not a constructor, an
     * initializer or a {@code @PostConstruct} method, outside {@code synchronized} code.
     */
    private static boolean assignedAfterInit(FieldDeclaration f) {
        String name = f.getVariable(0).getNameAsString();
        Optional<ClassOrInterfaceDeclaration> cls = f.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty()) {
            return false;
        }
        java.util.List<Node> writes = new java.util.ArrayList<>();
        cls.get().findAll(com.github.javaparser.ast.expr.AssignExpr.class).stream()
                .filter(a -> refersTo(a.getTarget(), name)).forEach(writes::add);
        cls.get().findAll(com.github.javaparser.ast.expr.UnaryExpr.class).stream()
                .filter(u -> u.getOperator().name().contains("INCREMENT") || u.getOperator().name().contains("DECREMENT"))
                .filter(u -> refersTo(u.getExpression(), name)).forEach(writes::add);
        for (Node w : writes) {
            Optional<MethodDeclaration> m = w.findAncestor(MethodDeclaration.class);
            if (m.isPresent() && m.get().getAnnotationByName("PostConstruct").isEmpty() && !inSynchronized(w)) {
                return true;
            }
        }
        return false;
    }

    private static boolean refersTo(com.github.javaparser.ast.expr.Expression e, String field) {
        return e instanceof NameExpr x && x.getNameAsString().equals(field)
                || e instanceof FieldAccessExpr a && a.getNameAsString().equals(field) && a.getScope().isThisExpr();
    }

    /** The method mentions a field of its class annotated {@code @GeneratedValue}, by name or through its getter. */
    private static boolean usesGeneratedId(MethodDeclaration m) {
        Optional<ClassOrInterfaceDeclaration> cls = m.findAncestor(ClassOrInterfaceDeclaration.class);
        if (cls.isEmpty()) {
            return false;
        }
        for (FieldDeclaration f : cls.get().getFields()) {
            if (f.getAnnotationByName("GeneratedValue").isEmpty()) {
                continue;
            }
            String field = f.getVariable(0).getNameAsString();
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            if (m.findAll(NameExpr.class).stream().anyMatch(x -> x.getNameAsString().equals(field))
                    || m.findAll(FieldAccessExpr.class).stream().anyMatch(x -> x.getNameAsString().equals(field))
                    || m.findAll(MethodCallExpr.class).stream().anyMatch(x -> x.getNameAsString().equals(getter))) {
                return true;
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
