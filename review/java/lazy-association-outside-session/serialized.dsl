# generated-from: review/java/lazy-association-outside-session/rule.md
# rule-hash: a5e2ee3664def61876276a1567b999e5168f63921427bc90bfcea8704e199f6b
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
