// generated-from: review/typescript/orm-n-plus-one-unbounded-read/rule.md
// rule-hash: 24c6752b051f5283b1110a242b891c010c27d85ac27cb2edbce2b7eca07f715d
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'orm-n-plus-one-unbounded-read'

def readMethod = { n -> n ==~ /(find|findOne|findMany|findAll|findAndCount|findUnique|findFirst|findOneBy|findBy\w*|findById|query|get|getOne|fetch|load|select)/ }
def ormReceiver = { t -> t ==~ /(?i)(\w+\.)*\w*(prisma|db|knex|repo|repository|manager|entityManager|sequelize|connection|pool|client|model|dataSource)\w*(\.\w+)?/ || t ==~ /[A-Z]\w*/ }

Node.each { n ->
    only n.type in ['call_expression', 'new_expression']
    skip file.isTest
    // "Do not flag": one-time scripts and migrations.
    skip file.path ==~ /(?i).*(\/|^)(scripts?|seeds?|migrations?|bin|cli|tools)\/.*/
    def fn = n.type == 'call_expression' ? n.field('function') : n
    def name = n.type == 'call_expression' && fn.type == 'member_expression' ? fn.field('property').text : (n.type == 'call_expression' ? fn.text : '')
    def f = n.ancestorOf(['function_declaration', 'arrow_function', 'function_expression', 'method_definition'])
    def handler = f != null && f.field('parameters') != null && f.field('parameters').text ==~ /(?s).*\b(req|request|res|ctx|event|message)\b.*/
    // A loop, or a callback of map, forEach and similar, around the query.
    def loop = n.ancestorOf(['for_statement', 'for_of_statement', 'for_in_statement', 'while_statement', 'do_statement', 'function_declaration', 'method_definition'])
    def inLoop = loop != null && loop.type in ['for_statement', 'for_of_statement', 'for_in_statement', 'while_statement', 'do_statement']
    def inCallback = n.ancestors('call_expression').any { k -> k.field('function').type == 'member_expression' && k.field('function').field('property').text in ['map', 'forEach', 'flatMap', 'filter', 'reduce'] }
    def awaited = n.parent != null && n.parent.type == 'await_expression'
    def opts = n.arg(0)
    def bounded = n.type == 'call_expression' && n.args.any { a -> a.type == 'object' && a.text ==~ /(?s).*(take|limit|skip|cursor|first|top|pageSize).*/ }

    candidate n, "${fn.text}() is a query awaited inside a loop or a map: it runs once per element, which is one round trip each (N+1) and gets slower with the size of the list. Fetch them together with where id in (...), include the relation, or a DataLoader",
        n.type == 'call_expression' && awaited && readMethod(name) && fn.type == 'member_expression' && ormReceiver(fn.field('object').text) && (inLoop || inCallback)
            && !(fn.field('object').text ==~ /(?i)(\w+\.)*(map|set|cache|memo|array|list|object)\w*/)
    candidate n, "${fn.text}() reads every row, with no take or limit, on a request path: a table that grows makes this slower and larger until it takes the server down. Add a limit and paging",
        n.type == 'call_expression' && handler && fn.type == 'member_expression' && name in ['findMany', 'findAll', 'find', 'all'] && ormReceiver(fn.field('object').text)
            && !bounded && (n.args.isEmpty() || (opts.type == 'object' && opts.children.isEmpty())) && !(fn.field('object').text ==~ /(?i).*(array|list|items|map|set)\w*/)
    candidate n, "${n.field('constructor').text}() is created inside a function: if the function runs per request, each call opens its own connection pool and exhausts the database's connections. Create it once at module level",
        n.type == 'new_expression' && n.field('constructor').text in ['PrismaClient', 'Pool', 'Sequelize', 'MongoClient', 'DataSource', 'Knex'] && f != null && !(f.field('name') != null && f.field('name').text ==~ /(?i).*(init|setup|start|create|bootstrap|connect|get|build).*/)
}
