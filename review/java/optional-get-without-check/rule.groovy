// generated-from: review/java/optional-get-without-check/rule.md
// rule-hash: b714e80c6a48f20f9c06556b6ecb4a14eb4c5ea3cdf17276e2b55d49944bb020
// api-version: 1
// generator: claude-sonnet-5-5   generated: 2026-11-20
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*

// "Do not flag": test sources.
if (file.path ==~ /(^|.*\/)src\/test\/.*/) {
    return []
}

final Set<String> OPTIONAL_TYPES = ['java.util.Optional', 'java.util.OptionalInt',
                                    'java.util.OptionalLong', 'java.util.OptionalDouble'] as Set
final Set<String> GETTERS = ['get', 'getAsInt', 'getAsLong', 'getAsDouble'] as Set
// Used only when the receiver's type cannot be resolved (confidence LOW).
final Set<String> OPTIONAL_PRODUCERS = ['findById', 'findFirst', 'findAny', 'findOne', 'max', 'min',
                                        'reduce', 'findFirstBy', 'findOneBy'] as Set
final CompilationUnit cu = ast as CompilationUnit

// ---- guard predicates -------------------------------------------------------------------------
// v is the text of the Optional variable, e.g. "o" or "this.name". Only variables can be guarded;
// a chained call such as repo.findById(id).get() has no variable and therefore no possible guard.

def isCallOn = { Expression e, String name, String v ->
    e instanceof MethodCallExpr && ((MethodCallExpr) e).nameAsString == name &&
        ((MethodCallExpr) e).arguments.isEmpty() &&
        ((MethodCallExpr) e).scope.map { it.toString() == v }.orElse(false)
}

// presentWhenTrue(c, v): if condition c evaluates to true, v is known to be present.
// presentWhenFalse(c, v): if condition c evaluates to false, v is known to be present.
def presentWhenTrue
def presentWhenFalse
presentWhenTrue = { Expression c, String v ->
    if (c instanceof EnclosedExpr) return presentWhenTrue(((EnclosedExpr) c).inner, v)
    if (isCallOn(c, 'isPresent', v)) return true
    if (c instanceof UnaryExpr && ((UnaryExpr) c).operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT)
        return presentWhenFalse(((UnaryExpr) c).expression, v)
    if (c instanceof BinaryExpr && ((BinaryExpr) c).operator == BinaryExpr.Operator.AND)
        return presentWhenTrue(((BinaryExpr) c).left, v) || presentWhenTrue(((BinaryExpr) c).right, v)
    return false
}
presentWhenFalse = { Expression c, String v ->
    if (c instanceof EnclosedExpr) return presentWhenFalse(((EnclosedExpr) c).inner, v)
    if (isCallOn(c, 'isEmpty', v)) return true
    if (c instanceof UnaryExpr && ((UnaryExpr) c).operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT)
        return presentWhenTrue(((UnaryExpr) c).expression, v)
    if (c instanceof BinaryExpr && ((BinaryExpr) c).operator == BinaryExpr.Operator.OR)
        return presentWhenFalse(((BinaryExpr) c).left, v) || presentWhenFalse(((BinaryExpr) c).right, v)
    return false
}

// A statement after which control never reaches the next statement of the same block.
def completesAbruptly
completesAbruptly = { Statement s ->
    if (s instanceof ReturnStmt || s instanceof ThrowStmt || s instanceof ContinueStmt || s instanceof BreakStmt)
        return true
    if (s instanceof BlockStmt) {
        def list = ((BlockStmt) s).statements
        return !list.isEmpty() && completesAbruptly(list.last())
    }
    return false
}

