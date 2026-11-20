package com.codeclear.rules;

import com.github.javaparser.ast.Node;

/**
 * Everything the engine hands to the rules for one file.
 *
 * @param file         path and language
 * @param ast          the parsed tree, a {@code CompilationUnit} for Java
 * @param model        project facade
 * @param changedLines lines touched by the change; findings elsewhere are dropped
 */
public record CCFileAnalysis(CCFileInfo file, Node ast, CCCodeModel model, CCChangedLines changedLines) { }
