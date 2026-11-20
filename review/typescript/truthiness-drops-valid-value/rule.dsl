// generated-from: review/typescript/truthiness-drops-valid-value/rule.md
// rule-hash: 8d2dea8824259a0f291f39e77179cfa48882906ba3182f073aed49869d1eadc4
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'truthiness-drops-valid-value'

// Names that hold a number or a flag, where 0, '' and false are real values.
def scalarName = { n -> n ==~ /(?i)(\w*[_.])?(count|total|amount|limit|offset|index|enabled|quantity|qty|page|size|retries|port|age|price|balance|score|level|volume|ratio|delay|timeout|min|max)\w*/ && !(n ==~ /(?i).*(url|name|list|items|options|type)$/) }
def nameOf = { x -> x.type == 'identifier' ? x.text : (x.type == 'member_expression' ? x.field('property').text : '') }

Node.each { n ->
    only n.type in ['unary_expression', 'if_statement', 'binary_expression', 'ternary_expression']
    skip file.isTest
    // The tested value: !x, if (x), x || fallback, x ? a : b.
    def x = n.type == 'unary_expression' ? n.field('argument') : (n.type == 'if_statement' ? n.field('condition').childAt(0) : (n.type == 'binary_expression' ? n.field('left') : n.field('condition')))
    only x != null && x.type in ['identifier', 'member_expression'] && scalarName(nameOf(x))

    candidate n, "!${x.text} is true for 0, an empty string and false as well as for a missing value: a real value of ${nameOf(x)} is treated as missing. Compare with undefined or null (=== undefined, == null) instead",
        n.type == 'unary_expression' && '!' in n.tokens
    candidate n, "if (${x.text}) skips the branch for 0, an empty string and false as well as for a missing value: a real value of ${nameOf(x)} is treated as missing. Compare with undefined or null instead",
        n.type == 'if_statement'
    // "Do not flag": ?? and the explicit checks do not match, because they are not || or a bare test.
    candidate n, "${x.text} || fallback replaces 0, an empty string and false with the fallback, not only a missing value: use ${x.text} ?? fallback",
        n.type == 'binary_expression' && '||' in n.tokens && n.field('right').type in ['number', 'string', 'true', 'false', 'array', 'object', 'template_string']
    candidate n, "${x.text} ? ... : ... takes the false branch for 0, an empty string and false as well as for a missing value: a real value of ${nameOf(x)} is treated as missing",
        n.type == 'ternary_expression'
}