// True when some guard on v dominates the get call: walk outward from the call to the enclosing
// method, checking each ancestor construct and every earlier statement of each enclosing block.
def isGuarded = { MethodCallExpr call, String v ->
    Node child = call
    while (true) {
        def parentOpt = child.parentNode
        if (!parentOpt.isPresent()) return false
        Node p = parentOpt.get()
        if (p instanceof MethodDeclaration || p instanceof ConstructorDeclaration || p instanceof InitializerDeclaration)
            return false

        if (p instanceof IfStmt) {
            IfStmt i = (IfStmt) p
            if (child.is(i.thenStmt) && presentWhenTrue(i.condition, v)) return true
            if (i.elseStmt.isPresent() && child.is(i.elseStmt.get()) && presentWhenFalse(i.condition, v)) return true
        } else if (p instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) p
            if (child.is(w.body) && presentWhenTrue(w.condition, v)) return true
        } else if (p instanceof ConditionalExpr) {
            ConditionalExpr t = (ConditionalExpr) p
            if (child.is(t.thenExpr) && presentWhenTrue(t.condition, v)) return true
            if (child.is(t.elseExpr) && presentWhenFalse(t.condition, v)) return true
        } else if (p instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) p
            if (child.is(b.right) && b.operator == BinaryExpr.Operator.AND && presentWhenTrue(b.left, v)) return true
            if (child.is(b.right) && b.operator == BinaryExpr.Operator.OR && presentWhenFalse(b.left, v)) return true
        } else if (p instanceof MethodCallExpr) {
            // v.ifPresent(x -> ... v.get() ...): the lambda only runs when v is present.
            MethodCallExpr m = (MethodCallExpr) p
            if (m.nameAsString in ['ifPresent', 'ifPresentOrElse'] && m.scope.map { it.toString() == v }.orElse(false) &&
                !m.arguments.isEmpty() && child.is(m.arguments[0])) return true
        } else if (p instanceof BlockStmt) {
            for (Statement s : ((BlockStmt) p).statements) {
                if (s.is(child)) break
                // if (v.isEmpty()) return/throw/continue/break;   -> v is present afterwards
                if (s instanceof IfStmt) {
                    IfStmt i = (IfStmt) s
                    if (!i.elseStmt.isPresent() && completesAbruptly(i.thenStmt) && presentWhenFalse(i.condition, v)) return true
                }
                if (s instanceof AssertStmt && presentWhenTrue(((AssertStmt) s).check, v)) return true
            }
        }
        child = p
    }
}

// Declared type text of a simple name, searched in the enclosing method and then the class fields.
def declaredType = { NameExpr n, MethodCallExpr call ->
    def name = n.nameAsString
    def method = call.findAncestor(CallableDeclaration)
    def fromMethod = method.map { m ->
        def p = m.findAll(Parameter).find { it.nameAsString == name }
        if (p) return p.type.toString()
        def d = m.findAll(VariableDeclarator).find { it.nameAsString == name }
        return d ? d.type.toString() : null
    }.orElse(null)
    if (fromMethod) return fromMethod
    def f = cu.findAll(FieldDeclaration).collectMany { it.variables }.find { it.nameAsString == name }
    return f ? f.type.toString() : null
}

// ---- detection --------------------------------------------------------------------------------
def results = []
cu.findAll(MethodCallExpr).each { MethodCallExpr call ->
    if (!(call.nameAsString in GETTERS) || !call.arguments.isEmpty() || !call.scope.isPresent()) return
    Expression receiver = call.scope.get()

    String confidence = null   // null = the rule's own confidence (MEDIUM)
    String type = model.typeOf(receiver)
    if (type != null) {
        // Resolved: Supplier, Future, AtomicReference, Map and every other type are not our business.
        if (!(type in OPTIONAL_TYPES)) return
    } else {
        // Unresolved receiver: fall back to naming heuristics and lower the confidence.
        boolean looksOptional = false
        if (receiver instanceof MethodCallExpr && ((MethodCallExpr) receiver).nameAsString in OPTIONAL_PRODUCERS) {
            looksOptional = true
        } else if (receiver instanceof NameExpr) {
            String declared = declaredType(receiver as NameExpr, call)
            looksOptional = declared != null && declared ==~ /Optional(Int|Long|Double)?(<.*>)?/
        }
        if (!looksOptional) return
        confidence = 'LOW'
    }

    boolean guardable = receiver instanceof NameExpr || receiver instanceof FieldAccessExpr
    if (guardable && isGuarded(call, receiver.toString())) return

    int line = call.nameAsExpression.begin.get().line
    results << finding(line, "${call.nameAsString}() on an Optional without a presence check; use orElseThrow(...) or guard it", confidence)
}
return results
