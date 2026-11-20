// generated-from: review/python/unsafe-deserialization-or-eval/rule.md
// rule-hash: 700fb5f964cf4e3f2dc0cbd2e3f1ddc1f4364004a34300c4d237e4c529ec031a
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unsafe-deserialization-or-eval'

def safeLoader = { v -> v.text ==~ /(yaml\.)?(C?SafeLoader|BaseLoader|CBaseLoader)/ }
// A string with nothing substituted into it, and a concatenation of such strings.
def constant = { a -> a.type in ['string', 'concatenated_string'] && a.descendants('interpolation').isEmpty() }

call.each { c ->
    skip file.isTest
    def fn = c.field('function').text

    report c, "${fn}() runs code that is inside the data it reads: use json, or a format that cannot carry code",
        fn ==~ /(pickle|cPickle|_pickle|dill|cloudpickle|marshal)\.loads?/ || fn == 'shelve.open'
    // "Do not flag": yaml.safe_load, and yaml.load with a safe Loader (keyword or second argument).
    report c, "yaml.load() without a safe loader builds arbitrary objects from the data: use yaml.safe_load()",
        fn in ['yaml.load', 'yaml.load_all'] && !(c.keyword('Loader') != null && safeLoader(c.keyword('Loader')))
            && !(c.arg(1) != null && safeLoader(c.arg(1)))
    report c, "${fn}() runs whatever is in its argument: it is not a string literal, so anyone who controls it controls the program",
        fn in ['eval', 'exec'] && !c.positional.isEmpty() && !constant(c.arg(0))
}
