package com.codeclear.rules;

import com.github.javaparser.ast.CompilationUnit;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterPython;
import org.treesitter.TreeSitterTsx;
import org.treesitter.TreeSitterTypescript;

/**
 * Parses Python and TypeScript (and JavaScript, which the TypeScript grammar reads; and the TSX grammar for .tsx and .jsx files, which also reads JSX) with tree-sitter into the
 * {@link CCTsTree} the rules receive. The grammars are native libraries bundled in the jar for Linux, macOS and Windows.
 * Tree-sitter recovers from syntax errors, so a file with one is still analysed.
 *
 * <p>A parser is not thread-safe, so each call makes its own.
 */
public final class TreeSitterFileAnalyzer {

    /** The language this analyzer reads for a file, or null: {@code python} or {@code typescript}. */
    public static String languageOf(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".py")) {
            return "python";
        }
        if (p.endsWith(".ts") || p.endsWith(".js") || p.endsWith(".mjs") || p.endsWith(".cjs") || p.endsWith(".mts") || p.endsWith(".cts")
                || p.endsWith(".tsx") || p.endsWith(".jsx")) {
            return "typescript";
        }
        return null;
    }

    /** @throws IllegalArgumentException for a file of another language */
    public CCFileAnalysis analyze(String path, String source, CCChangedLines changedLines) {
        String language = languageOf(path);
        if (language == null) {
            throw new IllegalArgumentException("no tree-sitter grammar for " + path);
        }
        String lower = path.toLowerCase(Locale.ROOT);
        // The TSX grammar reads JSX; it is the one for .tsx and .jsx files.
        TSLanguage grammar = language.equals("python") ? new TreeSitterPython()
                : lower.endsWith(".tsx") || lower.endsWith(".jsx") ? new TreeSitterTsx() : new TreeSitterTypescript();
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        try (TSParser parser = new TSParser()) {
            parser.setLanguage(grammar);
            TSTree tree = parser.parseString(null, source);
            return new CCFileAnalysis(new CCFileInfo(path, language), new CompilationUnit(), null, changedLines, null, null,
                    new CCTsTree(tree, bytes, language));
        }
    }
}
