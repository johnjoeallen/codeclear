// generated-from: review/typescript/express-async-handler-rejection/rule.md
// rule-hash: 03cc9215e8afa6c890aa652ab6de0be495020d684c3ec64396350e89b0098520
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'express-async-handler-rejection'

// A handler that rejects: Express 4 never sees the rejection, so the request hangs and Node logs an unhandled rejection.
call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only fn ==~ /(app|router|\w*[rR]outer|server)\.(get|post|put|patch|delete|all|use)/
    def module = c.ancestor('program')
    // "Do not flag": frameworks and versions that handle rejected promises, and a wrapper around the handler.
    skip module.text ==~ /(?s).*(fastify|@nestjs|koa|hapi|express-async-errors|asyncHandler|express-async-handler|catchAsync|wrapAsync).*/
    skip module.text ==~ /(?s).*(from\s+["']express["'].*"express"\s*:\s*"\^?5).*/

    // An async handler that awaits and has no try/catch of its own.
    candidate c, "This async route handler awaits, and a rejection is not caught: Express 4 ignores it, so the request hangs until it times out. Wrap the body in try/catch and call next(err), or wrap the handler in asyncHandler",
        c.args.any { h -> h.type in ['arrow_function', 'function_expression'] && 'async' in h.tokens && !h.descendants('await_expression').isEmpty() && h.descendants('try_statement').isEmpty() }
}
