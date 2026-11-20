// generated-from: review/java/test-without-meaningful-assertion/rule.md
// rule-hash: 52c38db63bc651fa55e9cdb6c79a0893ae647a505af3cd427baeb476d8dbe36a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-21
rule 'test-without-meaningful-assertion'

def isTest = { a -> a in ['Test', 'ParameterizedTest', 'RepeatedTest'] }
// All that these assert is that something exists.
def isWeak = { x -> x.name in ['assertNotNull', 'isNotNull'] }

Method.each { m ->
    only m.annotations.any(isTest)
    // "Do not flag": a test that expects an exception through the annotation, and the context-loads smoke test.
    skip m.hasAnnotationMember('Test', 'expected') || m.name == 'contextLoads'

    // The host lists the assertion calls: anything named assert..., verify..., check..., expect..., andExpect..., fail, then
    // or should. That includes helpers that delegate to assertions, and assertThrows / assertDoesNotThrow. Each call is weak
    // (only checks that something exists), so a test with none, or only weak ones, asserts nothing that can fail.
    report m.nameNode, "Test ${m.name}() has no assertion that can fail: it has none, or only checks that something is not null",
        m.assertions.every(isWeak)
}
