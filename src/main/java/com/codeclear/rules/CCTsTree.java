package com.codeclear.rules;

import org.treesitter.TSTree;

/** A parsed tree-sitter tree with the source it was parsed from, so a node can give its text. */
public final class CCTsTree {

    private final TSTree tree;
    private final byte[] source;
    private final String language;

    CCTsTree(TSTree tree, byte[] source, String language) {
        this.tree = tree;
        this.source = source;
        this.language = language;
    }

    byte[] source() {
        return source;
    }

    public String language() {
        return language;
    }

    public CCTsNode root() {
        return new CCTsNode(tree.getRootNode(), this);
    }

    /** The tree has a syntax error somewhere: tree-sitter still gives a tree, with an error node where it gave up. */
    public boolean hasError() {
        return tree.getRootNode().hasError();
    }
}
