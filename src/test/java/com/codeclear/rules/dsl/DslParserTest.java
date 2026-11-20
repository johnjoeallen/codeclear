package com.codeclear.rules.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** The surface syntax: layout, comments, closures, and the errors a developer sees for things rules cannot do. */
class DslParserTest {

    private static final String MULTI = """
            // header comment
            rule 'x'   /* block comment */
            def quiet = { s -> s instanceof ExprStmt }   # legacy comment
            Catch.each { c ->
                only c.param.name in ['a', 'b']
                skip file.isTest
                report c, "msg ${c.param.type}", c.body.statements.every(quiet), confidence: LOW
            }
            """;

    /** Every committed .dsl file must parse with the current syntax, whether or not it has tests yet. */
    @Test
    void everyCommittedDslFileUsesTheCurrentSyntax() throws java.io.IOException {
        try (var files = java.nio.file.Files.walk(java.nio.file.Path.of("review"))) {
            var dsl = files.filter(f -> f.toString().endsWith(".dsl")).sorted().toList();
            assertThat(dsl).as(".dsl files under review/").isNotEmpty();
            for (var f : dsl) {
                String source = java.nio.file.Files.readString(f);
                assertThat(source).as(f + " starts with a rule 'name' line").containsPattern("(?m)^rule '[^']+'");
                // The old syntax had these line-leading keywords, which the current language does not have.
                assertThat(source).as(f + " has no old-syntax clause").doesNotContainPattern("(?m)^(for |where |unless |require |let |flag |candidate at )");
                DslParser.parse(source);
            }
        }
    }

    @Test
    void theOldSyntaxIsNoLongerAccepted() {
        assertThatThrownBy(() -> DslParser.parse("rule x\nfor c: Catch\nunless file.isTest\nflag at c \"m\""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void layoutDoesNotMatter() {
        String oneLine = "rule 'x' def quiet = { s -> s instanceof ExprStmt } Catch.each { c -> only c.param.name in ['a','b'] "
                + "skip file.isTest report c, \"msg ${c.param.type}\", c.body.statements.every(quiet), confidence: LOW }";

        assertThat(DslParser.parse(oneLine).toString()).isEqualTo(DslParser.parse(MULTI).toString());
    }

    @Test
    void parsesTheStructureOfARule() {
        RuleAst rule = DslParser.parse(MULTI);

        assertThat(rule.id()).isEqualTo("x");
        assertThat(rule.var()).isEqualTo("c");
        assertThat(rule.kind()).isEqualTo("Catch");
        assertThat(rule.clauses()).hasSize(3); // def quiet, only, skip
        assertThat(rule.flags()).hasSize(1);
        assertThat(rule.flags().get(0).confidence()).isEqualTo(new Expr.Id("low"));
        assertThat(rule.flags().get(0).candidate()).isFalse();
    }

    @Test
    void anInlineClosureBecomesANamedPredicate() {
        RuleAst rule = DslParser.parse("rule 'x' Call.each { c -> report c, 'm', c.args.any { a -> a.text == 'x' } }");

        assertThat(rule.clauses()).hasSize(1);
        assertThat(rule.clauses().get(0)).isInstanceOf(RuleAst.Clause.Let.class);
        assertThat(((RuleAst.Clause.Let) rule.clauses().get(0)).param()).isEqualTo("a");
    }

    @Test
    void aClosureWithoutAParameterUsesIt() {
        RuleAst rule = DslParser.parse("rule 'x' def p = { it instanceof Call } Call.each { c -> report c, 'm' }");

        assertThat(((RuleAst.Clause.Let) rule.clauses().get(0)).param()).isEqualTo("it");
    }

    @Test
    void nullComparisonsBecomeExistenceTests() {
        RuleAst rule = DslParser.parse("rule 'x' Call.each { c -> only c.scope != null skip c.scope == null report c, 'm' }");

        assertThat(rule.clauses().get(0)).isEqualTo(new RuleAst.Clause.Where(
                new Expr.Exists(new Expr.Field(new Expr.Id("c"), "scope"))));
        assertThat(rule.clauses().get(1)).isEqualTo(new RuleAst.Clause.Unless(
                new Expr.Not(new Expr.Exists(new Expr.Field(new Expr.Id("c"), "scope")))));
    }

    @Test
    void aCandidateIsMarkedAsOne() {
        assertThat(DslParser.parse("rule 'x' Call.each { c -> candidate c, 'evidence' }").flags().get(0).candidate()).isTrue();
    }

    @Test
    void messagesInterpolateExpressions() {
        RuleAst rule = DslParser.parse("rule 'x' Call.each { c -> report c, \"a ${c.name} b\" }");

        assertThat(rule.flags().get(0).message()).hasSize(3);
        assertThat(rule.flags().get(0).message().get(1)).isEqualTo(new Expr.Field(new Expr.Id("c"), "name"));
    }

    @Test
    void conditionsAndConfidenceMayBeInEitherOrder() {
        RuleAst a = DslParser.parse("rule 'x' Call.each { c -> report c, 'm', c.args.isEmpty(), confidence: LOW }");
        RuleAst b = DslParser.parse("rule 'x' Call.each { c -> report c, 'm', confidence: LOW, c.args.isEmpty() }");

        assertThat(a.flags().get(0).toString()).isEqualTo(b.flags().get(0).toString());
    }

    @Test
    void thingsARuleCannotDoAreRefusedWithAnExplanation() {
        for (String construct : new String[]{"if (c.name == 'x') { }", "for (x in c.args) { }", "while (true) { }", "return"}) {
            assertThatThrownBy(() -> DslParser.parse("rule 'x' Call.each { c -> " + construct + " report c, 'm' }"))
                    .as(construct).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not supported")
                    .hasMessageContaining("only, skip, any");
        }
    }

    @Test
    void filtersMustComeBeforeTheFirstReport() {
        assertThatThrownBy(() -> DslParser.parse("rule 'x' Call.each { c -> report c, 'm' skip file.isTest }"))
                .hasMessageContaining("'skip' must come before the first report");
    }

    @Test
    void aRuleNeedsAReport() {
        assertThatThrownBy(() -> DslParser.parse("rule 'x' Call.each { c -> skip file.isTest }"))
                .hasMessageContaining("at least one report");
    }

    @Test
    void interpolationIsOnlyAllowedInAMessage() {
        assertThatThrownBy(() -> DslParser.parse("rule 'x' Call.each { c -> only c.name == \"${c.name}\" report c, 'm' }"))
                .hasMessageContaining("only allowed in a report message");
    }

    @Test
    void errorsNameTheLine() {
        assertThatThrownBy(() -> DslParser.parse("rule 'x'\nCall.each { c ->\n  only c.name in\n}"))
                .hasMessageContaining("line ");
    }

    @Test
    void aRegexIsNotConfusedWithAComment() {
        RuleAst rule = DslParser.parse("rule 'x' Call.each { c -> only c.name ==~ /a\\/b.*/ report c, 'm' }");

        assertThat(rule.clauses().get(0).toString()).contains("a/b.*");
    }
}
