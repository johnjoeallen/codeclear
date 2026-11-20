package com.codeclear.rules.dsl;

import com.codeclear.rules.CCCodeModel;
import com.codeclear.rules.CCFileInfo;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.List;
import java.util.Optional;

/**
 * The host side of the language: the schema of readable fields, and the "facts" that are too much computation for a
 * rule to express itself. Everything a rule can see is listed here; anything else is an error.
 */
final class DslFacts {

    private DslFacts() { }

    static Object field(Object t, String name, CCCodeModel model, DslProject project) {
        if (t == null) {
            return null;
        }
        if (t instanceof CCFileInfo f) {
            return switch (name) {
                case "path" -> f.getPath();
                case "language" -> f.getLanguage();
                case "isTest" -> f.getPath().matches("(^|.*/)src/test/.*")
                        || f.getPath().matches("(^|.*/)(tests?|__tests__)/.*|(^|.*/)(test_[^/]*|[^/]*_test|conftest)\\.py|.*\\.(test|spec)\\.[cm]?[jt]sx?");
                default -> unknown(t, name);
            };
        }
        if (t instanceof com.codeclear.rules.CCSqlModel.Node sqlNode) {
            return sqlField(sqlNode, name, t);
        }
        if (t instanceof com.codeclear.rules.CCTsNode n) {
            return switch (name) {
                case "type" -> n.type();
                case "text" -> n.text();
                case "line" -> (long) n.line();
                case "children" -> n.children();
                case "args" -> n.args();
                case "following" -> n.following();
                case "positional" -> n.positional();
                case "parent" -> n.parent();
                case "next" -> n.next();
                case "previous" -> n.previous();
                case "hasNearbyComment" -> n.hasNearbyComment();
                case "lastChild" -> n.lastChild();
                case "tokens" -> n.tokens();
                case "endLine" -> (long) n.endLine();
                case "fieldName" -> n.fieldName();
                case "isError" -> n.isError();
                default -> unknown(t, name);
            };
        }
        if (t instanceof com.codeclear.rules.CCConfigModel.Setting s) {
            return switch (name) {
                case "key" -> s.key();
                case "rawKey" -> s.rawKey();
                case "value" -> s.value();
                case "profile" -> s.profile();
                default -> unknown(t, name);
            };
        }
        if (!(t instanceof Node n)) {
            return unknown(t, name);
        }
        if (name.equals("text")) {
            return n.toString();
        }
        Object jpa = DslJpaFacts.field(n, name, project);
        if (jpa != DslJpaFacts.NOT_HANDLED) {
            return jpa;
        }
        Object syntax = DslSyntaxFacts.field(n, name, model);
        if (syntax != DslSyntaxFacts.NOT_HANDLED) {
            return syntax;
        }
        if (n instanceof CatchClause c) {
            switch (name) {
                case "param": return c.getParameter();
                case "body": return c.getBody();
                default: break;
            }
        } else if (n instanceof Parameter p) {
            switch (name) {
                case "name": return p.getNameAsString();
                case "type": return p.getType().toString();
                case "annotations": return p.getAnnotations().stream().map(a -> a.getNameAsString()).toList();
                default: break;
            }
        } else if (n instanceof BlockStmt b) {
            switch (name) {
                case "statements": return List.copyOf(b.getStatements());
                case "hasComment": return !b.getAllContainedComments().isEmpty();
                default: break;
            }
        } else if (n instanceof ExpressionStmt x) {
            if (name.equals("expression")) {
                return x.getExpression();
            }
        } else if (n instanceof MethodCallExpr c) {
            switch (name) {
                case "name": return c.getNameAsString();
                case "nameNode": return c.getName();
                case "args": return List.copyOf(c.getArguments());
                case "firstArg": return c.getArguments().isEmpty() ? null : c.getArgument(0);
                case "scope": return c.getScope().orElse(null);
                default: break;
            }
        } else if (n instanceof NameExpr x) {
            switch (name) {
                case "name": return x.getNameAsString();
                case "declaredType": return declaredType(x);
                default: break;
            }
        } else if (n instanceof FieldAccessExpr x) {
            switch (name) {
                case "name": return x.getNameAsString();
                case "scope": return x.getScope();
                default: break;
            }
        }
        if (name.equals("type") && n instanceof Expression e) {
            return model.typeOf(e);
        }
        return unknown(t, name);
    }

