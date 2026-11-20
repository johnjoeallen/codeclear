// generated-from: review/typescript/prototype-pollution/rule.md
// rule-hash: 1e7f4a3be4c15ea550ba6fe87d90db3195f33e95d63517da9a09cef6db82cf5f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'prototype-pollution'

def mergeCall = { t -> t ==~ /(_|lodash|\$|jQuery)\.(merge|mergeWith|defaultsDeep|set|setWith|extend|assignIn)/ || t in ['deepmerge', 'merge', 'deepExtend', 'mergeDeep', 'defaultsDeep'] || t ==~ /\w*(deepmerge|merge)\w*/ }

Node.each { n ->
    only n.type in ['call_expression', 'assignment_expression']
    skip file.isTest
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    // "Do not flag": a function that refuses __proto__, constructor and prototype, and targets with no prototype.
    skip f.text ==~ /(?s).*(__proto__|"constructor"|'constructor'|hasOwnProperty|Object\.create\(null\)|new Map|Object\.hasOwn).*/
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    def left = n.type == 'assignment_expression' ? n.field('left') : n
    candidate n, "${fn}() merges request data into an object recursively: a key such as __proto__ or constructor.prototype changes Object.prototype for the whole process (prototype pollution). Use a merge that rejects those keys, or build the object from the fields you expect",
        n.type == 'call_expression' && mergeCall(fn) && n.args.any { a -> a.derivedFrom('req', f) || a.derivedFrom('request', f) || a.derivedFrom('body', f) || a.text ==~ /(?s).*JSON\.parse.*/ }
    candidate n, "This assignment uses a key from the input as the property name, and nothing rejects __proto__, constructor or prototype: a crafted key changes Object.prototype for the whole process. Use a Map, Object.create(null), or reject those names",
        n.type == 'assignment_expression' && left.type == 'subscript_expression' && left.field('index').type == 'identifier'
            && (left.field('index').derivedFrom('req', f) || left.field('index').derivedFrom('request', f) || !f.descendants('for_in_statement').isEmpty() || !f.descendants('for_of_statement').isEmpty())
            && (f.mentions('req') || f.mentions('body') || f.mentions('input') || f.mentions('payload'))
}
