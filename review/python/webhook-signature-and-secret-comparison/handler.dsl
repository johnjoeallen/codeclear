// generated-from: review/python/webhook-signature-and-secret-comparison/rule.md
// rule-hash: 92b7c3f3231f240993da34000b678e163f85eff55f4d2403aa4730eaa6846204
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'webhook-signature-and-secret-comparison'

function_definition.each { f ->
    only f.field('name').text ==~ /(?i).*(webhook|callback|hook).*/ || (f.parent != null && f.parent.type == 'decorated_definition' && f.parent.children.any { d -> d.type == 'decorator' && d.text ==~ /(?is).*(webhook|callback).*/ })
    skip file.isTest
    // The handler reads the body ...
    only f.mentions('request') && (f.mentions('body') || f.mentions('data') || f.mentions('json') || f.mentions('get_data') || f.mentions('get_json') || f.mentions('form') || f.mentions('stream'))
    // "Do not flag": a verification call (compare_digest, a provider's construct_event or verify) and a timestamp check.
    skip f.mentions('compare_digest') || f.mentions('construct_event') || f.mentions('verify') || f.mentions('verify_signature') || f.mentions('validate_signature') || f.mentions('verify_webhook')
    skip f.text ==~ /(?is).*timestamp.*/

    candidate f.field('name'), "${f.field('name').text}() reads a webhook body but never verifies a signature: anyone who knows the URL can post events to it"
}
