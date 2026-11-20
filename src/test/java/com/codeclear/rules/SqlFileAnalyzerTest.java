package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlFileAnalyzerTest {

    private static CCSqlModel model(String sql) {
        return new SqlFileAnalyzer().analyze("a.sql", sql, CCChangedLines.all()).sql();
    }

    @Test
    void statementsEndAtASemicolonOrAGoLineAndKeepTheirLine() {
        CCSqlModel m = model("SELECT 1;\n\nSELECT 2\nGO\nSELECT 3");

        assertThat(m.statements()).extracting(CCSqlModel.Statement::line).containsExactly(1, 3, 5);
    }

    @Test
    void commentsAndStringContentsAreRemovedFromCode() {
        CCSqlModel m = model("-- c1\nSELECT 'a;b' /* x; */ FROM t; -- c2\n");

        assertThat(m.statements()).hasSize(1);
        assertThat(m.statements().get(0).text()).contains("'a;b'").doesNotContain("x;");
        assertThat(m.statements().get(0).code()).doesNotContain("a;b");
        assertThat(m.statements().get(0).precedingComment()).contains("c1");
    }

    @Test
    void aDollarQuotedBodyIsOneStatementWhoseCodeIsStillScanned() {
        CCSqlModel m = model("CREATE FUNCTION f() RETURNS void AS $$\nBEGIN\n  EXECUTE 'x';\nEND;\n$$ LANGUAGE plpgsql;\nSELECT 1;");

        assertThat(m.statements()).hasSize(2);
        assertThat(m.lines()).extracting(CCSqlModel.Line::text).contains("EXECUTE 'x';");
    }

    @Test
    void aWhereInsideParenthesesIsNotTopLevel() {
        CCSqlModel m = model("UPDATE t SET a = (SELECT b FROM u WHERE u.id = 1);\nUPDATE t SET a = 1 WHERE id = 2;");

        assertThat(m.statements()).extracting(CCSqlModel.Statement::hasTopLevelWhere).containsExactly(false, true);
    }

    @Test
    void anAlterTableIsCutIntoItsAddedColumns() {
        CCSqlModel m = model("ALTER TABLE t\n  ADD COLUMN a INT NOT NULL,\n  ADD b VARCHAR(5, 2),\n  ADD CONSTRAINT c CHECK (a > 0),\n  DROP COLUMN d;");

        assertThat(m.columns()).extracting(CCSqlModel.Column::name).containsExactly("a", "b");
        assertThat(m.columns()).extracting(CCSqlModel.Column::line).containsExactly(2, 3);
    }

    @Test
    void aTableCreatedInTheFileIsRecognisedWithOrWithoutASchema() {
        CCSqlModel m = model("CREATE TABLE IF NOT EXISTS s.t (a INT);\nUPDATE t SET a = 1;\nUPDATE other SET a = 1;");

        assertThat(m.statements()).extracting(CCSqlModel.Statement::tableCreatedInFile).containsExactly(false, true, false);
    }
}
