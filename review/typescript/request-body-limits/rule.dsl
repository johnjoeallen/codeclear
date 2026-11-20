// generated-from: review/typescript/request-body-limits/rule.md
// rule-hash: 0186929e40a7c2be0bf1343388ee10ad4ae0f9bb38efa4c4894fb6d1c0d94327
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'request-body-limits'

// An option object with a key set to something, or with the key missing.
def huge = { v -> v != null && (v.text ==~ /(?is)["']?\s*(\d{3,}|[5-9]\d)\s*(mb|gb)\s*["']?/ || v.text ==~ /(?is)["']?\s*\d+\s*gb\s*["']?/ || v.text in ['Infinity', 'Number.MAX_SAFE_INTEGER'] || v.text ==~ /\d{9,}/) }

call_expression.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def o = c.arg(0)
    def f = c.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition', 'program'])

    candidate c, "${fn}() has no limits, so one request can upload a file as large as the disk, or thousands of files: set limits.fileSize, files and fields",
        (fn == 'multer' && (o == null || (o.type == 'object' && !o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?limits["']?/ })))
            || (fn in ['busboy', 'Busboy'] && (o == null || (o.type == 'object' && !o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?limits["']?/ })))
            || (fn ==~ /(formidable|\w*\.IncomingForm|new formidable)/ && (o == null || !(o.text ==~ /(?s).*maxFileSize.*/)))
    candidate c, "${fn}() sets a very large body limit: one request can hold that much memory, and a few of them take the server down. Use the smallest limit the route needs",
        fn ==~ /(bodyParser|express)\.(json|urlencoded|raw|text)|(json|urlencoded|raw|text)/ && o != null && o.type == 'object' && o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?limit["']?/ && huge(p.field('value')) }
    // A request read by hand: req.on('data', chunk => body += chunk) with no size check.
    candidate c, "This reads the request body chunk by chunk and keeps all of it, with no size check: a client can send a body as large as it likes. Count the bytes and destroy the request past a limit",
        fn in ['req.on', 'request.on'] && o != null && o.text ==~ /["']data["']/ && c.arg(1) != null && c.arg(1).text ==~ /(?s).*(\+=|push\(|concat\().*/ && !(f.text ==~ /(?s).*(\.length|\.size|limit|destroy|MAX_|maxBytes).*/)
}
