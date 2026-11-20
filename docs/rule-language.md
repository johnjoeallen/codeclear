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
- [SQL files](#sql-files)
- [Config files](#config-files)
- [Python and TypeScript files](#python-and-typescript-files)
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
| `xs.size()` | the number of elements of a list, or characters of a string (a missing value is 0) |
| `n.isEqualTo(m)`, `n.isNotEqualTo(m)` (also `isNotEqual`), `n.isGreaterThan(m)`, `n.isGreaterThanOrEqualTo(m)`, `n.isLessThan(m)`, `n.isLessThanOrEqualTo(m)` | compare two numbers, for example `c.args.size().isGreaterThan(1)`. False when either side is missing or not a number |
| `xs.every(p)`, `xs.any(p)` | every or some element satisfies the test `p`; also `xs.any { a -> ... }` |
| `x.inside(Loop)` | the node has an ancestor of that kind |

Field access is `x.field`, and `x?.field` is the same. Reading a field of a missing value gives a missing value. A
field that exists for another kind of node also gives a missing value; a name that is no field of any kind is an
error. Constants: `LOW`, `MEDIUM`, `HIGH`, `DEFAULT`, `PRESENT`. Literals: `'string'`, whole numbers (`0`, `2`; no negative numbers or decimals), `"string"`, `/regex/` (only after
`==~`), `[list, of, values]`, `true`, `false`, `null`. `every` over an empty list is true.

## Node kinds and fields

Kinds to iterate with `Kind.each`: `Catch`, `Call`, `Name`, `Field`, `Param`, `ExprStmt`, `Method`, `Binary` (an operator
expression), `StringLit`, `New` (a constructor call), `Throw`, `Return`, `Break`, `Continue`, `FieldDecl` (a field
declaration, never a local variable), `DoubleLit` (a double or float literal), `If`, `Var` (a field or local variable declarator), `Lambda`, `MethodRef`, `Annotation` (every annotation), `Comment` (every comment in the file: line, block and Javadoc), `Jump` (any of `Return`, `Throw`, `Break`, `Continue`) `Creation` (a `Call` or a `New`) and `Text` (a `StringLit` or a `Comment`). `Loop` can be used in
`instanceof` and `inside`, not iterated.

| Node | Fields |
|---|---|
| any node | `text`, `inTransaction`, `calls` (the calls inside it), `throws` (the throw statements inside it), `catches` (the catch clauses inside it), `assignments`, `callsAfter` (calls in the enclosing method that start after it ends), `enclosingCatch`, `enclosingMethodName`, `enclosingClassMethods` (the method names of every class around it: `'main' in n.enclosingClassMethods`), `enclosingMethod` (the method around it, or missing), `enclosingIterations` (the for-each statements and `x.forEach(lambda)` calls it is inside), `iteratedCollection` / `iteratedType` (of such an iteration: the variable being walked, looking through `keySet()`, `values()`, `entrySet()` and `stream()`, and its type, taking a `new ConcurrentHashMap<>()` initializer into account) |
| `file` (always available) | `path`, `language`, `isTest` |
| `Catch` | `param`, `body` |
| `Param` | `name`, `type` (text), `annotations` (list of names) |
| a block (`.body`) | `statements`, `hasComment` |
| `ExprStmt` | `expression` |
| `Call` | `name`, `nameNode`, `args`, `firstArg`, `scope`, `lazyAssociation`, `dereferenced`, `followedByExit` (the next statement is a break, continue or return), `logArguments` (arguments a log call passes for its placeholders, without a trailing exception or with a single `Object[]` expanded), `updatesCurrentKey` (`put` of the key being visited) |
| `Name` | `name`, `declaredType` |
| any expression | `isIdentitySentinel` (a `static final` field whose comment calls it an identity sentinel; false for anything else) |
| `Binary` | `operator` (`==`, `!=`, `+`, ...), `left`, `right` |
| `New` | `args`, `firstArg`, `typeName`, `causeUnsupported` (the exception class is in the file or the JDK and no constructor takes a cause) |
| `Throw` | `expression` |
| `Return` | `expression` (missing for a bare `return;`) |
| `Call` (more) | `isDiscarded` (an expression statement), `sameClassTransactionalTarget` |
| `Method` (tests) | `assertions` (assertion calls, following helpers of the same class two hops), `isPrivate`, `isFinal`, `hasAnnotationMember('Test', 'expected')` |
| any node (more) | `enclosingClassAnnotations` |
| `Var` | `name`, `typeName`, `isLocal`, `usedInArithmetic`, `neverReferenced` |
| `Param` (more) | `inPublicMethod`, `usedInArithmetic` |
| `Call` (streams, holders) | `chainCalls`, `chainStartsStream`, `hasSideEffectStage`, `holderInitializer`, `releasedInMethodFinally(names)`, `pairedRelease(names)` |
| any node (data) | `inDataClass` |
| `Annotation` | `name` |
| `Call` and `New` (locals) | `assignedToLocal` (the value initialises a local variable, a try-with-resources one included), `inTryResource`, `closedOrHandedOn` (the method closes that local, returns it, stores it or passes it to another call) |
| any node (locks) | `inSynchronized` (inside a `synchronized` block or method) |
| any node (logging) | `inLevelGuard` (inside an `if` whose condition calls `isDebugEnabled()`, `isTraceEnabled()`, `isEnabledFor(...)` or `isLoggable(...)`) |
| any node (transactions) | `inReadOnlyTransaction` (the method says `@Transactional(readOnly = true)`, or has no `@Transactional` of its own and its class says so) |
| `FieldDecl` (more) | `mutatedAfterInit` (`put`, `add`, `remove` and similar on it in a method that is not a constructor, initializer or `@PostConstruct`, outside `synchronized` code), `assignedAfterInit` (`x = ...`, `this.x = ...` or `x++` in such a method) |
| `Method` (JPA) | `usesGeneratedId` (reads a `@GeneratedValue` field of its class, or its getter) |
| `StringLit` | `boundName` (the variable, field or assignment target it is stored under, or missing), `placeholders` (the number of unescaped `{}`) |
| `Method` (more) | `isPublic` (interface methods count), `classIsPublic`, `throwsTypes`, `annotations` |
| `Catch` (more) | `tryCalls` (the calls in the try block it belongs to) |
| any expression (more) | `isThrowable` (by its type, or a catch parameter, or a variable declared with an exception type or an exception-bounded type variable) |
| `FieldDecl` | `hasInitializer`, `name`, `typeName` (simple name, no generics or package), `initializerTypeName`, `isStatic`, `isFinal`, `isPrivate`, `isVolatile`, `classAnnotations`, `usedOnlyInSynchronized` |
| `If` | `doubleCheckedField` (the field declaration `f` in `if (f == null) { synchronized (...) { if (f == null) { f = ...; } } }`, or missing) |
| `Jump` | `escapesFinally` (leaves a `finally` block: not through a nested lambda, class or method, not a break or continue whose loop is inside the finally, not a throw caught by a nested try) |
| `Field` | `name`, `scope` |
| any expression | `type` (qualified name, missing when unresolved), `fetched` |
| `Method` | `name`, `nameNode`, `inController`, `returnedEntity`, `lazyAssociations`, `returnsFetched` |
| `project` (when the run has a project) | `project.config('key')` |

A statement is not an expression: `log.debug("x");` is an `ExprStmt` whose `expression` is a `Call`.

## SQL files

A `.sql` file is read by a tokenizer, not a parser: it removes comments, splits statements at `;` and at a line that is only
`GO`, keeps a `$$ ... $$` body with the statement that opens it, and does not know a dialect. A rule for SQL iterates one of
three kinds, and `file.isTest`, `report` and `candidate` work as for Java:

| Kind | One per | Fields |
|---|---|---|
| `Statement` | statement | `text` (comments removed), `code` (strings blanked too), `precedingComment`, `hasTopLevelWhere`, `tableCreatedInFile` |
| `Column` | column added by `ALTER TABLE ... ADD [COLUMN]` (constraints are not columns) | `name`, `text`, `code`, `tableCreatedInFile` |
| `Line` | non-blank line of code | `text`, `code`, `statementCode`, `executesBuiltString` (`EXEC @v` where the file assigns `@v` by concatenation with another variable) |

A statement is reported on its first line; a column and a line on their own line. `code` is the field to match keywords and
operators against, because the contents of strings are blank in it.

## Config files

A `.yml`, `.yaml`, `.json` or `.properties` file is read into key paths (JSON is read as YAML). A rule iterates `Setting`,
one per scalar value:

| Field | Meaning |
|---|---|
| `key` | the key path in lower-case kebab form with dots: `server.sslEnabled` is `server.ssl-enabled`; list items are `ports[0]` |
| `rawKey` | the key path as written |
| `value` | the value as text; empty for null |
| `profile` | the Spring profile of the document (`application-dev.yml`, or `spring.config.activate.on-profile`), or empty |
| `s.siblingValue('allow-credentials')` | the value of the setting beside this one (same parent key), or missing |

A finding is reported on the line of the value (of the key, in a properties file). Put the key in a message, never the value
of a secret.

## Python and TypeScript files

A `.py` file is parsed with tree-sitter's Python grammar, and a `.ts`, `.js`, `.mjs`, `.cjs`, `.mts` or `.cts` file with its
TypeScript grammar (which reads JavaScript too). The grammars are native libraries bundled in the jar for Linux, macOS and
Windows; a file with a syntax error still gives a tree. `.tsx` and `.jsx` are read with the TypeScript grammar, which has no JSX,
so the JSX in them becomes error nodes. A rule iterates the grammar's own node type as the kind, for example
`except_clause.each { c -> ... }` or `call_expression.each { c -> ... }`, or `Node.each` for every named node (then test
`n.type in ['a', 'b']`). `x instanceof type_name` and `x.inside(type_name)` take a grammar type too.

| Field or fact | Meaning |
|---|---|
| `n.type`, `n.text`, `n.line`, `n.endLine` | the grammar type, the source text, the first and last line |
| `n.children` | the named children in source order (keywords and punctuation are left out) |
| `n.tokens` | the unnamed children as text: `is`, `not`, `==`, `async` |
| `n.parent`, `n.next`, `n.previous`, `n.lastChild` | related nodes, or missing |
| `n.fieldName` | the name the parent gives this node: `body`, `condition`, `cause` |
| `n.field('body')` | the child the grammar gives that field name, or missing |
| `n.child('block')` | the first named child of that type, or missing |
| `n.descendants('call')` | every node below with that type, in source order |
| `n.ancestor('try_statement')`, `n.ancestorOf(['a', 'b'])` | the nearest ancestor with that type, or with one of those types |
| `c.args`, `c.positional` | the arguments of a call, and those that are not `name=value` or a `*`/`**` splat |
| `c.arg(0)`, `c.keyword('timeout')` | the i-th positional argument, and the value of the keyword argument, or missing |
| `n.countOf('name')` | how many identifiers with that text are at or below the node |
| `n.mentions('name')` | an identifier with that text appears at or below the node |
| `n.hasNearbyComment` | a comment starts on the last line of the node, or ends on the line above its first |
| `n.isError` | the node is a syntax error or a missing token |

`file.isTest` also covers `tests/`, `__tests__/`, `test_*.py`, `*_test.py`, `conftest.py` and `*.test.ts`/`*.spec.ts`. The shape of
the tree for a snippet is easiest to see by parsing it; the node and field names are those of the
[tree-sitter-python](https://github.com/tree-sitter/tree-sitter-python) and
[tree-sitter-typescript](https://github.com/tree-sitter/tree-sitter-typescript) grammars.

## Host facts

Facts are reviewed Java that does the work a rule should not express itself. They are called like methods.

| Fact | Meaning |
|---|---|
| `call.guarded(expr, PRESENT)` | A dominating condition or early exit proves the Optional variable `expr` is present at `call` |
| `expr.tainted(isSource, isSanitized)` | The value may derive from a node satisfying `isSource` without passing through one satisfying `isSanitized`. Follows locals, concatenation, pass-through string methods, parameters through callers and return values through callees |
| `expr.fetched` | The entity was produced by a join-fetch query or entity graph, possibly several calls away |
| `node.inTransaction` | The node runs inside a transaction, directly or because every caller does |
| `project.config('key')` | A project configuration value, for example `spring.jpa.open-in-view` |
| `call.guardedByCallers(expr, PRESENT)` | `expr` is a parameter of the method around `call`, the method has callers in the project, and every caller passes `Optional.of(...)` or a variable that is guarded at the call or is a parameter whose callers all guard it. Followed for a few hops; a method with no caller in the project proves nothing |
| `call.releasedInNextFinally(['unlock', 'release'])` | The call is a statement, the next statement is a `try`, and its `finally` calls one of those methods on the same receiver |
| `call.releasedInEnclosingFinally(names)` | The call is inside the `try` block of a `try` whose `finally` calls one of those methods on the same receiver |
| `call.releasedElsewhere(names)` | Another method of the class releases the same receiver without acquiring it itself (acquire and release split across methods) |
| `node.mentions(name)` | The node is, or contains, a use of that variable |
| `node.carries(name)` | The node passes the variable on: it is the variable, a local variable assigned from it, or contains it as something other than the receiver of `getMessage()`, `toString()` and similar. `e` and `e.getCause()` carry `e`; `e.getMessage()` does not |

Calls are resolved by the receiver's static type where the source states it, and otherwise by name and argument count.
Interprocedural facts are bounded: a limited number of calls are followed and a work budget ends a query early, so an
unusually deep flow is reported as not proven rather than slowing the review.

## What is deliberately missing

There are no `if`, `for`, `while`, `return`, `new`, `class`, `import` or assignment statements, and no way to define
a recursive test. Using one gives an error that says so and points to `only`, `skip`, `any { }` and `every { }`. Keeping
the language this small is what makes a rule safe to run without a sandbox, certain to finish, and cheap to test: the
test tooling can automatically delete or invert each clause of a rule to check that the rule's tests would notice.
