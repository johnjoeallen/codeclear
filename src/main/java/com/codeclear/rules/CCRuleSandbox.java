package com.codeclear.rules;

import groovy.transform.ThreadInterrupt;
import java.util.List;
import java.util.Set;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.MethodReferenceExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

/**
 * Compile-time restrictions for generated rule scripts.
 *
 * <p><b>This is not a security boundary.</b> Groovy is dynamic and the JVM has no in-process sandbox since the
 * Security Manager was removed, so these checks are a best-effort screen against a generator mistake or a
 * prompt-injected script, not protection against a determined attacker. Layers:
 * <ol>
 *   <li>Imports, declared types, constructors, casts and class references must be on an allowlist
 *       (the JavaParser AST, basic collections, regex, the script facade). No IO, network, process,
 *       reflection or threading classes.</li>
 *   <li>Method and property names that reach the runtime, classloaders, reflection or the metaclass are denied,
 *       and so are dynamic (computed) method and property names, which would bypass a name list.</li>
 *   <li>No annotations (so no AST transformations such as {@code @ASTTest} that run code at compile time),
 *       no static imports, no extra classes.</li>
 *   <li>Loops and methods check the thread's interrupt flag so the detector's timeout can stop a runaway script.</li>
 * </ol>
 * Known gap: scripts can still call any method of an allowed object, including JavaParser setters that mutate the
 * shared tree.
 */
final class CCRuleSandbox {

