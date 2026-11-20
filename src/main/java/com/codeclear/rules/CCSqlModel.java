package com.codeclear.rules;

import java.util.List;

/**
 * What the rules see of a SQL file: its statements, the column definitions of its {@code ALTER TABLE ... ADD}, and its
 * code lines. Comments are removed from {@code text}; {@code code} also blanks the contents of string literals, so a
 * keyword or an operator inside a string is not mistaken for code.
 */
public record CCSqlModel(List<Statement> statements, List<Column> columns, List<Line> lines) {

    /** A node with a position, so a rule can report on it. */
    public sealed interface Node permits Statement, Column, Line {
        int line();
    }

    /**
     * One statement, ending at a {@code ;} or a {@code GO} line.
     *
     * @param precedingComment    the comments between the previous statement and this one
     * @param hasTopLevelWhere    a {@code WHERE} outside parentheses
     * @param tableCreatedInFile  the table it changes is created by a {@code CREATE TABLE} in the same file
     */
    public record Statement(int line, String text, String code, String precedingComment, boolean hasTopLevelWhere,
                            boolean tableCreatedInFile) implements Node { }

    /** A column added by an {@code ALTER TABLE ... ADD [COLUMN] definition}. */
    public record Column(int line, String name, String text, String code, boolean tableCreatedInFile) implements Node { }

    /**
     * A non-blank line of code.
     *
     * @param statementCode        the code of the statement that contains it ({@code executesBuiltString} looks at the whole file)
     * @param executesBuiltString  it executes a variable that the statement builds by concatenation
     */
    public record Line(int line, String text, String code, String statementCode, boolean executesBuiltString) implements Node { }
}
