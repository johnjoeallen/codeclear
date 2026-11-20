package com.codeclear.rules;

import com.github.javaparser.ast.Node;

/**
 * Everything the engine hands to the rules for one file.
 *
 * @param file         path and language
 * @param ast          the parsed tree, a {@code CompilationUnit} for Java (an empty one for SQL)
 * @param model        project facade
 * @param changedLines lines touched by the change; findings elsewhere are dropped
 * @param sql          the statements, columns and lines of a SQL file; null for any other language
 */
public record CCFileAnalysis(CCFileInfo file, Node ast, CCCodeModel model, CCChangedLines changedLines, CCSqlModel sql) {

    public CCFileAnalysis(CCFileInfo file, Node ast, CCCodeModel model, CCChangedLines changedLines) {
        this(file, ast, model, changedLines, null);
    }
}
