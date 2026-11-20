// generated-from: review/typescript/float-money-arithmetic/rule.md
// rule-hash: a41c038f69c5354ec439ca87f90da844fce36fbdd56e55ba3dee19e340bd4953
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'float-money-arithmetic'

def money = { t -> t ==~ /(?i)(\w*[_.])?(price|amount|total|balance|fee|tax|cost|vat|subtotal|discount|salary|payment|charge|refund)\w*/ && !(t ==~ /(?i).*(count|length|type|id|name|url)$/) }
def rounding = { k -> k.field('function').text ==~ /(\w+\.)*toFixed|Math\.(round|floor|ceil)|parseFloat|Number\.parseFloat/ }
def moneyIn = { n -> n.type == 'identifier' && money(n.text) || n.type == 'member_expression' && money(n.field('property').text)
    || n.descendants('identifier').any { i -> money(i.text) } || n.descendants('property_identifier').any { i -> money(i.text) } }

Node.each { n ->
    only n.type in ['binary_expression', 'call_expression']
    skip file.isTest
    def module = n.ancestor('program')
    // "Do not flag": a decimal library, integer minor units, and display-only formatting.
    skip module.text ==~ /(?s).*(decimal\.js|big\.js|bignumber|dinero|currency\.js|Decimal\(|cents|pence|minorUnit|minor_unit|centavos).*/
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    skip f.text ==~ /(?s).*Intl\.NumberFormat.*/
    def fn = n.type == 'call_expression' ? n.field('function').text : ''

    // A rounding or parsing call has its own finding; the arithmetic inside it, or around it, is not reported again.
    def covered = n.type == 'binary_expression' && (n.ancestors('call_expression').any(rounding) || n.descendants('call_expression').any(rounding))

    candidate n, "Arithmetic on a money amount with floating-point numbers: 0.1 + 0.2 is 0.30000000000000004, so totals drift by a cent and comparisons fail. Keep amounts as integer cents, or use a decimal library",
        n.type == 'binary_expression' && !covered && (n.tokens.any { t -> t in ['+', '-', '*', '/'] }) && moneyIn(n) && !(n.field('left').type == 'string') && !(n.field('right').type == 'string')
            && !(n.field('left').type == 'template_string')
    candidate n, "${fn}() rounds or parses a money amount as a float: rounding errors accumulate. Work in integer cents, or use a decimal library",
        n.type == 'call_expression' && (fn ==~ /(\w+\.)*(toFixed)/ || fn in ['Math.round', 'Math.floor', 'Math.ceil', 'parseFloat', 'Number.parseFloat']) && (moneyIn(n.field('function')) || n.args.any { a -> moneyIn(a) })
}
