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
    private final JavaParser lenient;
    private final CCCodeModel model = new JavaParserModel();

    public JavaFileAnalyzer() {
        CombinedTypeSolver solver = new CombinedTypeSolver(new ReflectionTypeSolver(true));
        // The newest syntax the parser knows. Java 25 features (module imports, flexible constructor bodies) are only
        // accepted at this level; the highest-level shortcut BLEEDING_EDGE lags behind.
        this.parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
                .setSymbolResolver(new JavaSymbolSolver(solver)));
        // Second chance for a file the validator rejects: the review is of code that already compiles, so a validation
        // rule that is stricter or older than the compiler must not hide the whole file. RAW parses the grammar only.
        this.lenient = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.RAW)
                .setSymbolResolver(new JavaSymbolSolver(solver)));
    }

    /**
     * @throws IllegalArgumentException when the source does not parse; the caller should skip the file. The message names
     *                                  the file and the line of the first problem.
     */
    public CCFileAnalysis analyze(String path, String source, CCChangedLines changedLines) {
        ParseResult<CompilationUnit> result = parser.parse(source);
        if (!usable(result)) {
            ParseResult<CompilationUnit> second = lenient.parse(source);
            if (usable(second)) {
                result = second;
            } else {
                throw new IllegalArgumentException("cannot parse " + path + describe(result));
            }
        }
        return new CCFileAnalysis(new CCFileInfo(path, "java"), result.getResult().get(), model, changedLines);
    }

    private static boolean usable(ParseResult<CompilationUnit> result) {
        return result.isSuccessful() && result.getResult().isPresent();
    }

    /** {@code :<line>: <message>} for the first problem, as the compiler would say it. */
    private static String describe(ParseResult<CompilationUnit> result) {
        if (result.getProblems().isEmpty()) {
            return ": unknown error";
        }
        var problem = result.getProblems().get(0);
        String line = problem.getLocation().flatMap(l -> l.getBegin().getRange()).map(r -> ":" + r.begin.line).orElse("");
        return line + ": " + problem.getMessage().split("\\R", 2)[0];
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
