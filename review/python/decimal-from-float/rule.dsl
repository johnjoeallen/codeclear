// generated-from: review/python/decimal-from-float/rule.md
// rule-hash: fe50365e82824eba1e5b7372f3808c6e14f6353a12fc1d7941c7f1f9fe153947
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'decimal-from-float'

// A float anywhere in the argument: a literal, a float(...) call, or either one inside arithmetic. String text is
// string_content, so a "1.5" is not a float.
def hasFloat = { x -> x.type == 'float' || !x.descendants('float').isEmpty()
    || (x.type == 'call' && x.field('function').text == 'float')
    || x.descendants('call').any { k -> k.field('function').text == 'float' } }

call.each { c ->
    only c.field('function').text in ['Decimal', 'decimal.Decimal']
    // "Do not flag": Decimal("0.1"), Decimal(int) and Decimal(str(x)) have no float in them, and Decimal.from_float is another call.
    only c.positional.any(hasFloat)

    report c, "Decimal() of a float keeps the float's binary error (Decimal(19.99) is 19.989999...): pass a string, Decimal('19.99')"
}
