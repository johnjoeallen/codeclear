package com.codeclear.rules.dsl;

import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleDefinition;
import com.codeclear.rules.CCRuleFinding;
import com.codeclear.rules.CCRuleResult;
import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the {@code .dsl} detectors of a rule in the restricted rule language. A rule cannot call arbitrary code, loop
 * forever or reach the file system, so unlike a Groovy script it needs no sandbox: it can only read the fields and
 * call the facts that the host defines.
 *
 * <p>Detection is done per project: a {@link Session} holds the project-wide model (entities, call graph,
 * configuration) built once from all analysed files, and runs rules on each file against it. Like the Groovy
 * detector it never throws for a bad rule: a parse or evaluation failure becomes a failed {@link CCRuleResult}.
 * Thread-safe.
 */
public final class DslRuleDetector {

    private final Map<String, RuleAst> parsed = new ConcurrentHashMap<>();

    /**
     * Builds the project model for a set of files and returns a handle that runs rules over them.
     *
     * @param config project configuration facts, by key (for example {@code spring.jpa.open-in-view})
     */
    public Session session(List<CCFileAnalysis> files, Map<String, String> config) {
        List<CompilationUnit> units = new ArrayList<>();
        for (CCFileAnalysis f : files) {
            if (f.ast() instanceof CompilationUnit cu) {
                units.add(cu);
            }
        }
        DslProject project = new DslProject(units, new HashMap<>(config));
        for (CCFileAnalysis f : files) {
            if (f.config() != null) {
                f.config().settings().forEach(s -> project.addSetting(s.key()));
            }
        }
        return new Session(project);
    }

    public final class Session {

        private final DslProject project;

        private Session(DslProject project) {
            this.project = project;
        }

        /** Runs every {@code .dsl} file of the rule on one file of the session. */
        public CCRuleResult detect(CCRuleDefinition rule, CCFileAnalysis file) {
            if (!rule.metadata().detectionType().executable() || !rule.appliesTo(file.file().getPath())) {
                return CCRuleResult.empty(rule.id());
            }
            if (rule.dsl().isEmpty()) {
                return CCRuleResult.failed(rule.id(), "no .dsl detector for this rule");
            }
            Set<CCRuleFinding> findings = new LinkedHashSet<>();
            Set<CCRuleCandidate> candidates = new LinkedHashSet<>();
            int dropped = 0;
            for (Map.Entry<String, String> source : rule.dsl().entrySet()) {
                List<DslInterpreter.Finding> raw;
                try {
                    RuleAst ast = parsed.computeIfAbsent(source.getValue(), DslParser::parse);
                    raw = new DslInterpreter().run(ast, file, rule.metadata().confidence().name(), project);
                } catch (RuntimeException | StackOverflowError e) {
                    return CCRuleResult.failed(rule.id(), source.getKey() + ": " + e.getMessage());
                }
                for (DslInterpreter.Finding f : raw) {
                    if (!file.changedLines().contains(f.line())) {
                        dropped++;
                    } else if (f.candidate()) {
                        candidates.add(new CCRuleCandidate(rule.id(), file.file().getPath(), f.line(), f.message()));
                    } else {
                        findings.add(new CCRuleFinding(rule.id(), file.file().getPath(), f.line(), f.message(),
                                rule.metadata().severity(), confidence(rule, f.confidence())));
                    }
                }
            }
            List<CCRuleFinding> sortedFindings = new ArrayList<>(findings);
            sortedFindings.sort(Comparator.comparingInt(CCRuleFinding::line).thenComparing(CCRuleFinding::message));
            List<CCRuleCandidate> sortedCandidates = new ArrayList<>(candidates);
            sortedCandidates.sort(Comparator.comparingInt(CCRuleCandidate::line).thenComparing(CCRuleCandidate::evidence));
            return new CCRuleResult(rule.id(), List.copyOf(sortedFindings), List.copyOf(sortedCandidates), dropped,
                    Optional.empty());
        }
    }

    /** A rule may lower its own confidence, never raise it. */
    private static CCRuleConfidence confidence(CCRuleDefinition rule, String requested) {
        return CCRuleConfidence.lowest(rule.metadata().confidence(), CCRuleConfidence.valueOf(requested.toUpperCase(Locale.ROOT)));
    }
}
