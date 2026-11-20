// generated-from: review/python/cache-nonatomic-read-modify-write/rule.md
// rule-hash: 19791b7d6d3f14893be2dcfdaa0f891e9e878bb53e41d500a986c2879a6eb46a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'cache-nonatomic-read-modify-write'

call.each { c ->
    skip file.isTest
    def module = c.ancestor('module')
    only module.mentions('redis') || module.mentions('Redis') || module.mentions('cache') || module.mentions('memcache') || module.mentions('pymemcache')
    def fn = c.field('function')
    only fn.type == 'attribute'
    def scope = c.ancestorOf(['function_definition', 'module'])
    def recv = fn.field('object').text
    def method = fn.field('attribute').text
    def key = c.arg(0) == null ? null : c.arg(0).text
    // "Do not flag": WATCH, a pipeline with transaction=True, a Lua script, or a lock around the pair.
    def guarded = scope.mentions('watch') || scope.mentions('pipeline') || scope.mentions('eval') || scope.mentions('evalsha') || scope.mentions('lock') || scope.mentions('register_script') || scope.mentions('incr') || scope.mentions('hincrby')

    candidate c, "${recv}.set(${key}, ...) writes a value computed from an earlier ${recv}.get(${key}): another process can change the key in between, and its update is lost. Use INCR, a Lua script, or WATCH",
        method == 'set' && key != null && scope.hasCallOn(recv, 'get', key) && !guarded && c.keyword('nx') == null
    candidate c, "${recv}.expire(${key}, ...) after ${recv}.set(${key}, ...) is two steps: a crash between them leaves a key that never expires. Use set(..., ex=seconds) or setex",
        method == 'expire' && key != null && scope.hasCallOn(recv, 'set', key)
}
