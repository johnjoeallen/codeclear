// generated-from: review/java/double-checked-locking-without-volatile/rule.md
// rule-hash: 730d63eb48ea492d5693575d1be3633ea9beeb31a7715ce72545e1727c83f7d9
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'double-checked-locking-without-volatile'

// if (f == null) { synchronized (...) { if (f == null) { f = ...; } } } on a field f. The host finds the field.
If.each { i ->
    def f = i.doubleCheckedField
    only f != null
    // "Do not flag": volatile fields. The holder-class idiom, enum singletons, primitive fields and final fields never
    // match the pattern: it needs a null check and an assignment to the field inside a synchronized block.
    skip f.isVolatile

    report i, "Double-checked locking on '${f.name}' needs the field to be volatile; otherwise another thread can see a partly constructed object"
}