    /** The fields of a SQL statement, column or line. A name that belongs to another kind is missing, not an error. */
    private static Object sqlField(com.codeclear.rules.CCSqlModel.Node n, String name, Object t) {
        return switch (n) {
            case com.codeclear.rules.CCSqlModel.Statement s -> switch (name) {
                case "text" -> s.text();
                case "code" -> s.code();
                case "precedingComment" -> s.precedingComment();
                case "hasTopLevelWhere" -> s.hasTopLevelWhere();
                case "tableCreatedInFile" -> s.tableCreatedInFile();
                default -> unknown(t, name);
            };
            case com.codeclear.rules.CCSqlModel.Column c -> switch (name) {
                case "text" -> c.text();
                case "code" -> c.code();
                case "name" -> c.name();
                case "tableCreatedInFile" -> c.tableCreatedInFile();
                default -> unknown(t, name);
            };
            case com.codeclear.rules.CCSqlModel.Line l -> switch (name) {
                case "text" -> l.text();
                case "code" -> l.code();
                case "statementCode" -> l.statementCode();
                case "executesBuiltString" -> l.executesBuiltString();
                default -> unknown(t, name);
            };
        };
    }

    /** Every field name the schema defines for some kind. A known name on the wrong kind is missing, not an error. */
    private static final java.util.Set<String> KNOWN = java.util.Set.of("path", "language", "isTest", "text", "param",
            "body", "name", "type", "statements", "hasComment", "expression", "nameNode", "args", "scope",
            "declaredType", "inTransaction", "lazyAssociation", "dereferenced", "fetched", "inController",
            "returnedEntity", "lazyAssociations", "returnsFetched", "annotations", "firstArg", "calls", "throws",
            "enclosingCatch", "enclosingMethodName", "operator", "left", "right", "typeName", "causeUnsupported",
            "initializerTypeName", "isStatic", "isFinal", "isPrivate", "classAnnotations", "usedOnlyInSynchronized",
            "isIdentitySentinel", "escapesFinally", "assignments", "callsAfter", "doubleCheckedField", "isVolatile", "enclosingClassMethods", "placeholders", "logArguments", "isThrowable",
            "isPublic", "classIsPublic", "throwsTypes", "tryCalls", "enclosingMethod", "enclosingIterations", "iteratedCollection",
            "iteratedType", "followedByExit", "iterable", "updatesCurrentKey", "enclosingClassAnnotations", "isDiscarded", "sameClassTransactionalTarget", "assertions",
            "inDataClass", "chainCalls", "chainStartsStream", "hasSideEffectStage", "holderInitializer", "isLocal", "usedInArithmetic",
            "neverReferenced", "inPublicMethod", "marker", "markerText", "usesGeneratedId", "assignedToLocal", "inTryResource",
            "inReadOnlyTransaction", "mutatedAfterInit", "assignedAfterInit", "inLevelGuard", "catches", "hasInitializer", "inSynchronized", "closedOrHandedOn", "boundName",
            "code", "precedingComment", "hasTopLevelWhere", "tableCreatedInFile", "statementCode", "executesBuiltString",
            "key", "rawKey", "value", "profile", "children", "parent", "next", "fieldName", "isError", "line", "previous", "lastChild", "tokens", "endLine", "hasNearbyComment", "positional", "following", "enclosingCallName", "enclosingCalls", "returnType", "identifiers", "enclosingLoop", "fieldDeclText", "guardedField", "enclosingStatement", "enclosingClass");

    private static Object unknown(Object t, String name) {
        if (KNOWN.contains(name)) {
            return null;
        }
        throw new IllegalArgumentException("no field '" + name + "' anywhere in the schema (typo?)");
    }

