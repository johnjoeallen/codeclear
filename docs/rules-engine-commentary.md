# Rules Engine: Running Commentary

A running log of decisions, discoveries, recommendations and open questions while designing the
script-based rule engine. Newest entries go at the bottom of each section. The formal design lives in
`ai-generated-deterministic-checks.md`; this file records the reasoning and what we learned.

## Table of Contents

- [Decisions](#decisions)
- [Discoveries](#discoveries)
- [Recommendations](#recommendations)
- [Open questions](#open-questions)
- [Log](#log)

## Decisions

| # | Decision | Made |
|---|---|---|
| D1 | Deterministic and hybrid rules run their deterministic part as scripts through the Java scripting API (JSR-223). | 2026-11-20 |
| D2 | The script language is Groovy (`groovy-jsr223`). | 2026-11-20 |
| D3 | The core engine parses the source and passes the AST to the script. Scripts never parse files. | 2026-11-20 |
| D4 | When a rule `.md` changes, the AI regenerates its script. | 2026-11-20 |
| D5 | The tool regenerates stale scripts itself at review time, using the same AI driver as the review, because the AI is already required for `ai` and `hybrid` rules. | 2026-11-20 |
| D6 | Every rule has its own folder, `review/<lang>/<id>/`, holding `rule.md`, `rule.groovy` and `tests.md`. Tests were `tests.json` at first and moved to Markdown on 2026-11-21 so people can review them and the AI can write them. | 2026-11-20 |
| D7 | The restricted rule language uses a Groovy-flavoured surface syntax (`Catch.each { c -> ... }`, `only`, `skip`, `report`, `def`, closures) so developers can read it, parsed by a custom parser, not by Groovy (Groovy's parser is too heavy). Same rule tree underneath, so the interpreter, facts and mutation tooling did not change. | 2026-11-21 |

## Discoveries

### Rule inventory (from `review/INDEX.md`)

- 320 rules: 59 deterministic, 113 hybrid, 148 AI-only. By folder: java 85, typescript 107, python 94,
  common 19, sql 9, config 6.
- The AI is doing three different jobs, and only one needs a model:
  1. **Context lookup** (largest group): the rule's `Detect` and `Do not flag` sections are already
     nearly executable, but the detector sees one file or method. The AI is asked to check things such
     as `@Version`, `open-in-view`, container ack mode, security config, or whether a catch is at a
     legitimate boundary. These are computable from a project-wide model.
  2. **Intent**: the syntax is deterministic and the AI decides whether the outcome was meant
     (`transactional-rollback-mismatch`, `lost-error-detail`, `floating-point-money`). These can emit
     at lower confidence and severity instead of calling the model.
  3. **Genuine semantic judgement**: comments versus code, log wording, log level, test quality.
     These stay with the AI.
- Rough estimate, from reading about 15 of the 261 `hybrid` and `ai` files and skimming the Java
  titles, **not a measurement**: about 70-80% of `hybrid` and 30-40% of Java `ai` rules could become
  fully deterministic with a code model. The comment, log-wording and test-quality rules (about 25-30
  per language) stay AI.
- A lock-ordering deadlock, lost update, lazy-loading outside a session and a missing endpoint
  authorization are all decidable by queries over a call graph, entity model and parsed config.

### Environment

- JDK 25 has no built-in script engine (Nashorn was removed in JDK 15), so an engine must be bundled.
- Groovy 4.0.x and JavaParser (core and symbol solver) are in the local Maven repository. The
  `sdkman` Groovy CLI is too old for JDK 25 (`Unsupported class file major version 69`); run scripts
  with the Groovy 4.0.30 jar from the Maven repository instead.
- `pom.xml` has no Groovy or JavaParser dependency yet.
- No rule file mentioned scripting before this work; `docs/design.md` has no JSR-223 reference.

### From writing and running real scripts

Two scripts exist: `java/empty-catch-block` and `java/optional-get-without-check`. Both pass their
tests in a throwaway harness (not the real engine: no sandbox, no coverage-label check, no
changed-line filter).

- **Groovy scope trap.** Local variables are not visible inside script-level methods
  (`MissingPropertyException: No such property: QUIET_LEVELS`). First generated attempt failed on it.
  Rewriting the helper as a closure fixed it. Generator prompts should say: use closures, not script
  methods, for helpers that read script-level locals. This is exactly the failure class the
  test-and-retry loop in the acceptance gate is for.
- **The contract grew from usage.** Writing real rules showed the bindings needed `file` (path and
  language, for test-source exclusion), a `model.typeOf(expr)` call, and an optional confidence
  argument on `finding(...)`. Expect more additions; the `api-version` header exists for this reason.
- **Tests can be checked for teeth.** Deliberately breaking `optional-get-without-check` three ways
  (drop the guard check, drop the type check, drop the abrupt-exit requirement) failed the expected
  tests each time. Worth automating as part of the acceptance gate (mutation testing of the script,
  not only of the examples).
- **Type resolution decides the confidence.** With a resolved type, `Future.get()`, `Supplier.get()`,
  `Map.get()` and friends are excluded exactly. With an unresolved type the script has to fall back to
  names (`findById`, `Optional<...>` declarations) and report `LOW`. This confirms the rule's own
  note and means classpath resolution quality directly sets false-positive and false-negative rates.
- **Rule text can be wrong.** `optional-get-without-check` lists `ifPresent` and `orElse*` as guards,
  but `orElse` proves nothing about presence. The generator had to pick an interpretation. The
  generation step will not catch this kind of mistake; only a human reading the rule will. See R6.
- **Dominance matters.** Naive "an `isPresent` appears earlier" checks give false negatives. Real
  checks needed: then/else branch, early exit (`return`/`throw`/`continue`/`break`), ternary,
  short-circuit `&&`/`||`, `assert`, and `ifPresent` lambdas, all on the same variable.
- **Known limitation.** A variable reassigned between the guard and the call is not tracked.

### Groovy sandbox (built and tested)

- `GroovyRuleDetector` and `CCRuleSandbox` exist with 54 tests (about 32 forbidden-script cases). The sandbox is
  best effort: a class allowlist, method and property name denylists, no computed names, no annotations, `@Grab`
  disabled, extra imports and static imports rejected, `ThreadInterrupt` plus a worker-thread timeout.
- **Not a security boundary.** No in-JVM boundary exists on JDK 25. JavaParser mutators other than the denylisted
  ones stay callable.
- **The timeout reports but cannot stop a catastrophic `java.util.regex` match** (verified with `(.*a){12}`;
  `(a+)+$` did not blow up on this JDK). A linear-time engine (RE2/J, already in the local Maven repo) fixes it for
  rules that use regex through the host, but not for a script that calls `java.util.regex` directly.

### Alternative: a restricted rule language (the Distill idea from `../arete`)

- Arete's Distill is a single-expression language with no locals or recursion over an immutable fact model, using
  RE2/J. Its history went Groovy, then Starlark, then Distill. Measured about 4-5 times faster than compiled
  Groovy and 5-6 times slower than hand-written Java.
- **Core insight:** safety comes from moving all computation into trusted, host-built facts, so rules become thin
  queries over them.
- **Tiers of the 172 executable rules**, classified by hand from the Detect sections (TypeScript and Python tiers
  are less certain: only 300 characters read; the 148 AI-only rules were not analysed):

  | Tier | Rules | Needs |
  |---|---|---|
  | 0 | 19 (11%) | node kind, attributes, linear-time regex, list and glob membership |
  | 1 | 68 (40%) | adds inside/contains, sibling order, absence, counts, per-file aggregation, text similarity |
  | 2 | 75 (44%) | host-supplied facts: taint (about 13 rules), types (15), framework classification (12), def-use (12), guards (8), project/config lookup |
  | 3 | 10 (6%) | path-sensitive flow, call graph, two-version diff, external linters |

  Tiers 0 and 1 (87 rules, 51%) need only a pure query language over the syntax tree. Tier 3 should be host
  analyzers or stay with the AI.

### Experiment: the two Groovy rules in a restricted language (test scope only)

Engine code is `com.codeclear.rules.dsl` (it started in test scope and moved to main when wired in); the rules are `rule.dsl` files in their rule folders under `review/`, beside `rule.groovy` where one exists.

- Both rules pass their existing tests unchanged: 17 and 33 cases. Size: 53 to 15 lines and 160 to 22. The
  90 lines of guard logic in `optional-get-without-check` became one host fact, `c.guarded(recv, present)`,
  reusable by about 8 other rules.
- **Findings that changed the design:** statement wrappers must be explicit (`ExprStmt.expression`; the Groovy had
  hidden this); `let` must be lazy and a field known on another node kind must read as missing (found when a
  generated rule failed on `declaredType` of a `Call`); first matching `flag` wins per node because `all` over an
  empty list is true.
- **Automatic mutation testing works** because rules are data: delete a clause, drop an `and`/`or` operand, negate
  a leaf test. 43 mutants initially, 11 survived: 5 real test gaps (`getAsLong`, `getAsDouble`, `OptionalLong`,
  `OptionalDouble`, no resolved-Optional-with-unlisted-method case), 5 redundant clauses, 1 message-only. After
  adding the missing tests, 5 survive, all redundant or equivalent. The same gaps existed in the Groovy tests.
- **Not yet shown:** the language on a tier 2 rule, its speed, or a real RE2/J engine. The schema was designed
  around these two rules.

### Experiment: can an AI write the language from a grammar page

Fresh Sonnet 5.5 agents, one shot, given only a grammar page and `rule.md`. 3 attempts per rule. Test results
decide pass or fail; the tests are held out from the generator (they were `tests.json` then, `tests.md` now).

- **Syntax is not the risk.** All attempts parsed, including predicates, ternary confidence and a regex.
- **Round 0:** `empty-catch-block` 3/3 passed all 16 cases; `optional-get-without-check` 0/3 (29, 29, 28 of 30).
  Every `optional-get` miss was one case: the rule text said "naming heuristic" without naming the producer
  methods. A spec gap, not generator error. One attempt also hit the eager-`let` bug.
- **Two of three `empty-catch` attempts exempted every `InterruptedException`**, where the rule exempts only a
  catch that restores the interrupt flag. No test caught it, and the exemption was inert only because of a bug in
  the experiment (`Param.type` returned a node but the grammar said text, so the `==` never matched). After fixing
  that, a new test case caught it.
- **Round 1** (grammar, `rule.md` and tests corrected): `optional-get` 3/3 pass all 33; `empty-catch` 1/3 pass
  (the same two attempts repeated the `InterruptedException` exemption).
- **Retry with gate feedback** (the failing case source, expected and actual): both fixed it in one round. One
  implemented the rule properly (exempt only when the body calls `interrupt()`); the other deleted the exemption.
- **Conclusion:** failures came from vague rule text and untested exclusions, not syntax. A generator that sees
  the failing case fixes itself, so the gate's test coverage is the real control. Caveats: two rules, N=3, easy
  rules, schema built for them, and hand-written tests decided pass or fail.

### Experiment: a tier 2 rule, `lazy-association-outside-session` (test scope only)

- Split into two restricted-language rules of about 12 lines each (reads of a lazy association; controllers returning
  entities with lazy associations), run over small multi-file projects. 33 cases in
  `review/java/lazy-association-outside-session/tests.md`; mutation testing leaves one redundant-clause survivor.
- Needs a project model and about 10 host facts: entity and association index, `lazyAssociation`, `dereferenced`,
  `inTransaction`, `fetched`, `returnsFetched`, `inController`, `returnedEntity`, `lazyAssociations`,
  `project.config(key)`. One new operator, `x inside Loop`.
- **Interprocedural analysis is deterministic, not an AI job.** An early version stopped at one call and I wrongly
  suggested handing deeper flows to the AI. The `fetched` and `inTransaction` facts now walk the project call graph:
  through return statements, local variables, `Optional` unwrapping, parameters (every caller's argument must be
  fetched) and callers (every caller must run in a transaction), with cycle guards. Matching is by method name and
  arity as a stand-in for real symbol resolution.
- Remaining AI-only part: judgement the call graph cannot make (whether a serializer view hides a field, Lombok
  getters, reflective or framework-generated calls).
- A spec ambiguity surfaced: the N+1 flag does not need a transaction, so the condition was dropped.

### Experiment: a taint rule, `common/log-injection` (test scope only)

- The other large fact family (about 13 rules). The rule is 25 lines; the engine is `DslTaint`, about 190 lines of
  reviewed Java; 31 cases in `review/common/log-injection/tests.md`.
- **Design that worked: policy in the rule, mechanism in the host.** The rule defines what is a source and what
  neutralises a value as two named predicates (`isSource`, `isSanitized`) and asks `a.tainted(isSource, isSanitized)`.
  The host walks the flow and calls the predicates back. This needed one language feature: predicates passed as
  arguments to a fact. Other taint rules (SQL, command, path, SSRF) would reuse the engine and differ only in policy.
- **Flow handled:** concatenation, ternaries, casts, local variables with strong updates (a later assignment in an
  enclosing block kills earlier ones; a branch-only sanitiser does not), compound assignment, pass-through string
  methods, getters on a tainted object, `String.format`/`valueOf`, parameters (any caller may pass taint) and values
  returned from project methods. Interprocedural flow needs no AI, as with `fetched`.
- **Defects the tests found:** `replaceAll` was missing from the pass-through list, so a `replaceAll` that does not
  strip line breaks wrongly cleared the taint. A second gap was in the mutation harness, not the rule: with a failing
  baseline case every mutant counted as killed. The harness now requires a passing baseline.
- **Mutation testing** exposed untested log levels (`trace`, `debug`, `error`) and a missing "not a log call" case;
  two survivors remain, both equivalent (`c.scope exists`, `c.args is not empty`).
- **Not handled, and where a rule would still be wrong:** values stored in fields or collections and logged
  elsewhere, aliasing, lambdas, framework data binding into object fields, and path sensitivity beyond block
  dominance. A sanitiser is accepted by its name or a text match, not proven to remove line breaks.
- **Structured loggers** are modelled as a config key (`logging.structured`). The real engine must parse
  logback, log4j2 and Spring logging config into such facts.
- **Still judgement:** `rule.md` asks to report SECURITY only for request parameters, headers and body, and
  LOGGING/INFO otherwise. That split is deterministic too (run `tainted` twice with narrower source predicates) and
  was not built; how attacker-controlled a value really is stays with the AI.

### Test-quality rules: mutation testing instead of AI judgement

- Estimate of what still needs the AI (by rule count, from a title-level reading of the 148 AI-only rules; not a
  measurement): about 55% fully deterministic (about 175 rules), about 30% deterministic candidates with an AI verdict
  (about 95), about 15% AI-only (about 50). Roughly 45% (38 to 50%) still need the model for the final verdict.
- 7 of the 8 test-quality rules are covered by mutation testing, which moves the "still needs AI" figure to about
  43%. Four ("tests omit the risks of the change") by generic mutants on changed lines; three (mock-only tests offered
  as proof of a guarantee) by targeted mutants that break the guarantee, such as removing `@Transactional` or acking
  before the write. A mock cannot observe such a mutant, so it survives. `mock-verifies-implementation-details` is
  not caught by mutants and stays partly AI. Design in `targeted-mutation-design.md`, worked example
  `java/mocked-test-cannot-prove-transaction-guarantee`.
- Where it runs: a code review does not run mutation testing. It runs on the developer side or in CI, where the tests
  already execute. The review tool exposes it as an opt-in option (`--mutation`) of an overall review, off by
  default. With the option off, the seven rules are reported as not checked. A claim in prose with no recognisable
  construct in the code stays with the AI.

### Performance sanity check (speed is not a priority; it should be reasonable)

- On 3,990 files and 284k lines of real Java: parsing 1.9 s once, the project model 0.14 s, then each
  restricted-language rule over the whole project 46 to 165 ms (log-injection, the slowest, 165 ms). The two Groovy
  rules took 94 and 158 ms. Both engines found the same 19 and 14 findings on that code.
- **The benchmark found a real flaw.** The taint engine's return-value analysis was exponential: with no memoisation,
  a call such as `get(x)` matched hundreds of project methods by name and arity, and each was followed in turn. A run
  over 4,000 files exceeded ten minutes. Fixed with an ambiguity cap (calls matching more than 6 methods are treated
  as unresolved), per-query memoisation, a limit of 3 interprocedural hops and a work budget of 5,000 steps per
  query; a regression test (`DslTaintBudgetTest`) guards it. A second bug, found earlier by the same benchmark, was
  infinite recursion on `s = s + x`.
- **Receiver-type narrowing** now resolves a call by the receiver's static type where the source states it (a
  parameter, local, field, `this`, `new T()`, a static call, or another call's return type): only methods in that type,
  its project supertypes and its project subtypes remain, and a receiver that is a JDK or library type has no project
  target. The 3,990-file timings stayed in the same range (log-injection 143 ms) and results changed: 80 findings
  became 76, 6 removed and 2 added. The 3 removed ones I read looked like name collisions (for example
  `connection.remote()` matching an unrelated project method). The 2 added ones are plausible, because a call that was
  over the ambiguity cap now resolves, but I did not confirm them. `-Ddsl.nonarrow=true` switches narrowing off for
  comparison; `DslBenchTest` (`-Dbench.dir`, `-Dbench.n`, `-Dbench.out`) reproduces the numbers.
- Not measured: cold-start timings, memory, or the Groovy detector at the lazy-association scale.

### Estimate: reduction in AI use (a model, not a measurement)

There is no review pipeline yet, so the baseline is the design as written: the AI finds everything.

| Metric | Today | After | Reduction |
|---|---|---|---|
| Rules that involve the AI | 261 of 320 (82%) | about 145 (45%) | about 45% |
| AI calls per review | see below | | 30% if batches are 100k characters, 50 to 70% if batched per file |
| AI input tokens | all rule prompts plus all changed code | candidates and triggered rules only | about 80% (65 to 90%) |
| AI cost | | | about 65% (50 to 80%) |

- **The two design documents disagree on batching.** `design.md` section 7 sends batches of up to 100k characters plus
  a final assessment, so a small review is about 2 calls whatever the content. `ai-generated-deterministic-checks.md`
  batches per file and per language, so calls scale with files and a deterministic trigger removes calls outright.
  The call-count reduction is about 30% under the first and 50 to 70% under the second, assuming 25 to 50% of changed
  files still have a trigger. Decide which before building the pipeline.
- **Tokens fall much more than calls.** The 261 `AI prompt` sections total about 21k tokens, so a call carrying all of
  them pays that every time. A call carrying only the rules whose deterministic trigger fired carries about 1 to 2k.
- **The biggest unknown is the trigger rate of the judgement rules** (comments, log wording, test quality). Without a
  deterministic gate they apply to every file and the call reduction shrinks towards 15%.
- Measure it after the pipeline exists: count candidates and triggers per rule and file on real repositories.

### Wiring the restricted language into the rules engine

- The engine moved from test scope to `com.codeclear.rules.dsl` (main). `CCRuleEngine` runs a run's rules over all its
  files and chooses a detector per rule (`.dsl` first by default, Groovy otherwise). `DslRuleDetector` builds the
  project model once per run and returns the same `CCRuleResult` as the Groovy detector, with the same off-diff
  filtering and the rule of lowering confidence but never raising it.
- New language clause `candidate`, the output of a `hybrid` rule: evidence for the AI reviewer to confirm.
- `CCRuleLoader` reads every `.dsl` in a rule folder; `CCRuleDefinition.stale()` checks `.dsl` headers too;
  `CCRuleTestRunner` runs a rule through the engine with either detector, including multi-file cases and `config:`
  lines. `RuleScriptsTest` runs both detectors for every rule that has both.
- `java/lazy-association-outside-session` is now `hybrid` (it was `ai`): its detection is deterministic and only
  judgement about serialiser views and generated code remains. `INDEX.md` counts updated (114 hybrid, 147 AI-only).
- **Defects found by wiring:** "Applies to: any source file" (18 common rules) was never parsed as a glob, so those rules
  could not have run; it now means the code files of every supported language. The lazy-association fact resolved
  entities held in parameters and locals but not fields; it now uses the project model. Both would have been invisible
  without running a rule through the real engine.
- Nothing in the product calls the engine yet, because no review pipeline exists.

### Rule language syntax

- The first syntax read like SQL (`for c: Catch`, `where`, `unless`, `let quiet(s) = ...`, `and`/`or`/`is empty`). Feedback:
  `let name(p) = ...` looks like an assignment but is a function, and a Java or Groovy developer would have to learn
  the language to read a rule. The syntax now follows Groovy: `Catch.each { c -> ... }`, `only` and `skip` for filters,
  `report` and `candidate`, `def name = { p -> ... }` for tests, `&&`, `||`, `!`, `in`, `==~`, `instanceof`,
  `.isEmpty()`, `.every(...)`, `.any { ... }` and `${}` interpolation. See `rule-language.md`.
- Built with a custom parser (14 parser tests). Using Groovy's own parser with a whitelisting evaluator was considered
  and rejected as too heavy. The rule tree is unchanged, so all five rules passed their existing tests unchanged:
  the same 17, 33, 37 and 34 cases.
- The language says what it does not support: `if`, `for`, `while`, `return`, `new` and the like give an error that
  points to `only`, `skip`, `any { }` and `every { }`.
- Whether an AI generates rules correctly in the new syntax has not been re-measured; the earlier experiment used the
  old syntax.

### The review service with no AI backend

- No AI is integrated yet, so the review runs without it. `DummyAiDriver` (id `dummy`, now the default driver) reports
  itself as not ready and returns nothing if called. `CCReviewService` runs every applicable rule's detector through
  `CCRuleEngine`, turns hybrid candidates into findings only when a ready AI driver confirms them, and reports what it
  could not check: skipped rules with a reason (`NEEDS_AI`, `NO_DETECTOR`, `NO_ANALYZER`, `FAILED`), unconfirmed
  candidates and notes (files not analysed, AI unavailable).
- `CCDriverCandidateConfirmer` is the one AI use built: it sends the rule's AI prompt, the evidence and the code around a
  candidate and reads `{"confirmed": true|false}`. It is tested with a fake driver; no real model has been called. Whole-file
  AI review (the `ai`-type rules) is not built and those rules are skipped.
- Mutation testing is out of scope for the review: it is an external tool. See `targeted-mutation-design.md`.
- Against the real rules on a one-file Java change the review runs the 4 detectors that exist, finds the empty catch, and
  lists 35 applicable rules as needing the AI and 60 as having no detector yet (only rules that apply to the changed
  files are listed). Across the whole catalogue 147 rules are AI-only and about 169 executable rules have no detector.

### Command-line review

- `codeclear.sh review [options] [path ...]` (and `codeclear.ps1`) runs `CCReviewService` in the terminal: no server, no
  browser. The jar dispatches on the first argument and starts Spring without a web server; the launchers pass the
  bundled rules folder. Options: `--diff=<rev>` (only changed lines, from `git diff --unified=0 --relative`),
  `--format=text|json`, `--fail-on=<severity>`, `--config=k=v`, `--rules=<dir>`, `--show-skipped`, `--help`.
- The launchers review by default (`codeclear.sh [options] [path ...]`); only `codeclear.sh serve` starts the web
  application and opens the browser. Before this the command needed the word `review`, and without it the launcher started
  Spring and opened a page, which is what people running `codeclear.sh <folder>` hit.
- The command runs without Spring: `CodeClearApplication.main` hands `review` to `CCCliMain` before any Spring class is
  used, so a small folder reviews in about 0.45 s (it was 0.9 s with a Spring context) and 191 files in 1.4 s. The
  launchers pass the rules folder as `--rules=`, ahead of the caller's arguments so an explicit one wins.
- Tried end to end against a small project: two findings in about 0.9 s including startup (before Spring was removed), `--diff=HEAD` reduced it to
  the one changed line, `--fail-on` gave exit codes 1 and 0, a bad option gave 2.
- Not built: reading project configuration (`application.yml`, logback) into facts automatically, so cross-file rules rely
  on `--config` for now; non-Java languages; packaging the rules folder with the jar.

### Five more detectors (reference-equality-on-values, interrupt-flag-not-restored, exception-cause-dropped, shared-simpledateformat, return-or-throw-in-finally)

- Picked as the easiest executable Java rules without a detector, in that order. Each is a `rule.dsl` with a `tests.md` and
  passes the committed tests (12 to 17 cases each). 9 detectors now exist; 164 executable rules still have none.
- **New language surface was small:** seven node kinds (`Binary`, `StringLit`, `New`, `Throw`, `FieldDecl`, `Return`,
  `Break`, `Continue`) plus the pseudo-kind `Jump`, and about 20 fields and facts in `DslSyntaxFacts`
  (`escapesFinally`, `carries`, `enclosingCatch`, `callsAfter`, `causeUnsupported`, ...). No new syntax. See
  `rule-language.md`.
- **Mutation testing found real gaps in the first drafts** (untested string concatenation, a sentinel on one side only,
  an `Object` operand, an uninitialised declared field, three near-identical reports that only differed in text, which
  I merged). All five now have no surviving mutants except three redundant null-skips in `reference-equality`.
- **Running the rules on real code found what the tests did not.** Over 62 of the projects under `~/git` (all nine
  detectors, no rule failures) the first drafts produced false positives that the hand-written cases never covered:
  - `e.getMessage()` counted as passing the cause (replaced `mentions` with `carries`, which ignores message-only reads);
  - a cause passed through a local variable (`Throwable cause = e.getCause() ...; throw new X(cause)`);
  - a JDK exception such as `TimeoutException` that has no constructor taking a cause (now checked by reflection);
  - `interrupted = true` in the catch with the flag restored after the loop, a very common idiom;
  - `(this.a == rhs.a) || (this.a != null && this.a.equals(rhs.a))` inside generated `equals` methods, 11 findings.
  The `rule.md` text of `interrupt-flag-not-restored`, `exception-cause-dropped` and `reference-equality-on-values` was
  extended to say so, so the text, the detector and the tests agree.
- **Sweep totals after the fixes (62 projects):** log-injection 224, exception-cause-dropped 113, interrupt-flag-not-restored
  111, optional-get-without-check 76, empty-catch-block 47, reference-equality-on-values 35, shared-simpledateformat 16,
  lazy-association-outside-session 10, return-or-throw-in-finally 6. Samples of the new rules were read by hand: the
  `interrupt` and `cause` samples were all genuine; most of the `return-or-throw-in-finally` hits are demo code that shows
  the pitfall on purpose; many `reference-equality` hits are tutorial tests that show `==` on purpose.
- **Lesson:** hand-written cases test what the author thought of. Running on real code is the only check for what the author
  did not. It cost minutes and found five false-positive patterns. Worth building into the process (R19).
- Launcher fix: `codeclear.sh` and `codeclear.ps1` read the Java version from line 1 of `java -version`, which fails when
  `JAVA_TOOL_OPTIONS` is set (java prints `Picked up JAVA_TOOL_OPTIONS: ...` first). They now take the first line that
  contains `version "`; a test runs the bash launcher against a stub `java` to keep it fixed.

### Reading the Java the projects are written in

- A reviewer reported `not analysed: cannot parse ...: "var" is not allowed here` for a file that compiles on Java 25.
  Cause: JavaParser 3.26.2 validates up to Java 21 and rejects `var` inside record patterns (`case P(var x, var y)`,
  `o instanceof P(var x, var y)`), and also `_` (unnamed variables, final in Java 22). Java 25 syntax (module imports,
  flexible constructor bodies) needs the `JAVA_25` language level, which the old parser did not have.
- Fix: JavaParser 3.28.2 with the `JAVA_25` language level, and a second parse with no validation when the validator rejects
  a file the grammar accepts, so a validator quirk never hides a whole file. Parse errors now name the line
  (`cannot parse A.java:3: ...`).
- **Version matters.** 3.28.0 has a `JAVA_25` level that breaks `yield` in switch expressions: upgrading to it fixed 6 files
  and broke 12 others, which a re-run of the sweep showed. 3.28.2 fixes that. The analyzer tests keep `yield`, record
  patterns, unnamed variables, module imports and flexible constructors parsing.
- Across the 62 projects, files not analysed fell from 18 to 12. All 12 are invalid Java (templates with `${package}`,
  Java 17 preview pattern syntax, string templates, a deliberately broken test resource, a stray brace, stray text), not
  parser limits. Findings did not change, so none of the unparsed files were hiding detector results.

### Five more detectors (leftover-debug-output, bigdecimal-misuse, log-message-without-stacktrace, double-checked-locking-without-volatile, lock-not-released-in-finally)

- Detectors now number 14; 159 executable rules still have none. `bigdecimal-misuse` has two files (`rule.dsl` for the
  constructor, `equals.dsl` for `.equals`). New language surface: kinds `DoubleLit` and `If`, a few fields, and three lock
  facts (`releasedInNextFinally`, `releasedInEnclosingFinally`, `releasedElsewhere`), plus `doubleCheckedField`.
- **Mutation testing** left no unexplained survivors. It found redundant clauses (removed) and one real test gap (`warn` on a
  receiver that is not a logger). `bigdecimal-misuse` keeps one deliberate redundancy: the double-literal alternative is the
  syntactic safety net the rule text asks for, and the resolved type covers it when types resolve.
- **Real code again found what hand-written cases did not.** Over the 62 projects the first drafts had these problems, each
  fixed, tested and written into the `rule.md`:
  - `leftover-debug-output`: 1,902 findings, 767 of them helper methods of command-line programs that have a `main` method
    (the rule only exempted `main` itself). The exemption now covers any method of a class that has `main`.
  - `lock-not-released-in-finally`: `try { lock.lock(); ... } finally { lock.unlock(); }` was reported with a message that
    said the lock was not released in a finally. It is released; acquiring before the `try` is only the safer form. The
    acquire-inside-try variant is now exempt. A second acquire/release method pair in the same class also made the "split across
    methods" LOW rule misfire; a release only counts as split when its method does not acquire the lock itself.
  - `log-message-without-stacktrace`: any use of the exception triggered it, so `ex.getClass().getSimpleName()` was flagged.
    The trigger is now exactly `getMessage()`, `getLocalizedMessage()` and `toString()`.
  - `Applies to: any source file outside tests` was not understood by the loader, so the rule could never have run.
- **Sweep totals now:** leftover-debug-output 1,135 (about 1,050 in the `tutorials` repo, which is demo code), log-injection 224,
  exception-cause-dropped 113, interrupt-flag-not-restored 111, optional-get-without-check 76, empty-catch-block 47,
  reference-equality-on-values 35, log-message-without-stacktrace 31, bigdecimal-misuse 29, lock-not-released-in-finally 25
  (7 HIGH, all real: demo code that acquires with no `finally`; 18 LOW from name guesses and lock wrappers),
  shared-simpledateformat 16, lazy-association-outside-session 10, return-or-throw-in-finally 6,
  double-checked-locking-without-volatile 0.
- **Zero findings for double-checked locking is correct, not a gap:** the corpus has 5 real double-checked-locking classes
  and all declare the field `volatile`. The pattern is covered by 10 test cases.

### Markdown report

- Every review writes `target/codeclear/report.md` inside the scanned folder (`--report=<file>` to move it, `--report=none` to skip it): a summary table,
  findings grouped by file with a numbered snippet (3 lines of context, the flagged line marked `>`), unconfirmed candidates,
  the rules not checked in collapsible lists, and notes. The snippet fence grows when the code itself contains backticks, and
  messages are escaped so a `|` or `<` cannot break the tables. A report that cannot be written is a warning, not a failure.
- With `--format=json` the "Report:" notice goes to stderr so stdout stays pure JSON.

### Five more detectors (unowned-todo, log-placeholder-mismatch, generic-exception-in-public-api, catch-log-and-continue, collection-modified-during-iteration)

- Detectors now number 19; 154 executable rules still have none. `generic-exception-in-public-api` has two files (`rule.dsl`
  for `throws Exception`, `throw.dsl` for `throw new RuntimeException(...)`). Two of the five are `hybrid`, so they report
  **candidates** for the AI (`candidate`), not findings: the test format gained `// CANDIDATE` for that. It also gained
  `changed: 3,5-7`, which lets a case say which lines the change touches, so "a marker that already existed, outside the
  change" is a tested behaviour and not just an engine property.
- New language surface: the `Comment` kind (comments hang off the tree, so a walk of the nodes misses them), and facts for
  logging (`placeholders`, `logArguments`, `isThrowable`), iteration (`enclosingIterations`, `iteratedCollection`,
  `iteratedType`, `followedByExit`, `updatesCurrentKey`), methods (`isPublic`, `throwsTypes`, `annotations`,
  `enclosingMethod`) and `tryCalls`.
- Mutation testing again exposed missing tests (non-mutating calls in a loop, a catch outside any method, a `call()`
  boundary, a try block with no calls, a constructor that throws) and one redundant clause.
- **Running on real code found six more false-positive patterns, each fixed, tested and written into the `rule.md`:**
  - `log-placeholder-mismatch` (20 findings to 7, all 7 real): `{0}` / `{0.00}` formats of `java.util.logging`, `MessageFormat` and
    Tinylog; a single `new Object[]{...}`, which SLF4J takes as the whole argument array; and a caught exception whose class
    cannot be resolved (`uae`) or a variable of a type bounded by an exception (`E extends Exception`).
  - `collection-modified-during-iteration` (12 to 4): `put` of the key being visited only replaces a value, and a variable
    declared as `Map` but created as `new ConcurrentHashMap<>()` is concurrent. The type of `cache.keySet()` is now the type
    of `cache`.
  - `generic-exception-in-public-api` (567 candidates to 362): signatures the framework dictates, such as Spring's
    `@Bean SecurityFilterChain filterChain(HttpSecurity) throws Exception`, `@Around` advice and lifecycle callbacks.
- Sweep totals: unowned-todo 524, catch-log-and-continue 349 candidates, generic-exception-in-public-api 362 candidates,
  log-placeholder-mismatch 7, collection-modified-during-iteration 4. The remaining placeholder findings are all real; the
  remaining collection findings are demo code that shows the bug.

### Five more detectors (log-and-rethrow, kafka-producer-send-unobserved, unbounded-executor-queue, test-without-meaningful-assertion, transactional-on-private-method)

- Detectors now number 24; 149 executable rules still have none. Three are `hybrid` (candidates for the AI), two are
  deterministic. `unbounded-executor-queue` and `transactional-on-private-method` have two files each (factory calls and
  constructors; private/final methods and self-calls).
- New language surface: the `Lambda` and `MethodRef` kinds, and the facts `isDiscarded`, `enclosingClassAnnotations`,
  `isPrivate`, `isFinal`, `assertions` (the assertion calls of a test, with `assertThat(x).isNotNull()` reported as the weak
  call it is), `sameClassTransactionalTarget` and `hasAnnotationMember`.
- **The mutation harness now compares confidence.** It ignored it before, so a dropped `confidence: MEDIUM` survived as an
  equivalent mutant; `DslProjectTest` now treats an unmarked `// FLAG` as `HIGH`, as the engine test does.
- **Path globs were anchored to the repository root.** `src/test/**` never matched `clipper/src/test/A.java` when a review
  was started from the folder above a project, so `test-without-meaningful-assertion` found nothing in 62 projects. A glob
  now also matches with any number of leading folders removed (`CCRuleAppliesToTest`).
- **Real code found one false-positive pattern:** tests that delegate to a helper of their own class that asserts
  (`testAdding(1, 2)`, `parse("...")`), whatever the helper is called, with varargs helpers. The detector now follows such
  calls two hops. Findings on 62 projects went from 56 to 22, and the rest are tests that really assert nothing
  (`parse(...)` smoke tests, benchmarks).
- Sweep totals: log-and-rethrow 39 candidates, kafka-producer-send-unobserved 23, unbounded-executor-queue 71,
  transactional-on-private-method 17 findings.
- The Markdown report is now written under the folder that was scanned (`<folder>/target/codeclear/report.md`), not the
  working directory.

### Model choice (prices from the API reference cached 2026-11-03; token volumes are assumptions)

- Two different jobs. **Rule generation and regeneration:** Sonnet 5.5 first, escalate to Opus 5.5 when the gate
  fails (the gate is a machine-checkable signal, so a cascade is safe), Fable 5.2 only for repeat failures; use the
  Batch API. About $55 (Sonnet 5.5) to $110 (Opus 5.5) to generate all 172 scripts at 2 attempts each.
- **Review-time calls:** Sonnet 5.5 default; test Haiku 5.5 on the hybrid "confirm or drop this candidate"
  prompts. Do not build a cascade before measuring one model at lower effort: a review verdict has no automatic
  check and caches are per model.
- The 148 `ai` rules carry 548 `Evaluation cases`, a ready-made eval set for choosing the review model.
- `ai.driver` defaults to `copilot`, which picks its own model; per-purpose models need the Anthropic driver.
  `CCAiRequest` already has an optional `model` field.
- For developing this project itself: Sonnet 5.5 by default, Opus 5.5 for the sandbox, language design and flow
  analysis, Haiku 5.5 for mechanical bulk edits.

## Recommendations

| # | Recommendation | Why |
|---|---|---|
| R1 | Build the engine around a project-wide `CodeModel` and read-only `ast`, then convert context-lookup `hybrid` rules to deterministic ones. | Removes most model calls and makes results reproducible. |
| R2 | Do Java first. Treat Python and TypeScript as a later phase. | 85 Java rules, matches the stack, and tree-sitter native libraries conflict with the prebuilt-JAR, native-Windows distribution. |
| R3 | Keep `Detection type` values as they are (`ast`, `regex`, `linter`, `hybrid`, `ai`) and treat them as "mechanism plus whether AI confirms". | Avoids editing 320 files. Every executable rule runs on the Groovy engine regardless. |
| R4 | Make the sandbox and acceptance gate the first things built, before more rules. | Scripts generated at review time run unreviewed on a developer machine, so these are the only controls. |
| R5 | Treat every generated script as untrusted. Never load scripts from the reviewed repository. | A sandbox escape would be code execution on the developer's machine. |
| R6 | Review rule files for precision before generating from them, and fix `optional-get-without-check`'s guard list. | The generator follows the text; a wrong rule produces a faithfully wrong script. |
| R7 | Pilot the context-heavy rules next: `lock-ordering-deadlock`, `lazy-association-outside-session`, `lost-update-read-modify-write`, `missing-endpoint-authorization`, `remote-call-in-transaction`, `broad-catch-swallowed`. | They exercise the call graph, entity and config parts of `CodeModel`, which neither finished script touches. |
| R8 | Replace the name heuristic for loggers in `empty-catch-block` (`log`, `logger`, `LOGGER`) with a resolved-type check once `model.typeOf` is available there. | `auditLog` and the fluent `atDebug()` API are missed today. |
| R9 | Let teams commit and pin the script store (`codeclear rules export`). | Different machines can generate different scripts for the same rule, so findings can differ between users. |
| R10 | Put the rule-file edit, script regeneration and test run behind one command (`codeclear rules regen`) and a read-only `codeclear rules check`. | Gives CI a way to fail on stale scripts without calling the AI. |
| R11 | Make the gate require a test for every "Do not flag" bullet, and generate mutants automatically. | A wrong exclusion passed every existing test in the experiment. Implemented as a label-count check in `RuleScriptsTest`; automatic mutation exists only for the restricted-language experiment. |
| R12 | Write rule text precisely enough to implement: name the heuristic lists, and state what a guard is. | Every first-shot generator miss traced to vague prose. |
| R13 | On gate failure, retry the generator with the failing case's source, expected and actual result. | Both failing generators fixed themselves in one round. |
| R14 | If a restricted language is adopted, put computation in hand-written host facts (`guarded`, types, taint, framework classification) and keep the language itself tiny. | Rules then stay short, and the dangerous analysis is reviewed Java rather than generated code. |
| R15 | Pass policy to host facts as predicates: the rule says what is a source and what is a sanitiser, the host walks the flow. | One reviewed taint engine serves every taint rule, and a generated rule cannot change how flow is computed. |
| R16 | Only run mutation testing against a passing baseline. | A failing case makes every mutant look killed. |
| R18 | Bound every analysis: cap name-and-arity ambiguity, memoise per query, limit interprocedural hops and give each query a work budget. | An unbounded taint search ran for over ten minutes on a 4,000-file project. Real symbol resolution will make calls exact, but the budget stays as a safety net. |
| R19 | Before accepting a new detector, run it over a corpus of real code and read a sample of its findings. | The hand-written cases passed while five false-positive patterns were present; only real code showed them. |
| R17 | Cover test-quality rules with mutation testing, as an external tool run by developers or CI (generic mutants for "tests omit the risk", rule-named targeted mutants for "mock-only tests cannot prove the guarantee"). The review tool does not run it. | Deterministic, and it does not depend on reading the merge request's prose. It executes the project's tests, so it stays outside the read-only review. |

## Open questions

| # | Question | Notes |
|---|---|---|
| Q1 | Native or normalized AST for Python and TypeScript? | Native keeps detail but each language needs its own node API in the generator prompt. Normalized is portable but loses detail. Leaning native for Java and deciding later for the rest. |
| Q2 | How does tree-sitter ship in the prebuilt JAR on native Windows? | Needs native libraries per OS. Unresolved. |
| Q3 | How does the tool get the project classpath for symbol resolution? | Needs Maven or Gradle resolution. Without it, rules fall back to names at `LOW` confidence. Unresolved. |
| Q4 | Who reviews scripts generated at review time? | Currently nobody. Release builds regenerate and commit all scripts through a reviewed pull request, so the bundled baseline is reviewed; the user store is not. |
| Q5 | Is the acceptance gate strong enough without a human? | Gate: sandbox compile, tests covering every example and "do not flag" bullet, determinism, time budget, mutation check, previous tests. Whether weak generated tests can slip a wrong script through is unproven. |
| Q6 | Should the rollout order change to build the sandbox and gate first? | The doc's Rollout section still reads as if only maintainers generate. Not yet updated. |
| Q7 | Should `Detection type` collapse to a single `script` type later? | Possible bulk edit plus regenerating `INDEX.md`. |
| Q8 | Groovy or a restricted rule language? | Both pass the same tests on the two Groovy rules; the language also expressed a tier 2 rule (lazy associations) and a taint rule (log injection) as 12 to 25 line rules over host facts. It removes the sandbox problem and enables automatic mutation testing; it costs the language plus every fact family in reviewed Java. Leaning: adopt the language. Not yet measured: speed, real RE2/J, project-wide engine, symbol resolution (Q3). |
| Q9 | Can the language express a tier 2 rule such as `lazy-association-outside-session`? | Yes, with a project model and call-graph facts (see Discoveries). Open: replacing name-and-arity matching with real symbol resolution (Q3), and a project-wide engine instead of per-file. |
| Q10 | Which model does review-time work? | Run the 548 `Evaluation cases` across models and compare accuracy, false-positive rate and cost per completed review. |
| Q11 | How do the external mutation tool's results reach a review? | The review skips the seven test-quality rules (listed as needing the AI). Mutation testing is an external tool, so the review has no `--mutation` option. Open: whether the review should read the tool's report. |
| Q12 | Are AI calls batched by size (`design.md`, 100k characters) or per file and language (`ai-generated-deterministic-checks.md`)? | The two documents disagree. Per file makes deterministic triggers remove calls outright; by size does not. Decide before building the pipeline. |

## Log

### 2026-11-20

- Analysed all rule files, concentrating on `ai` and `hybrid`. Found three jobs the AI is doing; only
  one needs a model (see Discoveries).
- Decided on scripts via JSR-223 (D1), Groovy (D2), engine-supplied AST (D3), regeneration on rule
  change (D4) and at review time (D5).
- Updated `docs/ai-generated-deterministic-checks.md`: Script Engine, script contract, `CodeModel`
  facade, sandbox, generation workflow with staleness hash, script store, acceptance gate and fallback.
  Committed and pushed (`36df946`).
- Wrote `review/java/empty-catch-block/rule.groovy` and its tests (16 cases plus a mutation check). All pass in a
  throwaway harness. Committed and pushed.
- Wrote `review/java/optional-get-without-check/rule.groovy` and its tests (30 cases plus 3 mutation checks). All
  pass; three deliberate script mutants failed the expected tests. Added `model.typeOf` and the
  confidence argument to the doc. **Not yet committed.**
- Created this file.
- Moved every rule into its own folder (`rule.md`, `rule.groovy`, `tests.md`) and built the real
  `GroovyRuleDetector`, sandbox, loader and test runner under `com.codeclear.rules`.
- Analysed the Distill idea, classified the 172 executable rules into tiers, and answered the model-choice question
  (see Discoveries).
- Built a throwaway restricted rule language under `src/test`, ran both rules through it, and measured AI
  generation of the language (see Discoveries). Fixed `optional-get-without-check/rule.md` (R6), added test cases
  and a gate check for "Do not flag" coverage.

### Next candidates

- Write more detectors (about 169 executable rules have none), starting with the simplest, and measure candidates and triggers per rule on real code.
- Decide the AI batching (Q12) before building whole-file AI review.
- Decide Q8 (leaning to the restricted language, now wired into the engine). Open work either way: a project-wide engine, symbol resolution, and config parsing facts.
- Measure the language's speed against the Groovy detector on a real project (speed is not a priority; it should be reasonable).
- Build the mutant generator from `targeted-mutation-design.md` (no test execution needed for the first step).
- Update the design doc's Rollout section (Q6) and the regex-timeout gap in the sandbox.
- Run the model comparison on the `Evaluation cases` (Q10).
