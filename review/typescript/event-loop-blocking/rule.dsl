// generated-from: review/typescript/event-loop-blocking/rule.md
// rule-hash: 8d7704e27bf999c8e33b86ea5dd88c96dd281b1c70e46c9e14c3bca1e41c3ab2
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'event-loop-blocking'

def syncCall = { t -> t ==~ /(fs|fsSync)\.\w+Sync/ || t ==~ /(\w+\.)?(pbkdf2Sync|scryptSync|randomFillSync|execSync|spawnSync|execFileSync|gzipSync|gunzipSync|deflateSync|inflateSync|brotliCompressSync|brotliDecompressSync|readFileSync|writeFileSync|readdirSync|statSync|existsSync|mkdirSync|unlinkSync|copyFileSync)/ || t ==~ /(zlib|crypto)\.\w+Sync/ }

Node.each { n ->
    only n.type in ['call_expression', 'regex']
    skip file.isTest
    // "Do not flag": CLI scripts, build tools and browser code.
    skip file.path ==~ /(?i).*(\/|^)(scripts?|bin|cli|tools?|build|tasks|migrations?|seeds?|public|client|frontend|browser)\/.*|.*(webpack|gulp|rollup|vite|jest|karma)\.\w+\.?\w*/
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition'])
    def fn = n.type == 'call_expression' ? n.field('function').text : ''
    // Inside a request handler or a consumer, not at module load.
    def handler = f != null && f.field('parameters') != null && f.field('parameters').text ==~ /(?s).*\b(req|request|res|response|ctx|message|msg|event|job|payload)\b.*/

    candidate n, "${fn}() blocks the event loop: while it runs, the server answers nobody else. Use the async version, or move the work to a worker thread",
        n.type == 'call_expression' && syncCall(fn) && handler
    // A regex with a repeated group that contains a repeat: input such as aaaa...! makes it take exponential time.
    candidate n, "This regular expression has a repeat inside a repeat, such as (a+)+: a crafted input makes it run for minutes and block the event loop (ReDoS). Rewrite it without nested quantifiers",
        n.type == 'regex' && n.text ==~ /(?s).*\((?:[^()\\]|\\.)*[+*](?:[^()\\]|\\.)*\)[+*{].*/ && !(f == null)
}