    static Object fact(Object target, String name, List<Object> args, DslProject project,
                       java.util.function.BiPredicate<DslInterpreter.Pred, Object> apply) {
        if (name.equals("tainted") && target instanceof Expression e && args.size() == 2
                && args.get(0) instanceof DslInterpreter.Pred src && args.get(1) instanceof DslInterpreter.Pred san
                && project != null) {
            return new DslTaint(src, san, apply, project).tainted(e);
        }
        if (target instanceof com.codeclear.rules.CCTsNode n) {
            if (name.equals("field") && args.size() == 1 && args.get(0) instanceof String f) {
                return n.field(f);
            }
            if (name.equals("childAt") && args.size() == 1 && args.get(0) instanceof Number i) {
                return n.childAt(i.intValue());
            }
            if (name.equals("arg") && args.size() == 1 && args.get(0) instanceof Number i) {
                List<com.codeclear.rules.CCTsNode> p = n.positional(); // the i-th positional argument, or missing
                return i.intValue() < p.size() ? p.get(i.intValue()) : null;
            }
            if (name.equals("keyword") && args.size() == 1 && args.get(0) instanceof String kw) {
                return n.keyword(kw);
            }
            if (name.equals("child") && args.size() == 1 && args.get(0) instanceof String ty) {
                return n.child(ty);
            }
            if (name.equals("declaredAsNewIn") && args.size() == 1 && args.get(0) instanceof com.codeclear.rules.CCTsNode scope) {
                return n.declaredAsNewIn(scope);
            }
            if (name.equals("countOf") && args.size() == 1 && args.get(0) instanceof String id) {
                return n.countOf(id);
            }
            if (name.equals("mentions") && args.size() == 1 && args.get(0) instanceof String id) {
                return n.mentions(id);
            }
            if (name.equals("descendants") && args.size() == 1 && args.get(0) instanceof String ty) {
                return n.descendants(ty);
            }
            if (name.equals("ancestors") && args.size() == 1 && args.get(0) instanceof String ty) {
                return n.ancestors(ty);
            }
            if (name.equals("ancestor") && args.size() == 1 && args.get(0) instanceof String ty) {
                return n.ancestor(ty);
            }
            if (name.equals("ancestorOf") && args.size() == 1 && args.get(0) instanceof List<?> types) {
                return n.ancestorOf(types);
            }
        }
        if (name.equals("targetAnnotations") && target instanceof MethodCallExpr call && project != null) {
            // The annotations, as source, of the methods the call may reach in the project (a few at most).
            return project.targets(call).stream().flatMap(m -> m.getAnnotations().stream()).map(Object::toString).toList();
        }
        if (name.equals("targetReturnTypes") && target instanceof MethodCallExpr call && project != null) {
            return project.targets(call).stream().map(m -> m.getType().toString()).toList();
        }
        if (name.equals("mentions") && target instanceof DslProject p && args.size() == 1 && args.get(0) instanceof String id) {
            return p.mentions(id); // some file of the project uses this name as a type or identifier
        }
        if (name.equals("siblingValue") && target instanceof com.codeclear.rules.CCConfigModel.Setting s && args.size() == 1
                && args.get(0) instanceof String last) {
            // The setting beside this one: same parent key, the given last segment. Missing when there is none.
            int dot = s.key().lastIndexOf('.');
            return s.doc().get(dot < 0 ? last : s.key().substring(0, dot + 1) + last);
        }
        Object syntax = DslSyntaxFacts.fact(target, name, args);
        if (syntax != DslSyntaxFacts.NOT_HANDLED) {
            return syntax;
        }
        if (name.equals("config") && target instanceof DslProject p && args.size() == 1 && args.get(0) instanceof String k) {
            return p.config(k);
        }
        if (name.equals("guarded") && target instanceof MethodCallExpr call && args.size() == 2
                && args.get(0) instanceof Node v && args.get(1) instanceof DslInterpreter.Atom a
                && a.name().equals("present")) {
            return isGuarded(call, v.toString());
        }
        if (name.equals("guardedByCallers") && target instanceof MethodCallExpr call && args.size() == 2
                && args.get(0) instanceof Node v && args.get(1) instanceof DslInterpreter.Atom a
                && a.name().equals("present")) {
            return project != null && v instanceof NameExpr n
                    && paramGuarded(call, n.getNameAsString(), project, 0, java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
        }
        throw new IllegalArgumentException("no fact '" + name + "' for those arguments");
    }

    // ---- declaredType: type text of a simple name, from the enclosing method then the class fields -------------

    private static String declaredType(NameExpr n) {
        String name = n.getNameAsString();
        Optional<CallableDeclaration> method = n.findAncestor(CallableDeclaration.class);
        if (method.isPresent()) {
            for (Parameter p : method.get().findAll(Parameter.class)) {
                if (p.getNameAsString().equals(name)) {
                    return p.getType().toString();
                }
            }
            for (VariableDeclarator d : method.get().findAll(VariableDeclarator.class)) {
                if (d.getNameAsString().equals(name)) {
                    return d.getType().toString();
                }
            }
        }
        Optional<CompilationUnit> cu = n.findCompilationUnit();
        if (cu.isPresent()) {
            for (FieldDeclaration f : cu.get().findAll(FieldDeclaration.class)) {
                for (VariableDeclarator d : f.getVariables()) {
                    if (d.getNameAsString().equals(name)) {
                        return d.getType().toString();
                    }
                }
            }
        }
        return null;
    }

    // ---- guarded(v, present): a dominating condition proves the Optional named v is present ---------------------

    private static boolean isCallOn(Expression e, String name, String v) {
        return e instanceof MethodCallExpr m && m.getNameAsString().equals(name) && m.getArguments().isEmpty()
                && m.getScope().map(s -> s.toString().equals(v)).orElse(false);
    }

    private static boolean presentWhenTrue(Expression c, String v) {
        if (c instanceof EnclosedExpr e) {
            return presentWhenTrue(e.getInner(), v);
        }
        if (isCallOn(c, "isPresent", v)) {
            return true;
        }
        if (c instanceof UnaryExpr u && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            return presentWhenFalse(u.getExpression(), v);
        }
        if (c instanceof BinaryExpr b && b.getOperator() == BinaryExpr.Operator.AND) {
            return presentWhenTrue(b.getLeft(), v) || presentWhenTrue(b.getRight(), v);
        }
        return false;
    }

    private static boolean presentWhenFalse(Expression c, String v) {
        if (c instanceof EnclosedExpr e) {
            return presentWhenFalse(e.getInner(), v);
        }
        if (isCallOn(c, "isEmpty", v)) {
            return true;
        }
        if (c instanceof UnaryExpr u && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            return presentWhenTrue(u.getExpression(), v);
        }
        if (c instanceof BinaryExpr b && b.getOperator() == BinaryExpr.Operator.OR) {
            return presentWhenFalse(b.getLeft(), v) || presentWhenFalse(b.getRight(), v);
        }
        return false;
    }

    private static boolean completesAbruptly(Statement s) {
        if (s instanceof ReturnStmt || s instanceof ThrowStmt || s instanceof ContinueStmt || s instanceof BreakStmt) {
            return true;
        }
        if (s instanceof BlockStmt b) {
            return !b.getStatements().isEmpty() && completesAbruptly(b.getStatements().getLast().get());
        }
        return false;
    }

    /** {@code assertThat(v).isPresent()}, also when more assertions are chained after it. */
    private static boolean assertsPresent(Expression e, String v) {
        boolean present = false;
        for (Expression x = e; x instanceof MethodCallExpr c; x = c.getScope().orElse(null)) {
            if (c.getNameAsString().equals("assertThat") && c.getArguments().size() == 1) {
                return present && c.getArgument(0).toString().equals(v);
            }
            present |= c.getNameAsString().equals("isPresent") || c.getNameAsString().equals("isNotEmpty")
                    && c.getArguments().isEmpty();
        }
        return false;
    }

    private static boolean isGuarded(MethodCallExpr call, String v) {
        Node child = call;
        while (true) {
            Optional<Node> parent = child.getParentNode();
            if (parent.isEmpty()) {
                return false;
            }
            Node p = parent.get();
            if (p instanceof MethodDeclaration || p instanceof ConstructorDeclaration
                    || p instanceof InitializerDeclaration) {
                return false;
            }
            if (p instanceof IfStmt i) {
                if (child == i.getThenStmt() && presentWhenTrue(i.getCondition(), v)) {
                    return true;
                }
                if (i.getElseStmt().isPresent() && child == i.getElseStmt().get()
                        && presentWhenFalse(i.getCondition(), v)) {
                    return true;
                }
            } else if (p instanceof WhileStmt w) {
                if (child == w.getBody() && presentWhenTrue(w.getCondition(), v)) {
                    return true;
                }
            } else if (p instanceof ConditionalExpr t) {
                if (child == t.getThenExpr() && presentWhenTrue(t.getCondition(), v)) {
                    return true;
                }
                if (child == t.getElseExpr() && presentWhenFalse(t.getCondition(), v)) {
                    return true;
                }
            } else if (p instanceof BinaryExpr b) {
                if (child == b.getRight() && b.getOperator() == BinaryExpr.Operator.AND
                        && presentWhenTrue(b.getLeft(), v)) {
                    return true;
                }
                if (child == b.getRight() && b.getOperator() == BinaryExpr.Operator.OR
                        && presentWhenFalse(b.getLeft(), v)) {
                    return true;
                }
            } else if (p instanceof MethodCallExpr m) {
                if ((m.getNameAsString().equals("ifPresent") || m.getNameAsString().equals("ifPresentOrElse"))
                        && m.getScope().map(s -> s.toString().equals(v)).orElse(false)
                        && !m.getArguments().isEmpty() && child == m.getArgument(0)) {
                    return true;
                }
            } else if (p instanceof BlockStmt b) {
                for (Statement s : b.getStatements()) {
                    if (s == child) {
                        break;
                    }
                    if (s instanceof IfStmt i && i.getElseStmt().isEmpty() && completesAbruptly(i.getThenStmt())
                            && presentWhenFalse(i.getCondition(), v)) {
                        return true;
                    }
                    if (s instanceof AssertStmt a && presentWhenTrue(a.getCheck(), v)) {
                        return true;
                    }
                    if (s instanceof ExpressionStmt e && assertsPresent(e.getExpression(), v)) {
                        return true;
                    }
                }
            }
            child = p;
        }
    }

    // ---- guardedByCallers(v, present): v is a parameter and every caller passes something present -----------------

    /**
     * The Optional is a parameter of the method around {@code use}, the method has callers in the project, and every
     * caller passes {@code Optional.of(...)}, or a variable that is guarded at the call or is itself a parameter that
     * every one of its callers guards. Followed for a few hops; a method nobody in the project calls proves nothing.
     */
    private static boolean paramGuarded(Node use, String v, DslProject project, int hops, java.util.Set<Parameter> visiting) {
        Optional<MethodDeclaration> m = use.findAncestor(MethodDeclaration.class);
        if (m.isEmpty() || hops >= DslTaint.MAX_HOPS) {
            return false;
        }
        Parameter p = m.get().getParameters().stream().filter(x -> x.getNameAsString().equals(v)).findFirst().orElse(null);
        if (p == null || !visiting.add(p)) {
            return false;
        }
        try {
            int index = m.get().getParameters().indexOf(p);
            List<MethodCallExpr> calls = project.callsTo(m.get());
            if (calls.isEmpty()) {
                return false;
            }
            for (MethodCallExpr call : calls) {
                if (index >= call.getArguments().size()) {
                    return false;
                }
                Expression arg = call.getArgument(index);
                boolean present = arg instanceof MethodCallExpr o && o.getNameAsString().equals("of")
                        && o.getScope().map(x -> x.toString().equals("Optional")).orElse(false)
                        || arg instanceof NameExpr n && (isGuarded(call, n.getNameAsString())
                                || paramGuarded(call, n.getNameAsString(), project, hops + 1, visiting));
                if (!present) {
                    return false;
                }
            }
            return true;
        } finally {
            visiting.remove(p);
        }
    }
}
