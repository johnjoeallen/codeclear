// generated-from: review/python/context-manager-exit-suppression/rule.md
// rule-hash: ce1bebd0cb82eef1e3dfe002bd62440d1471ddf15f7847b52d63307922170feb
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'context-manager-exit-suppression'

def cleanupName = { n -> n in ['close', 'release', 'rollback', 'commit', 'disconnect', 'shutdown', 'flush', 'unlock'] }

// Swallowing an exception in a context manager means the body of the with block fails silently.
Node.each { n ->
    only n.type in ['function_definition', 'with_statement']
    skip file.isTest
    def name = n.type == 'function_definition' ? n.field('name').text : ''
    def exitMethod = name in ['__exit__', '__aexit__']
    def generator = n.type == 'function_definition' && n.parent != null && n.parent.type == 'decorated_definition'
        && n.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?s).*contextmanager.*/ }

    candidate n, "${name}() returns a true value or a computed one, which tells Python to swallow the exception from the with block: return None or False unless suppressing is the whole point",
        exitMethod && n.descendants('return_statement').any { r -> r.children.any { x -> x.type == 'true' || !(x.type in ['false', 'none']) } }
    // "Do not flag": the generator that re-raises, or does not catch at all.
    candidate n, "This @contextmanager catches an exception around its yield and does not re-raise it, so errors in the with block vanish: re-raise, or catch only what is expected",
        generator && n.descendants('try_statement').any { t -> !t.descendants('yield').isEmpty()
            && t.children.any { e -> e.type == 'except_clause' && e.descendants('raise_statement').isEmpty() } }
    candidate n, "Cleanup in ${name}() is not guarded: if it raises, its error replaces the exception that is already in flight, and the real cause is lost. Wrap it in try/except and log",
        exitMethod && n.descendants('call').any { k -> k.field('function').type == 'attribute' && cleanupName(k.field('function').field('attribute').text) && k.ancestors('try_statement').isEmpty() }
    // "Do not flag": suppress of a narrow expected exception (suppress(FileNotFoundError)).
    candidate n, "suppress(Exception) hides every failure of the block it wraps, writes and commits included: name the one exception that is expected",
        n.type == 'with_statement' && n.child('with_clause') != null && n.child('with_clause').text ==~ /(?s).*suppress\(\s*(contextlib\.)?(Base)?Exception\s*\).*/
}
