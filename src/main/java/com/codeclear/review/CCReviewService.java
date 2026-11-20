package com.codeclear.review;

import com.codeclear.ai.CCAiDriver;
import com.codeclear.ai.CCAiDriverRegistry;
import com.codeclear.ai.CCAiDriverStatus;
import com.codeclear.ai.CCCandidateConfirmer;
import com.codeclear.ai.CCCandidateVerdict;
import com.codeclear.ai.CCDriverCandidateConfirmer;
import com.codeclear.config.CodeClearProperties;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleDefinition;
import com.codeclear.rules.CCRuleEngine;
import com.codeclear.rules.CCRuleFinding;
import com.codeclear.rules.CCRuleLoader;
import com.codeclear.rules.CCRuleResult;
import com.codeclear.rules.JavaFileAnalyzer;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Reviews a change: runs the deterministic detector of every applicable rule, asks the AI to judge the candidates
 * hybrid rules raise, and reports everything that could not be checked.
 *
 * <p>The AI is optional. With no AI backend integrated (the {@code dummy} driver) candidates stay unconfirmed and
 * rules that have no deterministic part are listed as skipped, so a review still runs and says what it did not
 * cover. Rules whose only check is judgement (comments, test quality, intent) are always skipped for now: whole-file
 * AI review is not built.
 */
@Service
public class CCReviewService implements AutoCloseable {

    private static final String JAVA = ".java";

    private final List<CCRuleDefinition> rules;
    private final CCRuleEngine engine;
    private final CCCandidateConfirmer confirmer;
    private final String aiDriverId;
    private final JavaFileAnalyzer analyzer = new JavaFileAnalyzer();
    private final com.codeclear.rules.SqlFileAnalyzer sqlAnalyzer = new com.codeclear.rules.SqlFileAnalyzer();
    private final com.codeclear.rules.ConfigFileAnalyzer configAnalyzer = new com.codeclear.rules.ConfigFileAnalyzer();

    /** The service the application runs: rules from the configured folder, the configured AI driver. */
    @Autowired
    public CCReviewService(CCAiDriverRegistry registry, CodeClearProperties properties) {
        this(loadRules(Path.of(properties.rulesDir())), new CCRuleEngine(),
                confirmerFor(registry, properties), properties.ai().driver());
    }

    public CCReviewService(List<CCRuleDefinition> rules, CCRuleEngine engine, CCCandidateConfirmer confirmer,
                           String aiDriverId) {
        this.rules = List.copyOf(rules);
        this.engine = engine;
        this.confirmer = confirmer;
        this.aiDriverId = aiDriverId;
    }

    /** A YAML, JSON or properties file. */
    private static boolean isConfig(String path) {
        String p = path.toLowerCase();
        return p.endsWith(".yml") || p.endsWith(".yaml") || p.endsWith(".json") || p.endsWith(".properties");
    }

    private static List<CCRuleDefinition> loadRules(Path dir) {
        return Files.isDirectory(dir) ? CCRuleLoader.loadAll(dir) : List.of();
    }

    private static CCCandidateConfirmer confirmerFor(CCAiDriverRegistry registry, CodeClearProperties properties) {
        String id = properties.ai().driver();
        Optional<CCAiDriver> driver = registry.find(id);
        if (driver.isPresent()) {
            return new CCDriverCandidateConfirmer(driver.get(), Duration.ofSeconds(properties.promptTimeoutSeconds()));
        }
        CCAiDriverStatus missing = CCAiDriverStatus.notReady(
                "AI driver '" + id + "' is not available (available: " + registry.ids() + ")");
        return new CCCandidateConfirmer() {
            @Override
            public CCAiDriverStatus status() {
                return missing;
            }

            @Override
            public CCCandidateVerdict confirm(CCRuleDefinition rule, CCRuleCandidate candidate, String source) {
                return CCCandidateVerdict.noVerdict(missing.detail());
            }
        };
    }

