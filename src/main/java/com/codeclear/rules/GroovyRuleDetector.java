package com.codeclear.rules;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Runs the deterministic part of a rule: a Groovy script that receives the parsed tree and returns findings.
 *
 * <p>Per rule and file, the detector:
 * <ol>
 *   <li>checks the rule applies to the path and has a script;</li>
 *   <li>compiles the script under {@link CCRuleSandbox} (cached by script text);</li>
 *   <li>runs it on a worker thread with the bindings {@code ast}, {@code file}, {@code model}, {@code diff},
 *       interrupting it after the timeout;</li>
 *   <li>keeps only results on changed lines, applies the rule's severity and confidence ceiling, sorts and
 *       de-duplicates.</li>
 * </ol>
 * It never throws for a bad script: a compile error, sandbox violation, timeout or script exception becomes a
 * failed {@link CCRuleResult}, so one broken rule cannot stop a review. The detector is thread-safe.
 */
public final class GroovyRuleDetector implements AutoCloseable {

    /** Default time one script may spend on one file. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final Duration timeout;
    private final GroovyClassLoader loader;
    private final ExecutorService workers;
    private final Map<String, Class<? extends Script>> compiled = new ConcurrentHashMap<>();

    public GroovyRuleDetector() {
        this(DEFAULT_TIMEOUT);
    }

    public GroovyRuleDetector(Duration timeout) {
        this.timeout = timeout;
        this.loader = new GroovyClassLoader(GroovyRuleDetector.class.getClassLoader(), CCRuleSandbox.configuration());
        this.workers = Executors.newCachedThreadPool(runnable -> {
            Thread t = new Thread(runnable, "codeclear-rule");
            t.setDaemon(true);
            return t;
        });
    }

    /** Runs every rule on the file, in the order given. */
    public List<CCRuleResult> detectAll(List<CCRuleDefinition> rules, CCFileAnalysis file) {
        return rules.stream().map(rule -> detect(rule, file)).toList();
    }

    /** Runs one rule on one file. */
    public CCRuleResult detect(CCRuleDefinition rule, CCFileAnalysis file) {
        if (!rule.metadata().detectionType().executable() || !rule.appliesTo(file.file().getPath())) {
            return CCRuleResult.empty(rule.id());
        }
        if (rule.script().isEmpty()) {
            return CCRuleResult.failed(rule.id(), "no script generated for this rule");
        }
        Class<? extends Script> scriptClass;
        try {
            scriptClass = compile(rule);
        } catch (CCRuleCompilationException e) {
            return CCRuleResult.failed(rule.id(), e.getMessage());
        }
        Object returned;
        try {
            returned = run(scriptClass, file);
        } catch (TimeoutException e) {
            return CCRuleResult.failed(rule.id(), "script exceeded " + timeout.toMillis() + " ms");
        } catch (ExecutionException e) {
            return CCRuleResult.failed(rule.id(), "script failed: " + CCRuleSandbox.reason(e.getCause()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CCRuleResult.failed(rule.id(), "interrupted");
        }
        try {
            return collect(rule, file, returned);
        } catch (IllegalArgumentException e) {
            return CCRuleResult.failed(rule.id(), "script returned an invalid result: " + e.getMessage());
        }
    }

    /** Compiles under the sandbox. Throws {@link CCRuleCompilationException} on a syntax error or sandbox violation. */
    Class<? extends Script> compile(CCRuleDefinition rule) {
        String source = rule.script().orElseThrow();
        String key = rule.id() + "#" + Integer.toHexString(source.hashCode()) + "#" + source.length();
        Class<? extends Script> cached = compiled.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String name = "rule_" + rule.id().replaceAll("[^A-Za-z0-9]", "_");
            GroovyCodeSource code = new GroovyCodeSource(source, name + ".groovy", "/codeclear/rules");
            code.setCachable(false);
            @SuppressWarnings("unchecked")
            Class<? extends Script> c = (Class<? extends Script>) loader.parseClass(code, false);
            compiled.put(key, c);
            return c;
        } catch (RuntimeException | LinkageError e) {
            throw new CCRuleCompilationException(rule.id() + ": " + CCRuleSandbox.reason(e), e);
        }
    }

    private Object run(Class<? extends Script> scriptClass, CCFileAnalysis file)
            throws TimeoutException, ExecutionException, InterruptedException {
        Binding binding = new Binding();
        binding.setVariable("ast", file.ast());
        binding.setVariable("file", file.file());
        binding.setVariable("model", file.model());
        binding.setVariable("diff", file.changedLines());
        Future<Object> future = workers.submit(() -> InvokerHelper.createScript(scriptClass, binding).run());
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            future.cancel(true);
            throw e;
        }
    }

    private CCRuleResult collect(CCRuleDefinition rule, CCFileAnalysis file, Object returned) {
        if (returned == null) {
            return CCRuleResult.empty(rule.id());
        }
        if (!(returned instanceof List<?> items)) {
            throw new IllegalArgumentException("expected a list, got " + returned.getClass().getName());
        }
        Set<CCRuleFinding> findings = new LinkedHashSet<>();
        Set<CCRuleCandidate> candidates = new LinkedHashSet<>();
        int dropped = 0;
        for (Object item : items) {
            if (item instanceof CCRuleScriptBase.Finding f) {
                if (!file.changedLines().contains(f.line())) {
                    dropped++;
                    continue;
                }
                findings.add(new CCRuleFinding(rule.id(), file.file().getPath(), f.line(), requireText(f.message()),
                        rule.metadata().severity(), confidence(rule, f.confidence())));
            } else if (item instanceof CCRuleScriptBase.Candidate c) {
                if (!file.changedLines().contains(c.line())) {
                    dropped++;
                    continue;
                }
                candidates.add(new CCRuleCandidate(rule.id(), file.file().getPath(), c.line(), requireText(c.evidence())));
            } else {
                throw new IllegalArgumentException("unexpected element " + (item == null ? "null" : item.getClass().getName())
                        + "; build results with finding(...) or candidate(...)");
            }
        }
        List<CCRuleFinding> sortedFindings = new ArrayList<>(findings);
        sortedFindings.sort(Comparator.comparingInt(CCRuleFinding::line).thenComparing(CCRuleFinding::message));
        List<CCRuleCandidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(Comparator.comparingInt(CCRuleCandidate::line).thenComparing(CCRuleCandidate::evidence));
        return new CCRuleResult(rule.id(), List.copyOf(sortedFindings), List.copyOf(sortedCandidates), dropped, Optional.empty());
    }

    /** The script may lower the rule's confidence, never raise it. */
    private static CCRuleConfidence confidence(CCRuleDefinition rule, String requested) {
        CCRuleConfidence ceiling = rule.metadata().confidence();
        if (requested == null) {
            return ceiling;
        }
        try {
            return CCRuleConfidence.lowest(ceiling, CCRuleConfidence.valueOf(requested.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown confidence '" + requested + "'");
        }
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("empty message");
        }
        return text;
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
