// generated-from: review/typescript/command-injection/rule.md
// rule-hash: 4de04147978713176deef9cc547531992a7684f6a0941c15e3ee2748b52f0823
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'command-injection'

// A string built from variables, passed to a function that runs it through a shell.
def built = { a -> a != null && ((a.type == 'template_string' && !a.descendants('template_substitution').isEmpty())
    || (a.type == 'binary_expression' && '+' in a.tokens && !a.descendants('identifier').isEmpty())) }

call_expression.each { c ->
    skip file.isTest
    def module = c.ancestor('program')
    only module.text ==~ /(?s).*(child_process|shelljs|execa).*/
    def fn = c.field('function').text
    def options = c.arg(1)
    def shell = c.args.any { a -> a.type == 'object' && a.children.any { p -> p.type == 'pair' && p.field('key').text == 'shell' && p.field('value').type == 'true' } }

    // "Do not flag": a constant command, and execFile or spawn with an argument array and no shell.
    candidate c, "${fn}() runs a command string through the shell, and the string is built from variables: a value such as '; rm -rf /' becomes part of the command. Use execFile or spawn with an argument array",
        fn ==~ /(child_process\.|cp\.|shelljs\.|shell\.)?(exec|execSync)/ && built(c.arg(0))
    candidate c, "${fn}() is given shell: true, so its arguments go through the shell and a variable in them can inject commands: drop shell: true and keep the arguments in an array",
        fn ==~ /(child_process\.|cp\.)?(spawn|spawnSync|execFile|execFileSync)/ && shell && !(c.arg(0).type == 'string')
}
