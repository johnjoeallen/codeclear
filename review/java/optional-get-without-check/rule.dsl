# generated-from: review/java/optional-get-without-check/rule.md
# rule-hash: b714e80c6a48f20f9c06556b6ecb4a14eb4c5ea3cdf17276e2b55d49944bb020
# api-version: 1
# generator: claude-sonnet-5-5   generated: 2026-11-21
rule optional-get-without-check
for    c: Call
where  c.name in ["get", "getAsInt", "getAsLong", "getAsDouble"]
   and c.args is empty
   and c.scope exists
unless file.isTest

let recv     = c.scope
let resolved = recv.type exists
let guess    = (recv is Call and recv.name in ["findById", "findFirst", "findAny", "findOne",
                                               "max", "min", "reduce", "findFirstBy", "findOneBy"])
            or (recv is Name and recv.declaredType matches /Optional(Int|Long|Double)?(<.*>)?/)

# type known: only Optional types count. Type unknown: fall back to names.
require resolved => recv.type in ["java.util.Optional", "java.util.OptionalInt",
                                  "java.util.OptionalLong", "java.util.OptionalDouble"]
require resolved or guess

unless (recv is Name or recv is Field) and c.guarded(recv, present)

flag at c.nameNode  confidence (resolved ? default : low)
     "{c.name}() on an Optional without a presence check; use orElseThrow(...) or guard it"