    public CCReviewResult review(CCReviewRequest request) {
        List<String> notes = new ArrayList<>();
        Map<String, String> sources = new LinkedHashMap<>();
        List<CCFileAnalysis> analyses = new ArrayList<>();
        for (CCReviewFile f : request.files()) {
            sources.put(f.path(), f.content());
            if (f.path().toLowerCase().endsWith(".sql")) {
                analyses.add(sqlAnalyzer.analyze(f.path(), f.content(), f.changedLines()));
                continue;
            }
            if (isConfig(f.path())) {
                try {
                    analyses.add(configAnalyzer.analyze(f.path(), f.content(), f.changedLines()));
                } catch (IllegalArgumentException e) {
                    notes.add("not analysed: " + e.getMessage());
                }
                continue;
            }
            if (!f.path().endsWith(JAVA)) {
                continue;
            }
            try {
                analyses.add(analyzer.analyze(f.path(), f.content(), f.changedLines()));
            } catch (IllegalArgumentException e) {
                notes.add("not analysed: " + e.getMessage());
            }
        }
        long other = request.files().stream().filter(f -> !f.path().endsWith(JAVA) && !f.path().toLowerCase().endsWith(".sql") && !isConfig(f.path())).count();
        if (other > 0) {
            notes.add(other + " file(s) not analysed: only Java, SQL and config files have an analyzer so far");
        }

        List<CCSkippedRule> skipped = new ArrayList<>();
        List<CCRuleDefinition> runnable = new ArrayList<>();
        for (CCRuleDefinition rule : rules) {
            boolean applies = request.files().stream().anyMatch(f -> rule.appliesTo(f.path()));
            if (!applies) {
                continue;
            }
            boolean analysable = analyses.stream().anyMatch(a -> rule.appliesTo(a.file().getPath()));
            if (!analysable) {
                skipped.add(new CCSkippedRule(rule.id(), CCSkippedRule.Reason.NO_ANALYZER,
                        "applies only to files of a language with no analyzer yet"));
            } else if (!rule.metadata().detectionType().executable()) {
                skipped.add(new CCSkippedRule(rule.id(), CCSkippedRule.Reason.NEEDS_AI,
                        "the check needs the AI (whole-file AI review is not built yet)"));
            } else if (!rule.hasDetector()) {
                skipped.add(new CCSkippedRule(rule.id(), CCSkippedRule.Reason.NO_DETECTOR, "no detector written yet"));
            } else {
                runnable.add(rule);
            }
        }

        Map<String, CCRuleDefinition> byId = new LinkedHashMap<>();
        rules.forEach(r -> byId.put(r.id(), r));
        List<CCReviewFinding> findings = new ArrayList<>();
        List<CCRuleCandidate> unconfirmed = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();
        int dismissed = 0;
        List<CCSuppressedFinding> suppressedList = new ArrayList<>();
        Map<String, CCSuppressions> suppressions = new java.util.HashMap<>();
        CCAiDriverStatus ai = confirmer.status();
        boolean noteAi = false;

        for (CCRuleResult result : engine.run(runnable, analyses, request.projectConfig())) {
            if (!result.ok()) {
                if (!failedRules.contains(result.ruleId())) {
                    failedRules.add(result.ruleId());
                    skipped.add(new CCSkippedRule(result.ruleId(), CCSkippedRule.Reason.FAILED, result.failure().get()));
                }
                continue;
            }
            CCRuleDefinition rule = byId.get(result.ruleId());
            for (CCRuleFinding f : result.findings()) {
                if (suppressed(suppressions, sources, f.path(), f.line(), f.ruleId())) {
                    suppressedList.add(new CCSuppressedFinding(f.ruleId(), f.path(), f.line(), f.message()));
                    continue;
                }
                findings.add(new CCReviewFinding(f.ruleId(), f.path(), f.line(), f.message(), f.severity(), f.confidence(),
                        CCReviewFinding.Source.RULE));
            }
            for (CCRuleCandidate c : result.candidates()) {
                if (suppressed(suppressions, sources, c.path(), c.line(), c.ruleId())) {
                    suppressedList.add(new CCSuppressedFinding(c.ruleId(), c.path(), c.line(), c.evidence()));
                    continue;
                }
                CCCandidateVerdict verdict = confirmer.confirm(rule, c, sources.getOrDefault(c.path(), ""));
                switch (verdict.kind()) {
                    case CONFIRMED -> findings.add(new CCReviewFinding(c.ruleId(), c.path(), c.line(), c.evidence(),
                            rule.metadata().severity(), rule.metadata().confidence(), CCReviewFinding.Source.AI_CONFIRMED));
                    case DISMISSED -> dismissed++;
                    case NO_VERDICT -> {
                        unconfirmed.add(c);
                        noteAi = true;
                    }
                }
            }
        }
        if (!suppressedList.isEmpty()) {
            notes.add(suppressedList.size() + " finding(s) ignored by CodeClear: comments");
        }
        if (noteAi && !ai.ready()) {
            notes.add(ai.detail());
        }

        findings.sort(Comparator.comparing(CCReviewFinding::path).thenComparingInt(CCReviewFinding::line)
                .thenComparing(CCReviewFinding::ruleId));
        return new CCReviewResult(List.copyOf(findings), List.copyOf(unconfirmed), List.copyOf(skipped),
                List.copyOf(notes), runnable.size() - failedRules.size(), dismissed, aiDriverId, ai.ready() ? "ready" : ai.detail(),
                List.copyOf(suppressedList));
    }

    /** The line is covered by a {@code CodeClear:} comment (see {@link CCSuppressions}). */
    private static boolean suppressed(Map<String, CCSuppressions> cache, Map<String, String> sources, String path, int line,
                                      String ruleId) {
        return cache.computeIfAbsent(path, p -> CCSuppressions.parse(p, sources.getOrDefault(p, ""))).covers(line, ruleId);
    }

    @Override
    @PreDestroy
    public void close() {
        engine.close();
    }
}
