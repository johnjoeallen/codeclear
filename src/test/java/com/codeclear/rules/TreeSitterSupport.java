package com.codeclear.rules;

/**
 * Whether the tree-sitter native library can be loaded on this machine. Endpoint security (Carbon Black, for example) can
 * block it on a locked-down Windows machine; the tests that need it are then skipped instead of failing the build.
 */
public final class TreeSitterSupport {

    private static final boolean AVAILABLE = probe();

    private TreeSitterSupport() { }

    public static boolean available() {
        return AVAILABLE;
    }

    private static boolean probe() {
        try {
            new TreeSitterFileAnalyzer().analyze("probe.py", "x = 1\n", CCChangedLines.all());
            return true;
        } catch (Throwable e) { // UnsatisfiedLinkError, NoClassDefFoundError, ExceptionInInitializerError
            System.err.println("tree-sitter is not available, so the Python and TypeScript tests are skipped: " + e);
            return false;
        }
    }
}
