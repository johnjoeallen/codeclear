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
                case "isTest" -> f.getPath().matches("(^|.*/)src/test/.*");
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

    /** Every field name the schema defines for some kind. A known name on the wrong kind is missing, not an error. */
    private static final java.util.Set<String> KNOWN = java.util.Set.of("path", "language", "isTest", "text", "param",
            "body", "name", "type", "statements", "hasComment", "expression", "nameNode", "args", "scope",
            "declaredType", "inTransaction", "lazyAssociation", "dereferenced", "fetched", "inController",
            "returnedEntity", "lazyAssociations", "returnsFetched", "annotations", "firstArg", "calls", "throws",
            "enclosingCatch", "enclosingMethodName", "operator", "left", "right", "typeName", "causeUnsupported",
            "initializerTypeName", "isStatic", "isFinal", "isPrivate", "classAnnotations", "usedOnlyInSynchronized",
            "isIdentitySentinel", "escapesFinally", "assignments", "callsAfter");

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
                }
            }
            child = p;
        }
    }
}
