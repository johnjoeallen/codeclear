// generated-from: review/typescript/unsafe-html-sink/rule.md
// rule-hash: 8e2f1908f65ac97953d392db1341e869439a935b3080475f0c67477958117941
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unsafe-html-sink'

def literal = { a -> a != null && (a.type == 'string' || (a.type == 'template_string' && a.descendants('template_substitution').isEmpty())) }
// "Do not flag": a value that went through a recognised sanitizer, and an empty string.
def clean = { a -> a.text ==~ /(?s).*(DOMPurify\.sanitize|sanitizeHtml|sanitize\(|xss\(|escapeHtml|he\.encode).*/ }
def empty = { a -> a != null && a.type == 'string' && a.text ==~ /["'`]{2}/ }

Node.each { n ->
    only n.type in ['assignment_expression', 'call_expression', 'new_expression', 'jsx_attribute']
    skip file.isTest
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    def left = n.type == 'assignment_expression' ? n.field('left') : n
    def right = n.type == 'assignment_expression' ? n.field('right') : n
    def a0 = n.arg(0)
    def html = n.type == 'jsx_attribute' && n.child('property_identifier') != null && n.child('property_identifier').text == 'dangerouslySetInnerHTML'
    def htmlValue = html && n.child('jsx_expression') != null && n.child('jsx_expression').child('object') != null ? n.child('jsx_expression').child('object').children.any { p -> p.type == 'pair' && p.field('key').text == '__html' && !literal(p.field('value')) && !clean(p.field('value')) } : false

    candidate n, "Assigning a variable to ${left.field('property').text} lets markup in it run as HTML and scripts, so a value from outside is cross-site scripting. Use textContent, or sanitize it with DOMPurify first",
        n.type == 'assignment_expression' && left.type == 'member_expression' && left.field('property').text in ['innerHTML', 'outerHTML'] && !literal(right) && !empty(right) && !clean(right)
    candidate n, "${fn}() writes markup into the page, and the value is not a literal: a value from outside is cross-site scripting. Sanitize it, or use textContent",
        fn ==~ /(\w+\.)*(insertAdjacentHTML|document\.write|document\.writeln)/ && n.args.any { a -> !literal(a) && !clean(a) && a.type != 'string' }
    candidate n, "dangerouslySetInnerHTML takes a value that is not a literal: markup in it runs as HTML and scripts, so a value from outside is cross-site scripting. Sanitize it with DOMPurify first",
        htmlValue
    candidate n, "eval() runs the string it is given as code, and the string is not a literal",
        fn == 'eval' && a0 != null && !literal(a0)
    candidate n, "new Function() compiles the string it is given as code, and the string is not a literal",
        n.type == 'new_expression' && n.field('constructor').text == 'Function' && n.args.any { a -> !literal(a) }
    candidate n, "${fn}() is given a string, which is run as code like eval(): pass a function",
        fn in ['setTimeout', 'setInterval', 'window.setTimeout', 'window.setInterval'] && a0 != null && !(a0.type in ['arrow_function', 'function_expression', 'function', 'identifier', 'member_expression', 'call_expression'])
}
