package com.codeclear.rules.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.JavaFileAnalyzer;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression: a large, highly ambiguous project must not make the taint analysis explode. */
class DslTaintBudgetTest {

    @Test
    void ambiguousChainsFinishQuickly() throws Exception {
        StringBuilder src = new StringBuilder("class Big {\n");
        for (int i = 0; i < 400; i++) {
            // Every method is called "get": each call matches hundreds of candidates, and each returns a call.
            src.append("  String get(String s").append(i).append(") { return get(s").append(i).append(" + \"x\"); }\n");
        }
        src.append("  String get(String a, String b) { return a; }\n");
        src.append("  void f(HttpServletRequest request) {\n    log.info(get(request.getParameter(\"u\")));\n  }\n}\n");
        CCFileAnalysis fa = new JavaFileAnalyzer().analyze("src/main/java/Big.java", src.toString(), CCChangedLines.all());
        DslProject project = new DslProject(List.of((CompilationUnit) fa.ast()), new HashMap<>());
        RuleAst rule = DslParser.parse(Files.readString(Path.of("review/common/log-injection/rule.dsl")));
        var findings = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> new DslInterpreter().run(rule, fa, "MEDIUM", project));
        assertThat(findings).isNotNull(); // finishing within the budget is the point; the result may be empty
    }
}
