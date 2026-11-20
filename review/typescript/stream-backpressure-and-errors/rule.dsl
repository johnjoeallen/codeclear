// generated-from: review/typescript/stream-backpressure-and-errors/rule.md
// rule-hash: b4b8f7d144b49967cc2866a3d633f2dcd25627f2103e2fcfee4040b8534fb9d5
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'stream-backpressure-and-errors'

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function')
    only fn.type == 'member_expression'
    def name = fn.field('property').text
    def module = c.ancestor('program')
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": pipeline() and finished(), which forward errors and clean up.
    def managed = module.text ==~ /(?s).*(pipeline\(|finished\(|stream\/promises).*/

    // pipe() does not forward errors: an error on one stream leaves the others open, and the process can leak or crash.
    candidate c, "pipe() does not pass errors on, and no stream here has an 'error' listener: a failure leaves the other streams open and can crash the process. Use stream.pipeline(a, b, callback)",
        name == 'pipe' && !managed && !(module.text ==~ /(?s).*\.on\(\s*["']error["'].*/)
    // write() returns false when the buffer is full: a loop that ignores it fills memory.
    candidate c, "write() is called in a loop and its result is ignored: when the writable's buffer is full it returns false, and writing on anyway makes memory grow. Wait for 'drain' when it returns false",
        name == 'write' && c.parent != null && c.parent.type == 'expression_statement' && c.ancestorOf(['for_statement', 'for_in_statement', 'for_of_statement', 'while_statement', 'do_statement', 'function_declaration', 'arrow_function', 'function_expression']) != null
            && c.ancestorOf(['for_statement', 'for_in_statement', 'for_of_statement', 'while_statement', 'do_statement', 'function_declaration', 'arrow_function', 'function_expression']).type in ['for_statement', 'for_in_statement', 'for_of_statement', 'while_statement', 'do_statement']
            && !(f.text ==~ /(?s).*(drain|pipeline|once\(\s*\w+\s*,|writableNeedDrain).*/)
    // A data listener that writes into something slower, with no pause.
    candidate c, "This 'data' listener writes into another stream and never pauses the source, so a slow writable buffers everything in memory. Use pipe or pipeline, or pause until 'drain'",
        name == 'on' && c.arg(0) != null && c.arg(0).text ==~ /["']data["']/ && c.arg(1) != null && c.arg(1).text ==~ /(?s).*\.write\(.*/ && !(c.arg(1).text ==~ /(?s).*(pause|drain|resume|\bif\s*\(\s*!).*/) && !managed
}
