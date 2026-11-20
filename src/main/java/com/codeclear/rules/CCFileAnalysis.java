package com.codeclear.rules;

import com.github.javaparser.ast.Node;

/**
 * Everything the engine hands to the rules for one file.
 *
 * @param file         path and language
 * @param ast          the parsed tree, a {@code CompilationUnit} for Java (an empty one for any other language)
 * @param model        project facade
 * @param changedLines lines touched by the change; findings elsewhere are dropped
 * @param sql          the statements, columns and lines of a SQL file; null for any other language
 * @param config       the settings of a YAML, JSON or properties file; null for any other language
 */
public record CCFileAnalysis(CCFileInfo file, Node ast, CCCodeModel model, CCChangedLines changedLines, CCSqlModel sql,
                             CCConfigModel config) {

    public CCFileAnalysis(CCFileInfo file, Node ast, CCCodeModel model, CCChangedLines changedLines) {
        this(file, ast, model, changedLines, null, null);
    }

    public CCFileAnalysis(CCFileInfo file, Node ast, CCCodeModel model, CCChangedLines changedLines, CCSqlModel sql) {
        this(file, ast, model, changedLines, sql, null);
    }
}
