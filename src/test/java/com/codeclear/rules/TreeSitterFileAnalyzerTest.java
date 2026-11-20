package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TreeSitterFileAnalyzerTest {

    private static CCTsNode root(String path, String source) {
        return new TreeSitterFileAnalyzer().analyze(path, source, CCChangedLines.all()).tree().root();
    }

    @Test
    void theLanguageComesFromTheExtension() {
        assertThat(TreeSitterFileAnalyzer.languageOf("a/b.py")).isEqualTo("python");
        assertThat(TreeSitterFileAnalyzer.languageOf("a/b.TS")).isEqualTo("typescript");
        assertThat(TreeSitterFileAnalyzer.languageOf("a/b.mjs")).isEqualTo("typescript");
        assertThat(TreeSitterFileAnalyzer.languageOf("a/b.java")).isNull();
        assertThatThrownBy(() -> root("a/b.java", "class A {}")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pythonNodesHaveTypesLinesAndFields() {
        CCTsNode call = root("a.py", "x = 1\nresult = fetch(url)\n").descendants("call").get(0);

        assertThat(call.type()).isEqualTo("call");
        assertThat(call.line()).isEqualTo(2);
        assertThat(call.field("function").text()).isEqualTo("fetch");
        assertThat(call.parent().type()).isEqualTo("assignment");
        assertThat(call.ancestor("module")).isNotNull();
    }

    @Test
    void typescriptIsParsedToo() {
        CCTsNode call = root("a.ts", "async function f() {\n  fetch(u);\n}\n").descendants("call_expression").get(0);

        assertThat(call.field("function").text()).isEqualTo("fetch");
        assertThat(call.ancestor("function_declaration")).isNotNull();
    }

    @Test
    void textIsCutByBytesSoNonAsciiSourceKeepsItsPlace() {
        CCTsNode string = root("a.py", "s = 'héllo wörld'\nname = 'x'\n").descendants("string").get(1);

        assertThat(string.text()).isEqualTo("'x'");
        assertThat(string.line()).isEqualTo(2);
    }

    @Test
    void aSyntaxErrorStillGivesATree() {
        CCTsNode root = root("a.py", "def f(:\n    pass\nx = 1\n");

        assertThat(root.descendants("assignment")).hasSize(1);
    }

    @Test
    void tokensAreTheUnnamedChildren() {
        CCTsNode compare = root("a.py", "a is not b\n").descendants("comparison_operator").get(0);

        assertThat(compare.tokens()).anyMatch(t -> t.startsWith("is"));
    }
}
