# generated-from: review/java/lazy-association-outside-session/rule.md
# rule-hash: a5e2ee3664def61876276a1567b999e5168f63921427bc90bfcea8704e199f6b
# api-version: 1
# generator: claude-sonnet-5-5   generated: 2026-11-21
rule lazy-association-outside-session
for    c: Call
unless file.isTest
where  c.lazyAssociation exists
   and c.dereferenced
let    osivOff = project.config("spring.jpa.open-in-view") == "false"
unless c.scope.fetched

flag at c.nameNode when not c.inTransaction and osivOff
     "Lazy association '{c.lazyAssociation}' is read outside any transaction and open-in-view is disabled"
flag at c.nameNode when c inside Loop
     "Lazy association '{c.lazyAssociation}' is read in a loop: one query per iteration (N+1)"
