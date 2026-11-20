// generated-from: review/java/missing-endpoint-authorization/rule.md
// rule-hash: 6d0fac87c9f0ab671169067d8af6dfde6f330f7b4217d3ec4acd9ce33a54d36f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'missing-endpoint-authorization'

def mapping = { n -> n in ['GetMapping', 'PostMapping', 'PutMapping', 'DeleteMapping', 'PatchMapping', 'RequestMapping'] }
def guard = { n -> n in ['PreAuthorize', 'PostAuthorize', 'Secured', 'RolesAllowed', 'PermitAll', 'DenyAll'] }

// Whether the endpoint should be restricted, and where access is enforced, is not in the method, so this is a candidate for the AI.
Annotation.each { a ->
    only mapping(a.name)
    skip file.isTest
    // Only a mapping on a method of a controller, not a class-level @RequestMapping.
    def m = a.enclosingMethod
    only m != null && m.inController

    // "Do not flag": authorization on the method or the class, and an explicit principal or role check in the body.
    skip m.annotations.any(guard) || a.enclosingClassAnnotations.any(guard)
    skip m.text ==~ /(?s).*(isUserInRole|hasRole|hasAuthority|getAuthentication|getUserPrincipal|SecurityContextHolder|@AuthenticationPrincipal).*/
    // "Do not flag": public endpoints by design.
    skip a.text ==~ /(?is).*"[^"]*(health|login|logout|public|actuator|swagger|api-docs|ping)[^"]*".*/

    candidate a, "${m.name}() is a request mapping with no authorization annotation or check: any caller can reach it"
}
