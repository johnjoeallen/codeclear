# Rule Language Reference

The rule language is how a deterministic check is written when it does not need a general-purpose script. A rule is a
`rule.dsl` file in the rule's folder. The syntax follows Groovy so a developer can read it, but it is a small language
with its own parser: the text is never executed as Groovy, and there are no loops, conditionals, assignments or calls to
arbitrary code. A rule can only read the fields and call the facts listed here, so it needs no sandbox.

## Table of Contents

- [A complete rule](#a-complete-rule)
- [Structure](#structure)
- [Expressions](#expressions)
- [Node kinds and fields](#node-kinds-and-fields)
- [Host facts](#host-facts)
- [What is deliberately missing](#what-is-deliberately-missing)

## A complete rule

```groovy
// generated-from: review/java/empty-catch-block/rule.md
// rule-hash: f048993f...
// api-version: 1
rule 'empty-catch-block'

// A statement that is only a DEBUG/TRACE log call.
def quiet = { s ->
    s instanceof ExprStmt
        && s.expression instanceof Call
        && s.expression.name in ['debug', 'trace']
        && s.expression.scope.text ==~ /(?i)(this\.)?log(ger)?/
}

Catch.each { c ->
    skip file.isTest || c.param.name in ['ignored', 'ignore', 'expected'] || c.body.hasComment

    report c, "Empty catch block hides ${c.param.type} from the caller",
        c.body.statements.isEmpty()
    report c, "Catch block only logs at DEBUG/TRACE, hiding ${c.param.type} from the caller",
        c.body.statements.every(quiet)
}
```

## Structure

| Part | Meaning |
|---|---|
| `rule 'name'` | The rule's name |
| `def name = expression` | A value computed lazily and remembered, usable by the rest of the rule |
| `def name = { p -> expression }` | A named test on one value, used with `every`, `any` or called as `name(x)` |
| `Kind.each { v -> ... }` | Run the body once for every node of that kind in the file, with `v` bound to it. One per rule |
| `only expression` | Keep the node only if true |
| `skip expression` | Drop the node if true |
| `report node, "message"` | A finding on the node's line, with an optional condition and `confidence:` |
| `candidate node, "evidence"` | Evidence for the AI reviewer to confirm, instead of a finding (a `hybrid` rule) |

- **Order.** `only`, `skip` and `def` run top to bottom and must come before the first `report`. Within one node the
  first `report` whose condition holds fires, so a node has at most one finding.
- **Report form.** `report <node>, "<message>"` followed by an optional condition and an optional
  `confidence: LOW | MEDIUM | HIGH | DEFAULT`, for example
  `report c.nameNode, "...", resolved, confidence: resolved ? DEFAULT : LOW`. A rule can lower its own confidence but
  never raise it above the one in `rule.md`.
- **Messages.** A double-quoted message interpolates `${expression}`. A single-quoted string does not.
- **Layout does not matter.** Keywords start statements and an expression continues over line breaks. Comments are
  `// ...`, `/* ... */` and `# ...`.

## Expressions

Loosest to tightest: `a ? b : c`, `||`, `&&`, `!`, then one comparison:

| Form | Meaning |
|---|---|
| `x in ['a', 'b']` | x is one of the listed values (false if x is missing) |
| `x ==~ /regex/` | x is a string that matches the whole regex |
| `x instanceof Kind` | x is a node of that kind (false if missing) |
| `x == y`, `x != y` | equality; `x != null` and `x == null` test for presence |
| `xs.isEmpty()` | list or string is empty (a missing value counts as empty) |
| `xs.every(p)`, `xs.any(p)` | every or some element satisfies the test `p`; also `xs.any { a -> ... }` |
| `x.inside(Loop)` | the node has an ancestor of that kind |

Field access is `x.field`, and `x?.field` is the same. Reading a field of a missing value gives a missing value. A
field that exists for another kind of node also gives a missing value; a name that is no field of any kind is an
error. Constants: `LOW`, `MEDIUM`, `HIGH`, `DEFAULT`, `PRESENT`. Literals: `'string'`, `"string"`, `/regex/` (only after
`==~`), `[list, of, values]`, `true`, `false`, `null`. `every` over an empty list is true.

## Node kinds and fields

Kinds: `Catch`, `Call`, `Name`, `Field`, `Param`, `ExprStmt`, `Method`. `Loop` can be used in `instanceof` and
`inside`.

| Node | Fields |
|---|---|
| any node | `text`, `inTransaction` |
| `file` (always available) | `path`, `language`, `isTest` |
| `Catch` | `param`, `body` |
| `Param` | `name`, `type` (text), `annotations` (list of names) |
| a block (`.body`) | `statements`, `hasComment` |
| `ExprStmt` | `expression` |
| `Call` | `name`, `nameNode`, `args`, `firstArg`, `scope`, `lazyAssociation`, `dereferenced` |
| `Name` | `name`, `declaredType` |
| `Field` | `name`, `scope` |
| any expression | `type` (qualified name, missing when unresolved), `fetched` |
| `Method` | `name`, `nameNode`, `inController`, `returnedEntity`, `lazyAssociations`, `returnsFetched` |
| `project` (when the run has a project) | `project.config('key')` |

A statement is not an expression: `log.debug("x");` is an `ExprStmt` whose `expression` is a `Call`.

## Host facts

Facts are reviewed Java that does the work a rule should not express itself. They are called like methods.

| Fact | Meaning |
|---|---|
| `call.guarded(expr, PRESENT)` | A dominating condition or early exit proves the Optional variable `expr` is present at `call` |
| `expr.tainted(isSource, isSanitized)` | The value may derive from a node satisfying `isSource` without passing through one satisfying `isSanitized`. Follows locals, concatenation, pass-through string methods, parameters through callers and return values through callees |
| `expr.fetched` | The entity was produced by a join-fetch query or entity graph, possibly several calls away |
| `node.inTransaction` | The node runs inside a transaction, directly or because every caller does |
| `project.config('key')` | A project configuration value, for example `spring.jpa.open-in-view` |

Calls are resolved by the receiver's static type where the source states it, and otherwise by name and argument count.
Interprocedural facts are bounded: a limited number of calls are followed and a work budget ends a query early, so an
unusually deep flow is reported as not proven rather than slowing the review.

## What is deliberately missing

There are no `if`, `for`, `while`, `return`, `new`, `class`, `import` or assignment statements, and no way to define
a recursive test. Using one gives an error that says so and points to `only`, `skip`, `any { }` and `every { }`. Keeping
the language this small is what makes a rule safe to run without a sandbox, certain to finish, and cheap to test: the
test tooling can automatically delete or invert each clause of a rule to check that the rule's tests would notice.