    private static final Set<String> ALLOWED_CLASSES = Set.of(
            "java.lang.Object", "java.lang.String", "java.lang.CharSequence", "java.lang.Integer", "java.lang.Long",
            "java.lang.Double", "java.lang.Float", "java.lang.Short", "java.lang.Byte", "java.lang.Boolean",
            "java.lang.Character", "java.lang.Number", "java.lang.Math", "java.lang.StringBuilder",
            "java.lang.Iterable", "java.lang.Comparable", "java.lang.Void",
            "java.lang.Exception", "java.lang.RuntimeException", "java.lang.IllegalArgumentException",
            "java.lang.IllegalStateException", "java.lang.NumberFormatException",
            "java.util.NoSuchElementException",
            "java.util.List", "java.util.ArrayList", "java.util.LinkedList", "java.util.Map", "java.util.HashMap",
            "java.util.LinkedHashMap", "java.util.TreeMap", "java.util.Map$Entry", "java.util.Set",
            "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet", "java.util.SortedMap",
            "java.util.SortedSet", "java.util.Collection", "java.util.Collections", "java.util.Arrays",
            "java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble",
            "java.util.Objects", "java.util.Iterator", "java.util.Deque", "java.util.ArrayDeque",
            "java.util.Queue", "java.util.Comparator",
            "java.util.regex.Pattern", "java.util.regex.Matcher",
            "java.math.BigDecimal", "java.math.BigInteger",
            "groovy.lang.Closure", "groovy.lang.GString", "groovy.lang.IntRange", "groovy.lang.Range",
            "com.github.javaparser.Position", "com.github.javaparser.Range",
            "com.codeclear.rules.CCCodeModel", "com.codeclear.rules.CCFileInfo", "com.codeclear.rules.CCChangedLines",
            "com.codeclear.rules.CCRuleScriptBase$Finding", "com.codeclear.rules.CCRuleScriptBase$Candidate");

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "java.util.function.", "java.util.stream.", "com.github.javaparser.ast.");

    private static final Set<String> DENIED_METHODS = Set.of(
            // reflection and class loading
            "getClass", "forName", "newInstance", "loadClass", "getClassLoader", "getDeclaredMethods",
            "getDeclaredFields", "getDeclaredConstructors", "getMethods", "getFields", "invoke",
            // dynamic dispatch, metaprogramming, evaluation
            "invokeMethod", "getMetaClass", "setMetaClass", "getProperty", "setProperty", "getProperties",
            "evaluate", "execute", "run", "parse", "getBinding", "setBinding", "with", "tap",
            // process, runtime, files, urls
            "exec", "getRuntime", "exit", "halt", "load", "toURL", "toURI", "toFile", "newReader", "newWriter",
            // output and thread control
            "print", "println", "printf", "sleep", "wait", "notify", "notifyAll",
            // JavaParser: tree mutation and file storage
            "replace", "removeForced", "setParentNode", "unsetParentNode", "setStorage", "getStorage");

    private static final Set<String> DENIED_PROPERTIES = Set.of(
            "class", "metaClass", "properties", "binding", "owner", "delegate", "thisObject", "classLoader",
            "declaredMethods", "declaredFields", "declaredConstructors", "methods", "fields", "constructors",
            "metaMethods", "storage");

    private static final Set<String> DENIED_VARIABLES = Set.of("metaClass", "binding", "owner", "delegate");

    private CCRuleSandbox() { }

    static CompilerConfiguration configuration() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(CCRuleScriptBase.class.getName());
        config.setSourceEncoding("UTF-8");
        config.setDebug(false);
        // @Grab is a global transformation that would run at CONVERSION, before the annotation screen below.
        config.setDisabledGlobalASTTransformations(Set.of("groovy.grape.GrabAnnotationTransformation"));

        // Phase order matters: the annotation screen runs at CONVERSION, before any annotation-driven transformation
        // can execute; the secure customizer is added before ThreadInterrupt so the Thread calls that the
        // interrupt transform injects are not themselves rejected.
        config.addCompilationCustomizers(new AnnotationScreen());
        config.addCompilationCustomizers(secureCustomizer());
        config.addCompilationCustomizers(new ImportAndDeclarationScreen());
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
        return config;
    }

    private static SecureASTCustomizer secureCustomizer() {
        SecureASTCustomizer secure = new SecureASTCustomizer();
        secure.setClosuresAllowed(true);
        secure.setMethodDefinitionAllowed(true);
        secure.setPackageAllowed(false);
        secure.addExpressionCheckers(CCRuleSandbox::checkExpression);
        secure.addStatementCheckers(statement -> {
            if (statement instanceof org.codehaus.groovy.ast.stmt.TryCatchStatement tc) {
                for (CatchStatement c : tc.getCatchStatements()) {
                    requireAllowed(c.getVariable().getType(), "catch parameter");
                }
            }
            return true;
        });
        return secure;
    }

    private static boolean checkExpression(Expression e) {
        if (e instanceof ClassExpression c) {
            requireAllowed(c.getType(), "class reference");
        } else if (e instanceof ConstructorCallExpression c) {
            requireAllowed(c.getType(), "constructor");
        } else if (e instanceof CastExpression c) {
            requireAllowed(c.getType(), "cast");
        } else if (e instanceof DeclarationExpression d) {
            requireAllowed(d.getVariableExpression() != null ? d.getVariableExpression().getType() : ClassHelper.OBJECT_TYPE,
                    "variable declaration");
        } else if (e instanceof ClosureExpression c) {
            if (c.getParameters() != null) {
                for (Parameter p : c.getParameters()) {
                    requireAllowed(p.getType(), "closure parameter");
                }
            }
        } else if (e instanceof StaticMethodCallExpression s) {
            throw denied("static method call '" + s.getMethod() + "' (static imports are not allowed)");
        } else if (e instanceof AttributeExpression) {
            throw denied("direct field access (.@)");
        } else if (e instanceof MethodPointerExpression p) {
            requireConstantName(p.getMethodName(), DENIED_METHODS, "method pointer");
        } else if (e instanceof MethodReferenceExpression p) {
            requireConstantName(p.getMethodName(), DENIED_METHODS, "method reference");
        } else if (e instanceof MethodCallExpression m) {
            requireConstantName(m.getMethod(), DENIED_METHODS, "method call");
            // remove() with no arguments detaches a JavaParser node; remove(x) on a collection is fine.
            if ("remove".equals(m.getMethodAsString())
                    && m.getArguments() instanceof ArgumentListExpression args && args.getExpressions().isEmpty()) {
                throw denied("method call 'remove()'");
            }
        } else if (e instanceof PropertyExpression p) {
            requireConstantName(p.getProperty(), DENIED_PROPERTIES, "property");
        } else if (e instanceof VariableExpression v && DENIED_VARIABLES.contains(v.getName())) {
            throw denied("variable '" + v.getName() + "'");
        }
        return true;
    }

    private static void requireConstantName(Expression name, Set<String> denied, String what) {
        if (!(name instanceof ConstantExpression c) || !(c.getValue() instanceof String s)) {
            throw denied("computed " + what + " name; use a literal name");
        }
        if (denied.contains(s)) {
            throw denied(what + " '" + s + "'");
        }
    }

    private static void requireAllowed(ClassNode type, String what) {
        if (type == null) {
            return;
        }
        ClassNode t = type;
        while (t.isArray()) {
            t = t.getComponentType();
        }
        if (ClassHelper.isPrimitiveType(t) || !isAllowedClass(t.getName())) {
            if (ClassHelper.isPrimitiveType(t)) {
                return;
            }
            throw denied(what + " of type " + t.getName());
        }
    }

    static boolean isAllowedClass(String name) {
        if (ALLOWED_CLASSES.contains(name)) {
            return true;
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Star imports: only packages whose classes are all allowed, plus java.util and java.util.regex. */
    private static boolean isAllowedPackage(String pkg) {
        return pkg.equals("java.util.") || pkg.equals("java.util.regex.") || ALLOWED_PREFIXES.stream().anyMatch(pkg::startsWith);
    }

    private static SecurityException denied(String what) {
        return new SecurityException("not allowed in a rule script: " + what);
    }

    /** Rejects every annotation. Runs at CONVERSION, before annotation-driven transformations execute. */
    private static final class AnnotationScreen extends CompilationCustomizer {

        AnnotationScreen() {
            super(CompilePhase.CONVERSION);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            new ClassCodeVisitorSupport() {
                @Override
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                @Override
                public void visitAnnotations(org.codehaus.groovy.ast.AnnotatedNode node) {
                    rejectWrittenAnnotations(node);
                }

                @Override
                public void visitDeclarationExpression(DeclarationExpression expression) {
                    rejectWrittenAnnotations(expression);
                    super.visitDeclarationExpression(expression);
                }
            }.visitClass(classNode);
        }
    }

    /**
     * Rejects annotations that appear in the script text. The compiler adds its own (for the script base class);
     * those have no source position and are allowed.
     */
    private static void rejectWrittenAnnotations(org.codehaus.groovy.ast.AnnotatedNode node) {
        for (org.codehaus.groovy.ast.AnnotationNode a : node.getAnnotations()) {
            if (a.getLineNumber() > 0) {
                throw denied("annotation @" + a.getClassNode().getNameWithoutPackage());
            }
        }
    }

    /** Imports, extra classes and method signatures. */
    private static final class ImportAndDeclarationScreen extends CompilationCustomizer {

        ImportAndDeclarationScreen() {
            super(CompilePhase.CANONICALIZATION);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            ModuleNode module = source.getAST();
            if (module.getClasses().size() > 1) {
                throw denied("class, interface or anonymous class definitions; only methods and closures are allowed");
            }
            for (ImportNode i : module.getImports()) {
                requireAllowed(i.getType(), "import");
            }
            for (ImportNode i : module.getStarImports()) {
                String pkg = i.getPackageName();
                if (!isAllowedPackage(pkg)) {
                    throw denied("star import of " + pkg);
                }
            }
            if (!module.getStaticImports().isEmpty() || !module.getStaticStarImports().isEmpty()) {
                throw denied("static imports");
            }
            for (MethodNode method : classNode.getMethods()) {
                if (method.isSynthetic() || method.getName().equals("run") || method.getName().equals("main")) {
                    continue;
                }
                requireAllowed(method.getReturnType(), "method return type");
                for (Parameter p : method.getParameters()) {
                    requireAllowed(p.getType(), "method parameter");
                }
            }
        }
    }

    /** Unwraps the compile error to the sandbox or syntax message a rule author can act on. */
    static String reason(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && !(t instanceof SecurityException)) {
            t = t.getCause();
        }
        String message = t.getMessage() == null ? t.toString() : t.getMessage();
        return t instanceof CompilationFailedException ? message.lines().limit(3).reduce((a, b) -> a + " | " + b).orElse(message)
                : message;
    }
}
