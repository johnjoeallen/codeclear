// generated-from: review/typescript/path-traversal/rule.md
// rule-hash: 1b68e1b9ae062591430ece066a0b287347258411b65b375d0ce0cf0be24e3f2a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'path-traversal'

def fsCall = { t -> t ==~ /(fs\.|fsp\.|fs\.promises\.|promises\.)?(readFile|readFileSync|createReadStream|createWriteStream|writeFile|writeFileSync|appendFile|unlink|unlinkSync|rm|rmSync|readdir|readdirSync|stat|statSync|access|open|openSync|rename|copyFile)/
    || t ==~ /(\w+\.)?(sendFile|download|sendfile)/ }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    only fsCall(fn)
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    def a0 = c.arg(0)
    only a0 != null && a0.type != 'string'
    // A value the caller controls: from req/request, or a parameter whose name says it is a file or path.
    def fromRequest = a0.derivedFrom('req', f) || a0.derivedFrom('request', f) || a0.derivedFrom('params', f) || a0.derivedFrom('message', f)
    def fromName = a0.descendants('identifier').any { i -> i.text ==~ /(?i).*(file|path|dir|folder).*/ && f.field('parameters') != null && f.field('parameters').mentions(i.text) }
        || (a0.type == 'identifier' && a0.text ==~ /(?i).*(file|path|dir|folder).*/ && f.field('parameters') != null && f.field('parameters').mentions(a0.text))
    only fromRequest || fromName
    // "Do not flag": basename, a resolved path checked against a base, realpath, and sendFile with a root option.
    skip f.text ==~ /(?s).*(basename|startsWith\(|realpath|isSubPath|isPathInside|relative\(|root\s*:).*/

    candidate c, "${fn}() is given a path that the caller controls, and nothing checks that it stays under a base folder: '../../etc/passwd' reads or writes any file. Use path.basename, or resolve it and check it starts with the base"
}
