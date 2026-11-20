// generated-from: review/common/leftover-debug-output/rule.md
// rule-hash: 9ae364dac62dacf0ab876f5972f589d34f51a6d52183ad2bb3ea8d83949b7245
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'leftover-debug-output'

// Java: System.out / System.err print calls and e.printStackTrace(). Other languages' calls have no analyzer yet.
Call.each { c ->
    // "Do not flag": test files, and command-line tools or scripts whose purpose is console output: a main method, any
    // method of a class that has one, and code under bin/, scripts/ or cli/.
    skip file.isTest || 'main' in c.enclosingClassMethods || file.path ==~ /(^|.*\/)(bin|scripts|cli)\/.*/

    report c, "Debug output left in the code: ${c.scope.text}.${c.name}(...) writes to the console; use the logger",
        c.name ==~ /print(ln|f)?/ && c.scope.text in ['System.out', 'System.err']
    report c, "printStackTrace() writes the stack trace to the console; log the exception instead",
        c.name == 'printStackTrace' && c.args.isEmpty()
}
