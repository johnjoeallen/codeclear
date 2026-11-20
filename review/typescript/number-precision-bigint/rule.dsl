// generated-from: review/typescript/number-precision-bigint/rule.md
// rule-hash: b05f8fa182da71b3fcfcbe293943f2a380f46e476b6f670f7c7ab6ad29b614d2
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'number-precision-bigint'

def bigName = { t -> t ==~ /(?i)(\w*[_.])?(id|\w*Id|\w*_id|\w*count|balance|\w*total|\w*sequence|\w*offset)/ }
def fromRow = { a -> a.type == 'member_expression' && a.field('object').text ==~ /(?i)(\w+\.)*(row|rows\[\d+\]|result|record|entity|res\.rows\[\d+\]|r|data|item)/ && bigName(a.field('property').text) }

Node.each { n ->
    only n.type in ['call_expression', 'unary_expression']
    skip file.isTest
    def module = n.ancestor('program')
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    def a0 = n.type == 'call_expression' ? n.arg(0) : n.field('argument')

    // "Do not flag": ids that are UUIDs or kept as strings.
    candidate n, "${fn}() turns a value that may be a 64-bit database id or counter into a double: above 2^53 it silently loses digits, so two different ids can become equal. Keep it as a string or BigInt",
        n.type == 'call_expression' && fn in ['Number', 'parseInt', 'parseFloat', 'Number.parseInt', 'Number.parseFloat'] && a0 != null && fromRow(a0) && !(module.text ==~ /(?s).*(uuid|UUID).*/)
    candidate n, "Unary + turns a value that may be a 64-bit database id or counter into a double: above 2^53 it silently loses digits. Keep it as a string or BigInt",
        n.type == 'unary_expression' && '+' in n.tokens && a0 != null && fromRow(a0) && !(module.text ==~ /(?s).*(uuid|UUID).*/)
    // JSON cannot hold a BigInt: JSON.stringify throws TypeError unless something converts it.
    // "Do not flag": a toJSON override or a replacer.
    candidate n, "${fn}() is given a value that holds a BigInt, and JSON cannot represent one: it throws TypeError at run time. Convert it with toString(), or pass a replacer",
        n.type == 'call_expression' && (fn == 'JSON.stringify' || fn ==~ /(res|response|reply)\.(json|send)/) && a0 != null && (a0.text ==~ /(?s).*(BigInt\(|\b\d+n\b).*/)
            && !(n.args.size().isGreaterThan(1)) && !(module.text ==~ /(?s).*(BigInt\.prototype\.toJSON|replacer).*/)
}
