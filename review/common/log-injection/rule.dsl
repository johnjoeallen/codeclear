# generated-from: review/common/log-injection/rule.md
# rule-hash: fbf77d01c58cf0d89bc06f54fcbc432f848dc5a3cf7157cafac72a519cf9df10
# api-version: 1
# generator: claude-sonnet-5-5   generated: 2026-11-21
rule log-injection
for    c: Call
unless file.isTest
where  c.name in ["trace", "debug", "info", "warn", "error"]
   and c.scope exists
   and c.scope.text matches /(?i)(this\.)?(log|logger)/
   and c.args is not empty
unless project.config("logging.structured") == "true"

# policy: what is attacker-controlled, and what neutralises a value
let isReqAnn(a) = a in ["RequestParam", "PathVariable", "RequestBody", "RequestHeader", "ModelAttribute"]
let isSource(n) = (n is Call and n.name in ["getParameter", "getParameterValues", "getHeader", "getQueryString",
                                              "getRequestURI", "getPathInfo"])
               or (n is Param and n.annotations any isReqAnn)
let isSanitized(n) = n is Call
                 and (n.name in ["sanitize", "sanitise", "escape", "encode", "forLog", "stripNewlines",
                                 "parseInt", "parseLong", "parseDouble", "fromString", "valueOf"]
                      or (n.name == "replaceAll" and n.firstArg.text matches /.*\\[rn].*/))

let isTainted(a) = a.tainted(isSource, isSanitized)

flag at c.nameNode when c.args any isTainted
     "Untrusted request value is written to the log without neutralising line breaks"
