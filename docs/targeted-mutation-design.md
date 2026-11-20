# Targeted Mutation Testing for Test-Quality Rules

Status: design only, nothing built. First worked example: `java/mocked-test-cannot-prove-transaction-guarantee`.

## Table of Contents

- [Goal](#goal)
- [Where it runs](#where-it-runs)
- [Idea](#idea)
- [Worked example](#worked-example)
- [Mutants for transactional guarantees](#mutants-for-transactional-guarantees)
- [What the result means](#what-the-result-means)
- [Pipeline](#pipeline)
- [Rule syntax sketch](#rule-syntax-sketch)
- [What still needs the AI](#what-still-needs-the-ai)
- [Costs and risks](#costs-and-risks)
- [Build order](#build-order)

## Goal

Replace the AI judgement in the test-quality rules with an execution-based check. 7 of the 8 test-quality rules
(`risky-change-without-tests`, `changed-code-without-test`, `test-risk-coverage`, `tests-cover-risk-paths`, and the
three mock-only guarantee rules) ask the same question: do the tests fail when the behaviour they should protect is
broken? Mutation testing answers that directly.

## Where it runs

A code review does not run mutation testing. It runs where the project's tests already run, on a developer's
machine or in CI, in an environment that is already trusted to execute that code. The review tool offers it as an
option that is part of an overall review, off by default:

```
codeclear review                 reads code only; fast; no test execution
codeclear review --mutation      also runs mutation testing on the changed code (developer machine or CI)
```

- **Off (default):** the seven test-quality rules are not evaluated. The report lists them as "not checked, needs
  `--mutation`" so the gap is visible. Whether they instead fall back to the AI judgement is an open decision.
- **On:** the tool detects constructs and changed lines as usual, generates mutants, runs the mapped tests in a
  scratch copy of the workspace, and adds survivors to the report as ordinary findings.
- Because it runs inside the trust boundary where tests already execute, it adds no new exposure. The tool never
  runs the reviewed project's tests in a context that did not already run them.

## Idea

Two kinds of mutants, applied only to changed code:

- **Generic mutants** (PIT, Stryker, mutmut defaults): negate a condition, drop a call, change a constant. A surviving
  mutant is behaviour the tests do not check. This covers "tests omit the risks of the change".
- **Targeted mutants**, named by a rule: the inverse of the fix a reliability rule asks for. If a rule says "publish
  after commit", its mutant publishes before commit. A test suite that claims the guarantee must fail on that mutant.
  This covers "mock-only tests offered as proof of a guarantee".

Because the code that implements a guarantee can be recognised deterministically, the check no longer needs the
merge request's prose claim as its trigger.

## Worked example

`java/mocked-test-cannot-prove-transaction-guarantee` today asks the AI to link an MR claim to what the tests can
show. As a mutation rule:

1. **Detect a guarantee construct in changed code** (deterministic, from the rule language and host facts).
2. **Generate its targeted mutants** (below).
3. **Run the tests that cover that code** against each mutant, in an isolated copy of the workspace.
4. **Report each surviving mutant**: the guarantee is not proven.

Why a mock-only test survives: a Mockito mock of `KafkaTemplate` or `TransactionTemplate` never runs Spring's
transaction proxy, so removing `@Transactional` changes nothing it can observe. A test with a real transaction
manager and a broker (Testcontainers, embedded Kafka) that rolls back and asserts nothing was published fails on the
same mutant, so the mutant is killed.

The rule's three evaluation cases map as follows:

| Evaluation case | Outcome |
|---|---|
| 1. Claim of exactly-once, Mockito tests only | Mutants survive: reported |
| 2. Claim backed by a Testcontainers rollback test | Mutants killed: no report |
| 3. No guarantee claim | No guarantee construct, no mutants: no report |

## Mutants for transactional guarantees

| Guarantee construct (detected) | Targeted mutant | A test proving the guarantee must |
|---|---|---|
| `@TransactionalEventListener` (after commit) publishing an event | Replace with `@EventListener`, or set the phase to `BEFORE_COMMIT` | Fail when the publish happens before commit |
| Publish in a transaction synchronisation `afterCommit` | Run the publish inline, before the business write | Fail when the business write rolls back after the publish |
| Outbox row written in the same `@Transactional` method as the business write | Delete the outbox write; or give it its own transaction (`REQUIRES_NEW`) | Fail when the outbox row is missing or survives a rollback |
| `@Transactional` with `rollbackFor` or a thrown exception that must roll back | Remove `@Transactional`; drop `rollbackFor` | Fail when state persists after the failure |
| Ack or commit placed after the durable write (Kafka, Rabbit, SQS) | Swap the order: ack before the write | Fail when a crash between them loses the message |
| Publisher confirms or a retry bound | Remove the confirm wait; remove the bound | Fail when the broker rejects or the retry loops |

Each mutant has an applicability check, so it is only generated where it can change behaviour (the annotation exists,
the call exists in that order).

## What the result means

- **Killed:** some test fails on the mutant. The guarantee has evidence.
- **Survived, covering tests exist:** the tests run the code but cannot tell. Report with the mutant's description
  and the covering test names. For the mock-only rules this is the finding.
- **Survived, no covering test:** report as missing tests (`changed-code-without-test`).
- **A surviving "remove `@Transactional`" mutant on a real-database test is itself a finding:** the annotation is
  ineffective, typically self-invocation or a non-public method.

## Pipeline

```
changed files
  -> deterministic detectors find guarantee constructs and risky changed lines
  -> mutant generator rewrites the AST (lexical-preserving) into a temp workspace copy
  -> test mapper selects tests that cover the changed class (coverage or naming)
  -> build tool runs those tests per mutant
  -> report survivors with mutant description, covering tests, and the rule's severity
```

- Mutants are derived from the same rules that find the construct, so adding a reliability rule adds its mutants.
- Results are cached by hash of (mutant, covering test sources), so unchanged code is not re-run.
- A time budget and mutant cap per review keep it bounded; hitting the cap is reported, not hidden.

## Rule syntax sketch

Not implemented. A rule would declare the construct and its mutants next to its flags:

```
rule transactional-outbox-write
for    m: Method
where  m has annotation Transactional
   and m contains Call c where c.name == "save" and c.scope.type is "OutboxRepository"

mutant "delete-outbox-write"   delete c
mutant "outbox-own-transaction" annotate c.target with Transactional(propagation = REQUIRES_NEW)
mutant "remove-transactional"   remove annotation Transactional from m

flag survivors "Outbox write is not proven atomic with the business write: mutant '{mutant}' survived"
```

`delete`, `annotate`, `remove annotation`, `swap` and `replace annotation` are the whole mutation vocabulary needed so
far. Mutants are data, so the existing automatic mutator and mutation-testing discipline for the rules themselves
applies here too.

## What still needs the AI

- **A claim with no recognisable construct.** The MR says "exactly-once" but the changed code has none of the
  constructs above. That comparison of prose to code stays with the AI, as a small residual.
- **Equivalent mutants.** A survivor that cannot change behaviour (for example the default propagation already equals
  the mutated value). Applicability checks remove most; the rest need triage, a small AI use or a threshold.
- **`mock-verifies-implementation-details`.** Mutants do not catch tests that break on a behaviour-preserving
  refactor. A structural ratio (verifications of collaborators to assertions on results) approximates it; it probably
  stays partly AI.

## Costs and risks

- **Executes the project's tests.** Needs a working build, dependencies, and often a database or broker. Everything
  else in the engine only reads code.
- **It executes the project's tests,** so it only runs where that is already accepted: a developer's checkout or a
  CI job. It is an explicit option, never part of the default read-only review.
- **Slow.** Minutes, not milliseconds, even limited to changed lines. That is acceptable because it is opt-in.
- **Per-language tooling.** PIT (Java), Stryker (TypeScript), mutmut or similar (Python), with different mutator
  sets. Targeted mutants are AST rewrites the engine does itself, so only running the tests is tool-specific.
- **Flaky tests** turn into false survivors or false kills; a mutant run should repeat a failing baseline check first.

## Build order

1. **Mutant generator.** JavaParser rewrite of the five mutation operators with applicability checks and unit tests
   on snippets. Needs no test execution, so it can be built and verified on its own.
2. **Runner.** Copy the workspace, apply one mutant, run mapped tests with Maven or Gradle in a temp directory,
   collect killed or survived.
3. **Wire to rules.** Detectors supply constructs; survivors become findings.
4. **Evaluate** on the rule's three evaluation cases using real Mockito and Testcontainers fixtures.
