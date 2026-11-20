// generated-from: review/typescript/unstable-pagination-order/rule.md
// rule-hash: cd74044e393e4417b90b4f5c2e543a3499210806149b1b2e4ed61f68efc20b8f
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unstable-pagination-order'

def paged = { o -> o.type == 'object' && o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?(take|limit|skip|offset)["']?/ } }
def hasOrder = { o -> o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?(order|orderBy|sort)["']?/ } || o.children.any { p -> p.type == 'spread_element' } }
def orderPair = { o -> o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?(order|orderBy|sort)["']?/ } }
// "Do not flag": limit 1, which is a pick of one row and not a page.
def single = { o -> o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?(take|limit)["']?/ && p.field('value').text == '1' } }
// An ordering by columns that are not unique, with no id as a tiebreaker.
def weakOrder = { o -> o.children.any { p -> p.type == 'pair' && p.field('key').text ==~ /["']?(order|orderBy|sort)["']?/
    && p.field('value').text ==~ /(?is).*\b(createdAt|created_at|updatedAt|updated_at|status|name)\b.*/ && !(p.field('value').text ==~ /(?is).*\b(id|uuid)\b.*/) } }

Node.each { n ->
    only n.type in ['call_expression', 'string', 'template_string']
    skip file.isTest
    // A query call with a paging option object: the options are the arguments.
    def opts = n.args.any { a -> paged(a) && !single(a) && !hasOrder(a) }
    def weak = n.args.any { a -> paged(a) && !single(a) && orderPair(a) && weakOrder(a) }
    // A chained query: .limit(10) or .take(10) with no ordering call in the same statement.
    def stmt = n.ancestorOf(['expression_statement', 'lexical_declaration', 'return_statement', 'variable_declaration'])
    def chained = n.type == 'call_expression' && n.field('function').type == 'member_expression'
        && n.field('function').field('property').text in ['limit', 'take', 'skip', 'offset'] && n.arg(0) != null && n.arg(0).text != '1'
        && stmt != null && !(stmt.text ==~ /(?s).*\.(orderBy|orderByRaw|sort|order|orderByAsc|orderByDesc)\s*\(.*/)

    report n, "This query is paged with no ordering, so the database may return the rows in a different order each time, and pages repeat or skip rows: add an order, ending in a unique column such as id",
        opts || chained
    report n, "This query is ordered only by a column that is not unique, so rows with the same value change places between pages: add id as a tiebreaker",
        weak
    // Raw SQL in a string.
    report n, "This SQL pages with LIMIT but has no ORDER BY, so the page order is not defined: add ORDER BY with a unique column",
        n.type in ['string', 'template_string'] && n.text ==~ /(?is)\s*["'`]\s*select\b.*\blimit\b\s+(?!1\b)\S.*/ && !(n.text ==~ /(?is).*\border\s+by\b.*/)
    report n, "This SQL orders only by a column that is not unique, so rows with the same value change places between pages: add id as a tiebreaker",
        n.type in ['string', 'template_string'] && n.text ==~ /(?is)\s*["'`]\s*select\b.*\border\s+by\s+(created_at|updated_at|status|name)\b[^,]*\blimit\b.*/ && !(n.text ==~ /(?is).*order\s+by[^;]*\bid\b.*/)
}
