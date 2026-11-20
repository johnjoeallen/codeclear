# generated-from: review/java/lazy-association-outside-session/rule.md
# rule-hash: e5730b9c640a7bb9d815684f64f56819098189fd8bcf0240deb673b29877077b
# api-version: 1
# generator: claude-sonnet-5-5   generated: 2026-11-21
rule lazy-entity-serialized
for    m: Method
unless file.isTest
where  m.inController
   and m.returnedEntity exists
   and m.lazyAssociations is not empty
let    osivOff = project.config("spring.jpa.open-in-view") == "false"
require osivOff
unless m.returnsFetched

flag at m.nameNode
     "{m.name}() returns entity {m.returnedEntity} whose lazy associations {m.lazyAssociations} are serialised after the session has closed"
