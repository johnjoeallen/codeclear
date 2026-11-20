// generated-from: review/python/celery-subtask-wait-starvation/rule.md
// rule-hash: b3cf76a292fd11f6b8aa2589a51edd1b32e00f05e255ab3db5a2519a3d3c0713
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'celery-subtask-wait-starvation'

def isTask = { f -> f.parent != null && f.parent.type == 'decorated_definition' && f.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?s).*(app\.task|shared_task|@task|\.task\b).*/ } }

// A task that waits for another task holds a worker, and when every worker waits the queue deadlocks.
call.each { c ->
    skip file.isTest
    def fn = c.field('function')
    only fn.type == 'attribute' && fn.field('attribute').text in ['get', 'join', 'collect']
    def task = c.ancestors('function_definition').any(isTask)
    only task
    def obj = fn.field('object')
    def scope = c.ancestor('function_definition')
    // The wait is on a result of delay or apply_async, or on a group or chord.
    def direct = obj.type == 'call' && obj.field('function').text ==~ /(.*\.(delay|apply_async|s|si)|group|chord)(\(.*\))?/ || (obj.type == 'call' && obj.text ==~ /(?s)\s*(group|chord)\(.*\)\(.*\)\s*/)
    def viaVariable = obj.type == 'identifier' && scope.descendants('assignment').any { a -> a.field('left').text == obj.text && a.field('right').text ==~ /(?s).*\.(delay|apply_async)\(.*|(?s)\s*(group|chord)\(.*/ }

    candidate c, "${fn.text}() blocks this worker until another task finishes: if every worker is waiting like this, none is left to run the tasks they wait for, and the queue deadlocks. Chain the tasks (chain, chord callback) instead",
        direct || viaVariable
    candidate c, "disable_sync_subtasks=False turns off the check that stops a task from waiting on a task: this is a deadlock waiting for load",
        c.keyword('disable_sync_subtasks') != null && c.keyword('disable_sync_subtasks').type == 'false'
}
