// generated-from: review/common/expensive-log-construction/rule.md
// rule-hash: 82ca5799741a5586b5ddaa565764de1b562ee05c624f78732f3bcb9a52c26a43
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'expensive-log-construction'

// A call that is not a plain getter: building the argument does work even when the level is off.
def costly = { k -> !(k.name ==~ /(get|is)[A-Z].*/ && k.args.isEmpty()) }
def lazy = { a -> a instanceof Lambda || a instanceof MethodRef }

// Whether the argument is really expensive depends on the called method and the data, so this is a candidate for the AI.
Call.each { x ->
    only x.name in ['debug', 'trace'] && x.scope.text ==~ /(?i)(this\.)?(log|logger)/
    skip file.isTest

    // "Do not flag": a level guard around the call, and a supplier lambda, which is only evaluated when the level is on.
    skip x.inLevelGuard
    // An argument that calls something other than a trivial getter: concatenation of calls, String.format, toJson, ...
    only x.args.any { a -> !lazy(a) && a.calls.any(costly) }

    candidate x, "The ${x.name} message is built eagerly with a method call, so the work is done even when ${x.name} is off: guard it or pass a supplier"
}
