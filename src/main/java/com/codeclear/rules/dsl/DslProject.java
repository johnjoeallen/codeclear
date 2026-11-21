package com.codeclear.rules.dsl;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
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
    /** Simple class name to its declaration (the first one, if names collide across packages). */
    private final Map<String, ClassOrInterfaceDeclaration> classes = new HashMap<>();
    /** Class name to its direct supertypes, and the inverse. */
    private final Map<String, List<String>> supers = new HashMap<>();
    private final Map<String, List<String>> subs = new HashMap<>();
    private final Map<MethodDeclaration, String> owner = new java.util.IdentityHashMap<>();
    /** Call graph: method to the call expressions that may invoke it (matched by name and arity). */
    private final List<CompilationUnit> units;
    private final Map<MethodDeclaration, List<MethodCallExpr>> callers = new java.util.IdentityHashMap<>();

    DslProject(List<CompilationUnit> units, Map<String, String> config) {
        this.config = config;
        this.units = List.copyOf(units);
        for (CompilationUnit cu : units) {
            for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (has(c, "Entity")) {
                    indexEntity(c);
                }
                classes.putIfAbsent(c.getNameAsString(), c);
                for (ClassOrInterfaceType t : c.getExtendedTypes()) {
                    supers.computeIfAbsent(c.getNameAsString(), k -> new ArrayList<>()).add(t.getNameAsString());
                    subs.computeIfAbsent(t.getNameAsString(), k -> new ArrayList<>()).add(c.getNameAsString());
                }
                for (ClassOrInterfaceType t : c.getImplementedTypes()) {
                    supers.computeIfAbsent(c.getNameAsString(), k -> new ArrayList<>()).add(t.getNameAsString());
                    subs.computeIfAbsent(t.getNameAsString(), k -> new ArrayList<>()).add(c.getNameAsString());
                }
                for (MethodDeclaration m : c.getMethods()) {
                    methodsByName.computeIfAbsent(m.getNameAsString(), k -> new ArrayList<>()).add(m);
                    owner.put(m, c.getNameAsString());
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

    /**
     * A name-and-arity match alone is weak: {@code get(x)} fits hundreds of project methods. So the receiver's static
     * type narrows it where it can be read from the source (a parameter, local variable, field, {@code this},
     * {@code new T()}, a static call, or the return type of another resolved call): only methods declared in that
     * type, its project supertypes or its project subtypes remain. A receiver whose type is not a project class (a
     * JDK or library type) has no project target at all. When the type cannot be read, name and arity are used, and a
     * call still matching more than {@link #MAX_TARGETS} methods is treated as unresolved. Real symbol resolution
     * would make all of this exact.
     */
    static final int MAX_TARGETS = 6;

    List<MethodDeclaration> targets(MethodCallExpr call) {
        List<MethodDeclaration> all = methods(call.getNameAsString()).stream()
                .filter(m -> m.getParameters().size() == call.getArguments().size()).toList();
        if (all.isEmpty()) {
            return all;
        }
        // -Ddsl.nonarrow=true switches narrowing off, to compare results with and without it
        String type = Boolean.getBoolean("dsl.nonarrow") ? null : receiverType(call);
        if (type != null) {
            if (!classes.containsKey(type)) {
                return List.of(); // a JDK or library type: not a project method
            }
            java.util.Set<String> allowed = family(type);
            all = all.stream().filter(m -> allowed.contains(owner.get(m))).toList();
        }
        return all.size() > MAX_TARGETS ? List.of() : all;
    }

    /** The type itself, everything it extends or implements, and everything that extends or implements it. */
    private java.util.Set<String> family(String type) {
        java.util.Set<String> out = new java.util.HashSet<>();
        collect(type, supers, out);
        java.util.Set<String> below = new java.util.HashSet<>();
        collect(type, subs, below);
        out.addAll(below);
        return out;
    }

    private static void collect(String start, Map<String, List<String>> edges, java.util.Set<String> out) {
        java.util.ArrayDeque<String> work = new java.util.ArrayDeque<>();
        work.add(start);
        while (!work.isEmpty()) {
            String t = work.poll();
            if (out.add(t)) {
                work.addAll(edges.getOrDefault(t, List.of()));
            }
        }
    }

    /** The simple type name of the call's receiver, or null when it cannot be read from the source. */
    private String receiverType(MethodCallExpr call) {
        if (call.getScope().isEmpty()) {
            return call.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::getNameAsString)
                    .orElse(null);
        }
        var scope = call.getScope().get();
        if (scope instanceof ThisExpr) {
            return call.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::getNameAsString)
                    .orElse(null);
        }
        if (scope instanceof NameExpr n) {
            String t = declaredSimpleType(n);
            if (t == null && Character.isUpperCase(n.getNameAsString().charAt(0))) {
                t = n.getNameAsString(); // a static call on a class name
            }
            return t;
        }
        if (scope instanceof FieldAccessExpr f && f.getScope() instanceof ThisExpr) {
            return fieldType(f.getNameAsString(), call);
        }
        if (scope instanceof ObjectCreationExpr o) {
            return o.getType().getNameAsString();
        }
        if (scope instanceof MethodCallExpr inner) {
            java.util.Set<String> returned = new java.util.HashSet<>();
            for (MethodDeclaration m : targets(inner)) {
                returned.add(simpleName(m.getType()));
            }
            return returned.size() == 1 ? returned.iterator().next() : null;
        }
        return null;
    }

    /** Declared type of a simple name: parameter, local variable, then fields of the enclosing classes. */
    String declaredSimpleType(NameExpr n) {
        String name = n.getNameAsString();
        var method = n.findAncestor(MethodDeclaration.class);
        if (method.isPresent()) {
            for (var p : method.get().getParameters()) {
                if (p.getNameAsString().equals(name)) {
                    return simpleName(p.getType());
                }
            }
            for (VariableDeclarator d : method.get().findAll(VariableDeclarator.class)) {
                if (d.getNameAsString().equals(name)) {
                    if (d.getType().isVarType()) {
                        return d.getInitializer().filter(i -> i instanceof ObjectCreationExpr)
                                .map(i -> ((ObjectCreationExpr) i).getType().getNameAsString()).orElse(null);
                    }
                    return simpleName(d.getType());
                }
            }
        }
        return fieldType(name, n);
    }

    private String fieldType(String name, com.github.javaparser.ast.Node from) {
        for (var c = from.findAncestor(ClassOrInterfaceDeclaration.class); c.isPresent();
             c = c.get().findAncestor(ClassOrInterfaceDeclaration.class)) {
            for (FieldDeclaration f : c.get().getFields()) {
                for (VariableDeclarator d : f.getVariables()) {
                    if (d.getNameAsString().equals(name)) {
                        return simpleName(d.getType());
                    }
                }
            }
        }
        return null;
    }

    private static String simpleName(Type t) {
        return t.isClassOrInterfaceType() ? t.asClassOrInterfaceType().getNameAsString() : null;
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

    /** Some file of the project uses this simple name: as a type, a class, or any identifier. */
    boolean mentions(String name) {
        if (mentionIndex == null) {
            java.util.Set<String> all = new java.util.HashSet<>();
            for (CompilationUnit u : units) {
                u.findAll(com.github.javaparser.ast.expr.SimpleName.class).forEach(s -> all.add(s.getIdentifier()));
            }
            mentionIndex = all;
        }
        return mentionIndex.contains(name);
    }

    private java.util.Set<String> mentionIndex;
    private final java.util.Set<String> settings = new java.util.HashSet<>();

    /** A key that some configuration file of the run defines (in kebab form with dots, as the config analyzer writes it). */
    void addSetting(String key) {
        settings.add(key);
    }

    /** Some configuration file of the run defines this key, spelt in any of the relaxed ways. */
    boolean hasSetting(String key) {
        String k = key.replaceAll("([a-z0-9])([A-Z])", "$1-$2").replace('_', '-').toLowerCase(java.util.Locale.ROOT);
        return settings.contains(k) || settings.stream().anyMatch(s -> s.startsWith(k + ".") || s.startsWith(k + "["));
    }

    private Map<String, String> sources = Map.of();

    /** The text of the source files of the project, by path, searched by {@link #references}. */
    void setSources(Map<String, String> sources) {
        this.sources = sources;
    }

    /**
     * The lines of source files that read a configuration key, as {@code path:line: text}, at most five. The key is
     * matched in the spellings Spring accepts: {@code a.max-retries}, {@code a.maxRetries}, {@code a.max_retries} and the
     * environment form {@code A_MAX_RETRIES}.
     */
    List<String> references(String key) {
        String kebab = key.replaceAll("([a-z0-9])([A-Z])", "$1-$2").replace('_', '-').toLowerCase(java.util.Locale.ROOT);
        StringBuilder camel = new StringBuilder();
        boolean up = false;
        for (char c : kebab.toCharArray()) {
            if (c == '-') {
                up = true;
            } else {
                camel.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        java.util.Set<String> spellings = new java.util.LinkedHashSet<>(List.of(kebab, camel.toString(),
                kebab.replace('-', '_'), kebab.replace('.', '_').replace('-', '_').toUpperCase(java.util.Locale.ROOT)));
        // A spelling counts only as a whole name, not inside a longer one (a.timeout inside a.timeout-ms or b.a.timeout).
        // A name with no dot may follow one (process.env.A_B).
        List<String> alternatives = new ArrayList<>();
        for (String sp : spellings) {
            alternatives.add((sp.contains(".") ? "(?<![A-Za-z0-9_.\\-])" : "(?<![A-Za-z0-9_\\-])") + java.util.regex.Pattern.quote(sp));
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:" + String.join("|", alternatives)
                + ")(?![A-Za-z0-9_\\-]|\\.[A-Za-z0-9])");
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            if (e.getKey().matches("(?i).*(^|/)(test|tests|__tests__)/.*|.*(Test|Tests|IT)\\.java|.*\\.(test|spec)\\.[jt]sx?|.*(^|/)test_[^/]*\\.py")) {
                continue; // a test reading the key does not keep it needed
            }
            String[] lines = e.getValue().split("\n", -1);
            for (int i = 0; i < lines.length && out.size() < 5; i++) {
                if (pattern.matcher(lines[i]).find()) {
                    out.add(e.getKey() + ":" + (i + 1) + ": " + lines[i].strip());
                }
            }
        }
        return out;
    }

    private Map<String, String> bases = Map.of();

    /** Files as they were at the diff base, by path (only the files that existed there). */
    void setBases(Map<String, String> bases) {
        this.bases = bases;
    }

    /**
     * The keys of the {@code ${key}} placeholders in {@code text} that have no default, where the file {@code path} had
     * {@code ${key:default}} at the diff base: the change took the default away. Empty with no base.
     */
    List<String> droppedDefaults(String path, String text) {
        String base = bases.get(path);
        List<String> out = new ArrayList<>();
        if (base == null || text == null) {
            return out;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$\\{([^}:]+)\\}").matcher(text);
        while (m.find()) {
            String key = m.group(1).strip();
            if (!out.contains(key) && base.contains("${" + key + ":")) {
                out.add(key);
            }
        }
        return out;
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
