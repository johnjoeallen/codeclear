// generated-from: review/java/reference-equality-on-values/rule.md
// rule-hash: c9dfb108fb52b0a8c19ddd521d2f3e25fc9758e0269e47f3f724715a48a573b1
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'reference-equality-on-values'

def boxed = ['java.lang.String', 'java.lang.Integer', 'java.lang.Long', 'java.lang.Short', 'java.lang.Byte',
             'java.lang.Double', 'java.lang.Float', 'java.lang.Boolean', 'java.lang.Character']

Binary.each { b ->
    only b.operator in ['==', '!=']

    // "Do not flag": a null operand, and a comparison with a constant documented as an identity sentinel.
    // Enums and primitives never reach the report: only String and boxed types are listed above.
    skip b.left.text == 'null' || b.right.text == 'null'
    skip b.left.isIdentitySentinel || b.right.isIdentitySentinel
    // An identity check before .equals(...) inside an equals() implementation is the usual shortcut.
    skip b.enclosingMethodName == 'equals'

    def known = b.left.type != null && b.right.type != null
    def literal = b.left instanceof StringLit || b.right instanceof StringLit

    // Both operand types are known: a String or boxed type on both sides is a reference comparison.
    report b, "'${b.operator}' compares references, not values, for ${b.left.type} and ${b.right.type}; use equals()",
        b.left.type in boxed && b.right.type in boxed
    // A type could not be resolved: only a comparison with a string literal is certain enough to report.
    report b, "'${b.operator}' compares a String by reference; use equals()",
        literal && !known, confidence: LOW
}
