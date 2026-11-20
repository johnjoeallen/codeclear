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

Code under `src/test/java/com/codeclear/rules/dsl/`, rules under `src/test/resources/dsl/`.

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
| R17 | Cover test-quality rules with mutation testing as an opt-in option of the review tool (`--mutation`), run on the developer side or in CI: generic mutants for "tests omit the risk", rule-named targeted mutants for "mock-only tests cannot prove the guarantee". | Deterministic, and it does not depend on reading the merge request's prose. The tool only runs tests where they already run, so there is no new trust boundary. |

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
| Q11 | With `--mutation` off, are the seven test-quality rules skipped, or does the AI judge them? | Leaning: skipped and listed as "not checked, needs --mutation", so the default review stays read-only and cheap. Decide when the option is built. |

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

- Decide Q8 (leaning to the restricted language). Open work either way: a project-wide engine, symbol resolution, and config parsing facts.
- Measure the language's speed against the Groovy detector on a real project (speed is not a priority; it should be reasonable).
- Build the mutant generator from `targeted-mutation-design.md` (no test execution needed for the first step).
- Update the design doc's Rollout section (Q6) and the regex-timeout gap in the sandbox.
- Run the model comparison on the `Evaluation cases` (Q10).
