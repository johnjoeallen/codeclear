package com.codeclear.rules;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * Parses Java source into the {@link CCFileAnalysis} the rules receive: a symbol-resolved tree plus a
 * {@link CCCodeModel} that answers type questions.
 *
 * <p>Only the JDK is on the type solver today, so types from the project's own dependencies resolve to
 * {@code null} and rules fall back to LOW confidence heuristics. Adding project classpath resolution means adding
 * solvers here.
 */
public final class JavaFileAnalyzer {

    private final JavaParser parser;
    private final CCCodeModel model = new JavaParserModel();

    public JavaFileAnalyzer() {
        CombinedTypeSolver solver = new CombinedTypeSolver(new ReflectionTypeSolver(true));
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                .setSymbolResolver(new JavaSymbolSolver(solver));
        this.parser = new JavaParser(config);
    }

    /**
     * @throws IllegalArgumentException when the source does not parse; the caller should skip the file
     */
    public CCFileAnalysis analyze(String path, String source, CCChangedLines changedLines) {
        ParseResult<CompilationUnit> result = parser.parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            String problem = result.getProblems().isEmpty() ? "unknown error" : result.getProblems().get(0).getMessage();
            throw new IllegalArgumentException("cannot parse " + path + ": " + problem);
        }
        return new CCFileAnalysis(new CCFileInfo(path, "java"), result.getResult().get(), model, changedLines);
    }

    private static final class JavaParserModel implements CCCodeModel {
        @Override
        public String typeOf(Expression expression) {
            try {
                ResolvedType type = expression.calculateResolvedType();
                return type.isReferenceType() ? type.asReferenceType().getQualifiedName() : type.describe();
            } catch (RuntimeException e) {
                // UnsolvedSymbolException and friends: the type is unknown, which rules treat as "do not report".
                return null;
            }
        }
    }
}
