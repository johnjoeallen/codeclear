// generated-from: review/java/unbounded-findall/rule.md
// rule-hash: 3baeec3941c83514d27784b36e20885095a51c5dd53e3db57c6dd6d0297fa401
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
rule 'unbounded-findall'

// A repository whose name says it holds a small reference table: findAll() on it is fine.
def referenceTable = { t -> t ==~ /.*(Type|Status|Config)Repository/ }
// A file read from a classpath resource or a configuration file is small by nature.
def smallSource = { a -> a != null && a.text ==~ /(?is).*(getResource|ClassPathResource|ClassLoader|config|\.properties|\.ya?ml).*/ }

Call.each { c ->
    only c.args.isEmpty() || c.scope.text == 'Files'
    skip file.isTest

    candidate c, "findAll() with no paging loads every row into memory: the table may grow without bound",
        c.name == 'findAll' && c.args.isEmpty() && c.scope.declaredType ==~ /.*Repository/ && !referenceTable(c.scope.declaredType)
    candidate c, "${c.scope.text}.${c.name}() reads the whole stream into memory: the input may be large",
        c.name == 'readAllBytes' && c.args.isEmpty() && c.scope.declaredType in ['InputStream', 'FileInputStream', 'BufferedInputStream']
    candidate c, "Files.${c.name}() reads the whole file into memory: the file may be large",
        c.scope.text == 'Files' && c.name in ['readAllBytes', 'readString', 'readAllLines'] && !smallSource(c.firstArg)
}
