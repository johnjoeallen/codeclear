// generated-from: review/python/template-and-path-injection/rule.md
// rule-hash: dca2c794e6bf4cdb6de769d212b9b81eff2f76165a123fdc3cd48d79cb2e5989
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'template-and-path-injection'

def templateSink = { t -> t in ['render_template_string', 'jinja2.Template', 'Template', 'mako.Template'] || t ==~ /(\w+\.)*from_string/ }
def pathSink = { t -> t in ['open', 'send_file', 'send_from_directory', 'os.path.join', 'Path', 'pathlib.Path', 'os.remove', 'os.unlink', 'shutil.copy', 'shutil.move', 'shutil.rmtree', 'os.listdir'] }

call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def f = c.ancestor('function_definition')
    // A value that the caller controls: the request, or a parameter of the handler.
    def scope = c.ancestorOf(['function_definition', 'module'])
    def controlled = { a -> a != null && a.type != 'integer' && !(a.type == 'string' && a.descendants('interpolation').isEmpty()) && (a.derivedFrom('request', scope) || a.usesParameterOf(f)) }
    // "Do not flag": a path checked against a base, or cleaned by secure_filename.
    // The outermost sink only: send_file(os.path.join(...)) is one finding.
    skip c.ancestors('call').any { k -> pathSink(k.field('function').text) }
    def checked = f != null && (f.mentions('secure_filename') || f.mentions('is_relative_to') || f.mentions('resolve') || f.mentions('realpath') || f.mentions('commonpath') || f.mentions('allowed'))

    candidate c, "The template source passed to ${fn}() comes from the request: a template is code, so the caller can run Python through it (server-side template injection). Pass the value as a context variable to a constant template",
        templateSink(fn) && controlled(c.arg(0))
    candidate c, "The path passed to ${fn}() comes from the request, and nothing checks that it stays under a base folder: '../' reads or deletes any file. Use secure_filename, or resolve the path and check it with is_relative_to",
        pathSink(fn) && !checked && c.args.any { a -> controlled(a) }
}
