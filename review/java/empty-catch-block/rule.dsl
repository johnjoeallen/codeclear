# generated-from: review/java/empty-catch-block/rule.md
# rule-hash: f048993f9cff87c64bd101e2a389c4cb36615f2ab41d78c2a934d89510222587
# api-version: 1
# generator: claude-sonnet-5-5   generated: 2026-11-21
rule empty-catch-block
for    c: Catch
unless file.isTest
unless c.param.name in ["ignored", "ignore", "expected"]
unless c.body.hasComment

let quiet(s) = s is ExprStmt
           and s.expression is Call
           and s.expression.name in ["debug", "trace"]
           and s.expression.scope.text matches /(?i)(this\.)?log(ger)?/

flag at c   when c.body.statements is empty
     "Empty catch block hides {c.param.type} from the caller"
flag at c   when c.body.statements all quiet
     "Catch block only logs at DEBUG/TRACE, hiding {c.param.type} from the caller"
