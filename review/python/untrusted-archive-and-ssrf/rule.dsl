// generated-from: review/python/untrusted-archive-and-ssrf/rule.md
// rule-hash: 2d845592b5db4ed462d341e7e00e20ebae4e4ccb47b008a3e0a357bbb4ff4f64
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'untrusted-archive-and-ssrf'

def fromRequest = { s -> s.mentions('request') || s.mentions('upload') || s.mentions('files') || s.mentions('file') }
def getter = { t -> t ==~ /(requests|httpx)\.(get|post|put|patch|delete|head|request)/ || t ==~ /\w*(session|client)\w*\.(get|post|put|patch|delete|head|request)/ || t in ['urlopen', 'urllib.request.urlopen', 'request.urlopen'] }

call.each { c ->
    skip file.isTest
    def fn = c.field('function').text
    def scope = c.ancestorOf(['function_definition', 'module'])
    def name = c.field('function').type == 'attribute' ? c.field('function').field('attribute').text : fn
    // The URL (or host) comes from the request: directly, or through a variable assigned from it in this function.
    def a0 = c.arg(0)
    def derived = a0 != null && a0.derivedFrom('request', scope)

    // "Do not flag": a tar extraction with a safe filter, and a function that checks hosts against an allow-list.
    candidate c, "${name}() extracts an archive from an upload or request without a filter: a crafted entry such as ../../etc/cron.d/x writes outside the target folder. Use filter='data' (tarfile), or check each member's path",
        ((name in ['extractall', 'extract'] && (c.ancestor('module').mentions('tarfile') || c.ancestor('module').mentions('zipfile')))
            || fn == 'shutil.unpack_archive') && fromRequest(scope) && !(c.keyword('filter') != null)
    candidate c, "${fn}() fetches a URL that comes from the request, and follows redirects: a caller can reach internal services and cloud metadata. Check the host against an allow-list, resolve it, and refuse private addresses",
        getter(fn) && derived && !(scope.mentions('allowlist') || scope.mentions('allowed_hosts') || scope.mentions('ALLOWED_HOSTS') || scope.mentions('allowed') || scope.mentions('is_private'))
            && !(c.keyword('allow_redirects') != null && c.keyword('allow_redirects').type == 'false')
}
