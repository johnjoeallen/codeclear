package com.codeclear.rules;

import com.codeclear.rules.dsl.DslRuleDetector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the deterministic part of every rule over a set of files, choosing a detector per rule: the restricted-language
 * {@code .dsl} detector or the Groovy {@code rule.groovy} script. A rule that has both uses the preferred one. A rule
 * with neither is reported as failed (when it is executable) rather than silently skipped, so a review can list the
 * rules that did not run.
 *
 * <p>The rules see all files of a run together: {@code .dsl} detectors build a project model (entities, call graph,
 * configuration) from them, so cross-file rules work on the same set the review covers.
 */
public final class CCRuleEngine implements AutoCloseable {

    /** Which detector wins when a rule has both. */
    public enum Prefer {
        /** The restricted language: no sandbox needed, so it is the default. */
        DSL,
        /** The Groovy script. */
        GROOVY
    }

    private final GroovyRuleDetector groovy;
    private final DslRuleDetector dsl;
    private final Prefer prefer;

    public CCRuleEngine() {
        this(new GroovyRuleDetector(), new DslRuleDetector(), Prefer.DSL);
    }

    public CCRuleEngine(GroovyRuleDetector groovy, DslRuleDetector dsl, Prefer prefer) {
        this.groovy = groovy;
        this.dsl = dsl;
        this.prefer = prefer;
    }

    /**
     * Runs each rule on each file it applies to.
     *
     * @param config project configuration facts for cross-file rules, by key
     * @return one result per rule and file, in rule then file order
     */
    public List<CCRuleResult> run(List<CCRuleDefinition> rules, List<CCFileAnalysis> files, Map<String, String> config) {
        return run(rules, files, config, Map.of());
    }

    /**
     * As {@link #run(List, List, Map)}, with the text of the project's source files by path, which a rule can search for
     * the readers of a configuration key.
     */
    public List<CCRuleResult> run(List<CCRuleDefinition> rules, List<CCFileAnalysis> files, Map<String, String> config,
                                  Map<String, String> sources) {
        return run(rules, files, config, sources, Map.of());
    }

    /** As above, with the files as they were at the diff base, by path. */
    public List<CCRuleResult> run(List<CCRuleDefinition> rules, List<CCFileAnalysis> files, Map<String, String> config,
                                  Map<String, String> sources, Map<String, String> bases) {
        DslRuleDetector.Session session = dsl.session(files, config, sources, bases);
        List<CCRuleResult> results = new ArrayList<>();
        for (CCRuleDefinition rule : rules) {
            for (CCFileAnalysis file : files) {
                results.add(useDsl(rule) ? session.detect(rule, file) : groovy.detect(rule, file));
            }
        }
        return results;
    }

    /** Which detector a rule runs on. */
    boolean useDsl(CCRuleDefinition rule) {
        boolean hasDsl = !rule.dsl().isEmpty();
        boolean hasGroovy = rule.script().isPresent();
        return hasDsl && (prefer == Prefer.DSL || !hasGroovy);
    }

    @Override
    public void close() {
        groovy.close();
    }
}
