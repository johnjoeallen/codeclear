package com.codeclear.rules.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.JavaFileAnalyzer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Numbers, {@code size()} and the comparison methods: {@code x.args.size().isGreaterThan(1)}. */
class DslComparisonTest {

    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();

    /** Calls f() .. f(1, 2, 3) sit on lines 2 to 5; the result is the flagged ones as "1" (no arguments) to "4" (three). */
    private static List<String> flagged(String condition) {
        String rule = "rule 'x'\nCall.each { c -> only c.name == 'f' report c, 'm', " + condition + " }";
        String source = "class C { void t() {\n f();\n f(1);\n f(1, 2);\n f(1, 2, 3);\n } }";
        return new DslInterpreter().run(DslParser.parse(rule), ANALYZER.analyze("C.java", source, CCChangedLines.all()), "MEDIUM")
                .stream().map(f -> String.valueOf(f.line() - 1)).toList();
    }

    @Test
    void equalNotEqualGreaterAndLess() {
        assertThat(flagged("c.args.size().isEqualTo(2)")).containsExactly("3");
        assertThat(flagged("c.args.size().isNotEqualTo(2)")).containsExactly("1", "2", "4");
        assertThat(flagged("c.args.size().isNotEqual(2)")).containsExactly("1", "2", "4");
        assertThat(flagged("c.args.size().isGreaterThan(1)")).containsExactly("3", "4");
        assertThat(flagged("c.args.size().isGreaterThanOrEqualTo(1)")).containsExactly("2", "3", "4");
        assertThat(flagged("c.args.size().isLessThan(2)")).containsExactly("1", "2");
        assertThat(flagged("c.args.size().isLessThanOrEqualTo(2)")).containsExactly("1", "2", "3");
    }

    @Test
    void aNumberCanBeComparedWithEqualsAndCombined() {
        assertThat(flagged("c.args.size() == 3")).containsExactly("4");
        assertThat(flagged("c.args.size().isGreaterThan(0) && c.args.size().isLessThan(3)")).containsExactly("2", "3");
    }

    @Test
    void theSizeOfAMissingValueIsZeroAndAMissingNumberComparesFalse() {
        assertThat(flagged("c.scope.text.size().isEqualTo(0)")).containsExactly("1", "2", "3", "4");
        assertThat(flagged("c.scope.size().isEqualTo(0)")).hasSize(4);
        assertThat(flagged("c.firstArg.isEqualTo(1)")).isEmpty();
    }

    @Test
    void theSizeOfAStringIsItsLength() {
        assertThat(flagged("c.name.size().isEqualTo(1)")).hasSize(4);
    }

    @Test
    void aNumberTooLargeForALongIsAnError() {
        assertThatThrownBy(() -> DslParser.parse("rule 'x' Call.each { c -> only c.args.size().isEqualTo(99999999999999999999) report c, 'm' }"))
                .hasMessageContaining("too large");
    }
}
