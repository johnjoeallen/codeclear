// generated-from: review/java/exception-cause-dropped/rule.md
// rule-hash: a3b45ceb23bdd7cd33ead5f59a3dfddc476dae3b7da5bba3d7c330769c1cd20d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'exception-cause-dropped'

Throw.each { t ->
    only t.expression instanceof New
    def c = t.enclosingCatch
    only c != null

    // "Do not flag": a comment explains why the cause is discarded.
    skip c.body.hasComment
    // The new exception's class is in this file, or is a JDK class, and none of its constructors accept a cause.
    skip t.expression.causeUnsupported
    // The caught exception is passed in any argument position, including e.getCause() or a local variable
    // assigned from it. Reading only its message (e.getMessage()) does not pass the cause on.
    skip t.expression.args.any { a -> a.carries(c.param.name) }
    // Or it is attached some other way.
    skip c.body.calls.any { x -> x.name in ['initCause', 'addSuppressed'] && x.args.any { a -> a.carries(c.param.name) } }

    report t, "The cause is dropped: '${c.param.name}' is not passed to the new ${t.expression.typeName}"
}
