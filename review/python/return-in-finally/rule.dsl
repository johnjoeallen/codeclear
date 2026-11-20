// generated-from: review/python/return-in-finally/rule.md
// rule-hash: be723fa3eece1758ae0cc87b6cf5f4898ec68f93526c9138a93d46ac08e9f290
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'return-in-finally'

def jump = { n -> n.type in ['return_statement', 'break_statement', 'continue_statement'] }
def boundary = ['finally_clause', 'function_definition', 'lambda', 'class_definition', 'for_statement', 'while_statement']

// A return, break or continue that leaves a finally block drops the exception or the result that the try was delivering.
Node.each { j ->
    only jump(j)
    skip file.isTest
    // The nearest enclosing finally, without leaving the function: a return in a nested function or lambda is its own.
    // A break or continue whose loop is inside the finally stops at that loop, so it is not a problem.
    def nearest = j.ancestorOf(boundary)
    only nearest != null && nearest.type == 'finally_clause'

    report j, "This ${j.type} leaves a finally block, so it discards the exception or result that the try block was delivering"
}
