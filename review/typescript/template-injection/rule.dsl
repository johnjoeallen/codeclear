// generated-from: review/typescript/template-injection/rule.md
// rule-hash: 43f331b4e114dc00eb5e742499a181a4fe8340ea34fedf704a3161c87d5b3b3b
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'template-injection'

def literal = { a -> a != null && (a.type == 'string' || (a.type == 'template_string' && a.descendants('template_substitution').isEmpty())) }
def engine = { t -> t in ['ejs.render', 'pug.render', 'pug.compile', 'Handlebars.compile', 'handlebars.compile', '_.template', 'lodash.template', 'nunjucks.renderString', 'vm.runInNewContext', 'vm.runInThisContext', 'vm.runInContext', 'mustache.render', 'Mustache.render', 'doT.template'] }

Node.each { n ->
    only n.type in ['call_expression', 'new_expression']
    skip file.isTest
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])
    def a0 = n.arg(0)

    // The template or code itself, not the data it is rendered with. "Do not flag": a literal template with data as context.
    candidate n, "${fn}() is given a template or code that is not a literal: a template is code, so whoever controls the string runs code on the server. Keep the template constant and pass the value as data",
        engine(fn) && a0 != null && !literal(a0)
    candidate n, "new vm.Script() runs the string it is given: whoever controls the string runs code on the server",
        n.type == 'new_expression' && n.field('constructor').text in ['vm.Script', 'Script'] && a0 != null && !literal(a0)
    // A template name from the request lets the caller render any file, including ones with ../.
    candidate n, "res.render() is given a template name from the request: the caller can pick any template file, or climb out of the views folder with '../'. Check the name against an allow-list",
        fn ==~ /(\w+\.)?render/ && a0 != null && !literal(a0) && (a0.derivedFrom('req', f) || a0.derivedFrom('request', f)) && !(f.text ==~ /(?s).*(allowlist|allowedViews|ALLOWED|whitelist|\.includes\().*/)
}
