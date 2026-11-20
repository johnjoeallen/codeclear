// generated-from: review/java/missing-network-timeout/rule.md
// rule-hash: 0bd3c058a367f64cc10c21652594d6d9ef683c0870aed786238e83572a71b7d6
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'missing-network-timeout'

// Timeouts set anywhere in the same class or factory count: that is where a client is configured.
def configured = { k -> k.enclosingClass != null && k.enclosingClass.text ==~ /(?s).*(setConnectTimeout|setReadTimeout|connectTimeout|readTimeout|setSoTimeout|responseTimeout|ReadTimeoutHandler|\.timeout\(|RestTemplateBuilder|setConnectionRequestTimeout|callTimeout).*/ }

Creation.each { x ->
    skip file.isTest
    skip configured(x)

    candidate x, "new RestTemplate() has no connect or read timeout, so a stalled server blocks the thread forever: use RestTemplateBuilder with timeouts",
        x.typeName == 'RestTemplate' && x.args.isEmpty()
    candidate x, "HttpClient has no connectTimeout, and a request has no timeout: a stalled server blocks the thread forever",
        x.name in ['newHttpClient'] && x.scope.text == 'HttpClient'
    candidate x, "This HttpClient builder never sets connectTimeout: a stalled server blocks the thread forever",
        x.name == 'newBuilder' && x.scope.text == 'HttpClient' && !(x.enclosingStatement.text ==~ /(?s).*connectTimeout.*/)
    candidate x, "new Socket(host, port) connects with no timeout: use connect(address, timeout) and setSoTimeout",
        x.typeName == 'Socket' && x.args.size().isEqualTo(2)
    candidate x, "openConnection() has no connect or read timeout: call setConnectTimeout and setReadTimeout",
        x.name == 'openConnection' && x.args.isEmpty()
    candidate x, "This WebClient sets no response timeout: a stalled server holds the request forever",
        x.scope.text == 'WebClient' && x.name in ['create', 'builder']
    candidate x, "${x.name}() waits with no timeout: a future that never completes blocks the thread forever, so pass a timeout",
        x.name in ['get', 'await'] && x.args.isEmpty() && x.scope.declaredType ==~ /(Future|CompletableFuture|Condition|ScheduledFuture|FutureTask)(<.*>)?/
}
