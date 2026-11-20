// generated-from: review/python/config-defaults-and-import-side-effects/rule.md
// rule-hash: 78c391ae862aec587c7c0dd30e5139cb7a2261f5c578ebb7101eb0e2f6aba12d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'config-defaults-and-import-side-effects'

def env = { t -> t in ['os.getenv', 'os.environ.get', 'getenv', 'environ.get'] }
def sideEffect = { t -> t ==~ /requests\.\w+/ || t ==~ /(\w+\.)*connect/ || t ==~ /Thread\(.*\)\.start/ || t ==~ /(schedule|scheduler)\.(start|run_pending|every)/
    || t ==~ /subprocess\.\w+/ || t ==~ /(app|server)\.run/ || t ==~ /(\w+\.)*(urlopen|socket)/ }

call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    // A call at module level: not in a function or class, and not under 'if __name__ == "__main__"'.
    def atModule = c.ancestorOf(['function_definition', 'class_definition', 'lambda']) == null
    def mainGuard = c.ancestors('if_statement').any { i -> i.field('condition').text ==~ /(?s).*__name__.*/ }
    def inner = c.arg(0)

    // "Do not flag": a default that is a real default, and a bool() of a comparison.
    candidate c, "bool(os.getenv(...)) is True for any non-empty text, including 'false' and '0', so setting the variable to false turns the feature on: compare the text, os.getenv('X', '').lower() in ('1', 'true')",
        fn == 'bool' && inner != null && inner.type == 'call' && env(inner.field('function').text)
    candidate c, "int(os.getenv(...)) with no default raises TypeError at import when the variable is missing, with an unhelpful message: give a default or check it and say which setting is missing",
        fn in ['int', 'float'] && inner != null && inner.type == 'call' && env(inner.field('function').text) && inner.args.size().isEqualTo(1)
    candidate c, "This environment default looks like a production value: a missing variable silently points a dev or test run at production. Fail when it is missing, or default to something harmless",
        env(fn) && c.arg(1) != null && c.arg(1).type == 'string' && c.arg(1).text ==~ /(?is).*(prod|:\/\/|\.com|\.io|amazonaws|live).*/
    candidate c, "${fn}() runs when the module is imported: importing it (a test, a tool, a worker that only needs a constant) connects, starts a thread or runs a process. Move it into a function that main calls",
        atModule && !mainGuard && sideEffect(fn)
}
