// generated-from: review/typescript/hardcoded-secret/rule.md
// rule-hash: 6b744a1e5687c1355255bd179cd9d39bf8317510a6a8fc6320eff903311a0130
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'hardcoded-secret'

def secretName = { t -> t ==~ /(?i)["'`]?(\w*[-_.])?\w*(password|passwd|secret|api[-_]?key|token|private[-_]?key|client[-_]?secret)\w*["'`]?/ }
def known = { v -> v ==~ /(?s).*(AKIA[0-9A-Z]{16}|-----BEGIN (RSA |EC )?PRIVATE KEY-----|ghp_[A-Za-z0-9]{36}|xox[abp]-|sk_live_[A-Za-z0-9]+|eyJ[A-Za-z0-9_-]{8,}\.eyJ[A-Za-z0-9_-]{8,}\.).*/ }
def placeholder = { v -> v ==~ /(?is).*(changeme|xxx|<[^>]*>|\$\{|example|dummy|fake|sample|your[-_ ]|test).*/ }
// No entropy check: a secret has no spaces, and mixes letters with digits.
def secretLike = { v -> v ==~ /["'`][^\s"'`]{12,}["'`]/ && v ==~ /(?s).*[0-9].*/ && v ==~ /(?s).*[A-Za-z].*/ }

Node.each { n ->
    only n.type in ['string', 'template_string', 'variable_declarator', 'pair', 'assignment_expression', 'public_field_definition']
    skip file.isTest

    // The name and the value of a declaration, a property, an assignment or a class field. The finding names the name, never the value.
    def name = n.type == 'variable_declarator' ? n.field('name') : n.type == 'pair' ? n.field('key')
        : n.type == 'assignment_expression' ? n.field('left') : n.field('name')
    def value = n.type == 'assignment_expression' ? n.field('right') : n.field('value')

    report n, "A secret in a known format is written in the source: it stays in the repository history, so remove it and rotate it",
        n.type in ['string', 'template_string'] && known(n.text) && !placeholder(n.text)
    report n, "${name.text} is a secret written in the source: read it from the environment or a secret store",
        value != null && name != null && value.type in ['string', 'template_string'] && value.descendants('template_substitution').isEmpty()
            && secretName(name.field('property').text) && secretLike(value.text) && !placeholder(value.text) && !known(value.text)
    report n, "${name.text} is a secret written in the source: read it from the environment or a secret store",
        value != null && name != null && value.type in ['string', 'template_string'] && value.descendants('template_substitution').isEmpty()
            && name.type in ['identifier', 'property_identifier', 'string'] && secretName(name.text) && secretLike(value.text) && !placeholder(value.text) && !known(value.text)
}
