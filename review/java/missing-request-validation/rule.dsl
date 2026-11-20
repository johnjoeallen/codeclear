// generated-from: review/java/missing-request-validation/rule.md
// rule-hash: 7a1457f292bc65ca320a38243d33101c5e6874beeef4e2030fa9a9f89a31ddf8
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'missing-request-validation'

def validated = { n -> n in ['Valid', 'Validated'] }
def write = { n -> n in ['PostMapping', 'PutMapping', 'PatchMapping', 'RequestMapping'] }

// Whether the invalid input does harm downstream is not in the file, so this is a candidate for the AI.
Param.each { p ->
    only 'RequestBody' in p.annotations
    skip file.isTest
    def m = p.enclosingMethod
    only m != null && m.inController

    // "Do not flag": a validated parameter, one validated by hand, and a read-only request that is not a write.
    skip p.annotations.any(validated)
    skip m.text ==~ /(?s).*\b\w*[vV]alidator\w*\.validate\w*\(.*/
    only m.annotations.any(write)

    candidate p, "The @RequestBody ${p.name} of ${m.name}() is not validated: add @Valid so invalid input is rejected before it reaches the logic"
}
