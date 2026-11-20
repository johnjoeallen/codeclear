// generated-from: review/python/shell-injection-and-tls/rule.md
// rule-hash: 54be5593306c8a78cefa3e3e6ef3404f3cfa4398c1462700f742ac2dd07d8b0f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'shell-injection-and-tls'

def constant = { a -> a.type in ['string', 'concatenated_string'] && a.descendants('interpolation').isEmpty() }
def subprocessCall = { t -> t ==~ /subprocess\.(run|call|check_call|check_output|Popen|getoutput|getstatusoutput)/ }

Node.each { n ->
    only n.type in ['call', 'attribute', 'assignment']
    skip file.isTest
    def fn = n.field('function').text
    def shell = n.keyword('shell')
    def verify = n.keyword('verify')

    // "Do not flag": shell=True with a constant string and no interpolation.
    report n, "${fn}() runs a command through the shell with a value that is not a constant: it can inject commands, so pass a list and drop shell=True",
        subprocessCall(fn) && shell != null && shell.type == 'true' && n.arg(0) != null && !constant(n.arg(0))
    report n, "${fn}() runs its argument through the shell and the argument is not a constant: use subprocess with a list of arguments",
        fn in ['os.system', 'os.popen'] && n.arg(0) != null && !constant(n.arg(0))
    report n, "verify=False turns certificate verification off, so the connection can be intercepted",
        verify != null && verify.type == 'false'
    report n, "ssl._create_unverified_context() builds a context that does not verify certificates",
        fn == 'ssl._create_unverified_context'
    report n, "ssl.CERT_NONE turns certificate verification off, so the connection can be intercepted",
        n.type == 'attribute' && n.text == 'ssl.CERT_NONE'
    report n, "check_hostname = False turns hostname verification off, so the connection can be intercepted",
        n.type == 'assignment' && n.field('left').type == 'attribute' && n.field('left').field('attribute').text == 'check_hostname'
            && n.field('right').type == 'false'
}
