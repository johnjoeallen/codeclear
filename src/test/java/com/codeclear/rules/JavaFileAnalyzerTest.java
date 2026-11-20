package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.Test;

/** The analyzer must read the Java the project is written in, up to the version it runs on. */
class JavaFileAnalyzerTest {

    private final JavaFileAnalyzer analyzer = new JavaFileAnalyzer();

    private CCFileAnalysis parse(String source) {
        return analyzer.analyze("A.java", source, CCChangedLines.all());
    }

    @Test
    void varInRecordPatternsParses() {
        assertThat(parse("class A { record P(int x, int y) {} void f(Object o) { if (o instanceof P(var x, var y)) { } } }")
                .ast()).isInstanceOf(CompilationUnit.class);
        parse("class A { record P(int x, int y) {} int f(Object o) { return switch (o) { case P(var x, var y) -> x + y; default -> 0; }; } }");
        parse("class A { record P(int x, int y) {} record Q(P p) {} int f(Object o) { return switch (o) { case Q(P(var x, var y)) -> x; default -> 0; }; } }");
    }

    @Test
    void unnamedVariablesAndPatternsParse() {
        parse("class A { void f() { try { } catch (Exception _) { } } }");
        parse("class A { void f(java.util.List<String> l) { for (var _ : l) { } } }");
        parse("class A { void f() { java.util.function.Function<String,Integer> g = _ -> 1; } }");
        parse("class A { record P(int x, int y) {} void f(Object o) { if (o instanceof P(var x, _)) { } } }");
    }

    @Test
    void java25SyntaxParses() {
        parse("import module java.base;\nclass A { }");
        parse("class A { A(int x) { if (x < 0) throw new IllegalArgumentException(); super(); } }");
    }

    @Test
    void yieldInSwitchExpressionsParses() {
        // JavaParser 3.28.0 failed on all of these at the JAVA_25 level.
        parse("class A { int f(int v, T t) { return switch (v) { case 1 -> { yield t.value; } default -> 0; }; } }");
        parse("class A { int f(int v) { return switch (v) { case 1: yield g(v); default: yield 0; }; } }");
        parse("class A { int f(int v) { return switch (v) { case 1 -> { int x = 1; yield A.X + x; } default -> 0; }; } }");
    }

    @Test
    void ordinaryModernJavaParses() {
        parse("class A { void f() { var x = 1; for (var i = 0; i < 2; i++) { } try (var in = new java.io.StringReader(\"\")) { } catch (Exception e) { } } }");
        parse("sealed interface S permits R {} record R(int a) implements S {} class A { int f(S s) { return switch (s) { case R r -> r.a(); }; } }");
        parse("class A { String s = \"\"\"\n  hi\n  \"\"\"; }");
    }

    @Test
    void aFileTheValidatorRejectsButTheGrammarAcceptsIsStillAnalysed() {
        // javac rejects this, the validator does too, the grammar does not. The review must not lose the file over it.
        CCFileAnalysis a = parse("class A { void f() { var x; g(); } }");

        assertThat(a.ast().findAll(MethodCallExpr.class)).hasSize(1);
    }

    @Test
    void aRealSyntaxErrorNamesTheFileAndTheLine() {
        assertThatThrownBy(() -> parse("class A {\n  void f() {\n    int x = ;\n  }\n}\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot parse A.java:3:");
    }
}
