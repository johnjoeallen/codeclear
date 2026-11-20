// generated-from: review/java/empty-catch-block.md
// rule-hash: f048993f9cff87c64bd101e2a389c4cb36615f2ab41d78c2a934d89510222587
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement

// "Do not flag": test sources.
if (file.path ==~ /(^|.*\/)src\/test\/.*/) {
    return []
}

final Set<String> IGNORED_NAMES = ['ignored', 'ignore', 'expected'] as Set
final Set<String> QUIET_LEVELS = ['debug', 'trace'] as Set
final CompilationUnit cu = ast as CompilationUnit

// A statement that is only a DEBUG/TRACE log call: log.debug(...), logger.trace(...), LOG.debug(...)
def isQuietLogCall = { Statement s ->
    if (!(s instanceof ExpressionStmt)) return false
    def expr = ((ExpressionStmt) s).expression
    if (!(expr instanceof MethodCallExpr)) return false
    MethodCallExpr call = (MethodCallExpr) expr
    return call.nameAsString in QUIET_LEVELS &&
           call.scope.map { it.toString() ==~ /(?i)(this\.)?log(ger)?/ }.orElse(false)
}

def results = []
cu.findAll(CatchClause).each { CatchClause c ->
    // "Do not flag": catch parameter named ignored / ignore / expected.
    if (c.parameter.nameAsString in IGNORED_NAMES) return

    def body = c.body
    def stmts = body.statements

    // "Do not flag": a comment inside the block explains why the exception is ignored.
    boolean hasComment = !body.allContainedComments.isEmpty()
    if (hasComment) return

    boolean empty = stmts.isEmpty()
    // Only DEBUG/TRACE logging: no rethrow, return or fallback value is possible, because any such
    // statement is not a quiet log call and makes this false.
    boolean quietLogOnly = !empty && stmts.every { isQuietLogCall(it) }

    if (empty || quietLogOnly) {
        int line = c.begin.get().line
        String what = empty ? 'Empty catch block' : 'Catch block only logs at DEBUG/TRACE'
        results << finding(line, "${what} hides ${c.parameter.type} from the caller")
    }
}
return results
