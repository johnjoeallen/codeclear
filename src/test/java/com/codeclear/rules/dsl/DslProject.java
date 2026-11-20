package com.codeclear.rules.dsl;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The project-wide model a cross-file rule needs, built once per review: every parsed file, the JPA entity index,
 * a method index and the parsed configuration. In the real engine this is the {@code CodeModel} behind symbol
 * resolution over the project classpath; here names are matched textually, which is enough to find where the
 * language needs facts and no more.
 */
final class DslProject {

    private final Map<String, String> config;
    /** Entity simple name to (association field name to whether it is lazy). */
    private final Map<String, Map<String, Boolean>> entities = new HashMap<>();
    /** Entity simple name to the fields hidden from serialisation with {@code @JsonIgnore}. */
    private final Map<String, java.util.Set<String>> jsonIgnored = new HashMap<>();
    private final Map<String, List<MethodDeclaration>> methodsByName = new HashMap<>();
    /** Call graph: method to the call expressions that may invoke it (matched by name and arity). */
    private final Map<MethodDeclaration, List<MethodCallExpr>> callers = new java.util.IdentityHashMap<>();

    DslProject(List<CompilationUnit> units, Map<String, String> config) {
        this.config = config;
        for (CompilationUnit cu : units) {
            for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (has(c, "Entity")) {
                    indexEntity(c);
                }
                for (MethodDeclaration m : c.getMethods()) {
                    methodsByName.computeIfAbsent(m.getNameAsString(), k -> new ArrayList<>()).add(m);
                }
            }
        }
        for (CompilationUnit cu : units) {
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                for (MethodDeclaration target : targets(call)) {
                    callers.computeIfAbsent(target, k -> new ArrayList<>()).add(call);
                }
            }
        }
    }

    /** The project methods a call may invoke: same name and parameter count. */
    List<MethodDeclaration> targets(MethodCallExpr call) {
        return methods(call.getNameAsString()).stream()
                .filter(m -> m.getParameters().size() == call.getArguments().size()).toList();
    }

    List<MethodCallExpr> callsTo(MethodDeclaration m) {
        return callers.getOrDefault(m, List.of());
    }

    private void indexEntity(ClassOrInterfaceDeclaration c) {
        Map<String, Boolean> assoc = new HashMap<>();
        java.util.Set<String> ignored = new java.util.HashSet<>();
        for (FieldDeclaration f : c.getFields()) {
            for (var v : f.getVariables()) {
                if (has(f, "JsonIgnore")) {
                    ignored.add(v.getNameAsString());
                }
                Boolean lazy = lazyOf(f);
                if (lazy != null) {
                    assoc.put(v.getNameAsString(), lazy);
                }
            }
        }
        entities.put(c.getNameAsString(), assoc);
        jsonIgnored.put(c.getNameAsString(), ignored);
    }

    /** null when the field is not an association; otherwise whether it is fetched lazily. */
    private static Boolean lazyOf(FieldDeclaration f) {
        for (AnnotationExpr a : f.getAnnotations()) {
            String n = a.getNameAsString();
            boolean toMany = n.equals("OneToMany") || n.equals("ManyToMany");
            boolean toOne = n.equals("ManyToOne") || n.equals("OneToOne");
            if (!toMany && !toOne) {
                continue;
            }
            String fetch = "";
            if (a instanceof NormalAnnotationExpr na) {
                fetch = na.getPairs().stream().filter(p -> p.getNameAsString().equals("fetch"))
                        .map(p -> p.getValue().toString()).findFirst().orElse("");
            }
            return toMany ? !fetch.contains("EAGER") : fetch.contains("LAZY");
        }
        return null;
    }

    static boolean has(NodeWithAnnotations<?> n, String annotation) {
        return n.getAnnotationByName(annotation).isPresent();
    }

    String config(String key) {
        return config.get(key);
    }

    boolean isEntity(String simpleName) {
        return simpleName != null && entities.containsKey(simpleName);
    }

    /** True when {@code entity} has an association field {@code assoc} fetched lazily. */
    boolean isLazy(String entity, String assoc) {
        Map<String, Boolean> m = entities.get(entity);
        return m != null && Boolean.TRUE.equals(m.get(assoc));
    }

    /** The lazy association fields of an entity that are not hidden with {@code @JsonIgnore}. */
    List<String> serialisedLazy(String entity) {
        Map<String, Boolean> m = entities.getOrDefault(entity, Map.of());
        return m.entrySet().stream()
                .filter(e -> e.getValue() && !jsonIgnored.getOrDefault(entity, java.util.Set.of()).contains(e.getKey()))
                .map(Map.Entry::getKey).sorted().toList();
    }

    List<MethodDeclaration> methods(String name) {
        return methodsByName.getOrDefault(name, List.of());
    }
}
