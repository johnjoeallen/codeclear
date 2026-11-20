package com.codeclear.rules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.treesitter.TSNode;

/**
 * A node of a tree-sitter syntax tree (Python, TypeScript). The rule language sees its grammar type ({@code except_clause},
 * {@code call_expression}), its text, its line, and its named children; it never sees the native tree itself.
 */
public final class CCTsNode implements CCModelNode {

    private final TSNode node;
    private final CCTsTree tree;

    CCTsNode(TSNode node, CCTsTree tree) {
        this.node = node;
        this.tree = tree;
    }

    /** The grammar type, for example {@code except_clause}. */
    public String type() {
        return node.getType();
    }

    @Override
    public int line() {
        return node.getStartPoint().getRow() + 1;
    }

    public String text() {
        byte[] src = tree.source();
        int start = Math.min(node.getStartByte(), src.length);
        int end = Math.min(node.getEndByte(), src.length);
        return new String(src, start, end - start, StandardCharsets.UTF_8);
    }

    public boolean isError() {
        return node.isError() || node.isMissing();
    }

    public CCTsNode parent() {
        TSNode p = node.getParent();
        return p == null || p.isNull() ? null : new CCTsNode(p, tree);
    }

    /** The named children in source order: punctuation and keywords are left out. */
    public List<CCTsNode> children() {
        List<CCTsNode> out = new ArrayList<>();
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            out.add(new CCTsNode(node.getNamedChild(i), tree));
        }
        return out;
    }

    /** The child that the grammar gives this field name ({@code name}, {@code body}, {@code function}), or null. */
    public CCTsNode field(String name) {
        TSNode c = node.getChildByFieldName(name);
        return c == null || c.isNull() ? null : new CCTsNode(c, tree);
    }

    /** Every node below this one, in source order, whose type is {@code type}; or every named node when it is null. */
    public List<CCTsNode> descendants(String type) {
        List<CCTsNode> out = new ArrayList<>();
        collect(node, type, out);
        return out;
    }

    private void collect(TSNode n, String type, List<CCTsNode> out) {
        for (int i = 0; i < n.getNamedChildCount(); i++) {
            TSNode c = n.getNamedChild(i);
            if (type == null || c.getType().equals(type)) {
                out.add(new CCTsNode(c, tree));
            }
            collect(c, type, out);
        }
    }

    /** The nearest ancestor of this type, or null. */
    public CCTsNode ancestor(String type) {
        for (TSNode p = node.getParent(); p != null && !p.isNull(); p = p.getParent()) {
            if (p.getType().equals(type)) {
                return new CCTsNode(p, tree);
            }
        }
        return null;
    }

    /** Every ancestor of this type, nearest first. */
    public List<CCTsNode> ancestors(String type) {
        List<CCTsNode> out = new ArrayList<>();
        for (TSNode p = node.getParent(); p != null && !p.isNull(); p = p.getParent()) {
            if (p.getType().equals(type)) {
                out.add(new CCTsNode(p, tree));
            }
        }
        return out;
    }

    /** The nearest ancestor with a type in the list, or null. */
    public CCTsNode ancestorOf(List<?> types) {
        for (TSNode p = node.getParent(); p != null && !p.isNull(); p = p.getParent()) {
            if (types.contains(p.getType())) {
                return new CCTsNode(p, tree);
            }
        }
        return null;
    }

    /** The arguments of a call: the named children of its {@code arguments} field, or an empty list. */
    public List<CCTsNode> args() {
        CCTsNode a = field("arguments");
        return a == null ? List.of() : a.children();
    }

    /** The arguments of a Python call that are neither {@code name=value} nor a {@code *}/{@code **} splat. */
    public List<CCTsNode> positional() {
        return args().stream().filter(x -> !x.type().equals("keyword_argument") && !x.type().endsWith("splat")
                && !x.type().equals("comment")).toList();
    }

    /** The value of the Python keyword argument {@code name=value} of a call, or null. */
    public CCTsNode keyword(String name) {
        for (CCTsNode a : args()) {
            if (a.type().equals("keyword_argument") && a.field("name") != null && a.field("name").text().equals(name)) {
                return a.field("value");
            }
        }
        return null;
    }

    /** The last of the named children, or null. */
    public CCTsNode lastChild() {
        int n = node.getNamedChildCount();
        return n == 0 ? null : new CCTsNode(node.getNamedChild(n - 1), tree);
    }

    /** The named siblings after this node, in order. */
    public List<CCTsNode> following() {
        List<CCTsNode> out = new ArrayList<>();
        for (TSNode s = node.getNextNamedSibling(); s != null && !s.isNull(); s = s.getNextNamedSibling()) {
            out.add(new CCTsNode(s, tree));
        }
        return out;
    }

    /** The i-th named child (from 0), or null. */
    public CCTsNode childAt(int i) {
        return i >= 0 && i < node.getNamedChildCount() ? new CCTsNode(node.getNamedChild(i), tree) : null;
    }

    /** The first named child of this type, or null. */
    public CCTsNode child(String type) {
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            if (node.getNamedChild(i).getType().equals(type)) {
                return new CCTsNode(node.getNamedChild(i), tree);
            }
        }
        return null;
    }

    /** The unnamed children, as text: the keywords and operators, {@code is}, {@code not}, {@code ==}. */
    public List<String> tokens() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            TSNode c = node.getChild(i);
            if (!c.isNamed()) {
                out.add(c.getType());
            }
        }
        return out;
    }

    /** The line this node ends on. */
    public int endLine() {
        return node.getEndPoint().getRow() + 1;
    }

    /** The name is used below this node: an identifier with this text. */
    public boolean mentions(String name) {
        if ((type().equals("identifier") || type().equals("property_identifier")) && text().equals(name)) {
            return true;
        }
        for (CCTsNode d : descendants(null)) {
            if ((d.type().equals("identifier") || d.type().equals("property_identifier")) && d.text().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** A comment starts on this node's last line (after it), or ends on the line just above its first line. */
    public boolean hasNearbyComment() {
        // A trailing comment is not always a sibling in the tree, so look at every comment of the file by line.
        CCTsNode root = this;
        for (CCTsNode p = parent(); p != null; p = p.parent()) {
            root = p;
        }
        for (CCTsNode c : root.descendants("comment")) {
            if (c.line() == endLine() || c.endLine() == line() - 1) {
                return true;
            }
        }
        return false;
    }

    /** This is an identifier that a variable declaration inside {@code scope} initialises with a {@code new} expression. */
    public boolean declaredAsNewIn(CCTsNode scope) {
        if (!type().equals("identifier")) {
            return false;
        }
        for (CCTsNode d : scope.descendants("variable_declarator")) {
            CCTsNode name = d.field("name");
            CCTsNode value = d.field("value");
            if (name != null && value != null && name.text().equals(text()) && value.type().equals("new_expression")) {
                return true;
            }
        }
        return false;
    }

    /** How many times an identifier with this text appears at or below this node. */
    public long countOf(String name) {
        long n = type().equals("identifier") && text().equals(name) ? 1 : 0;
        for (CCTsNode d : descendants("identifier")) {
            if (d.text().equals(name)) {
                n++;
            }
        }
        return n;
    }

    /**
     * This node mentions {@code name}, or an identifier in it is assigned in {@code scope} from an expression that mentions
     * {@code name} (one step: {@code q = request.json} then {@code find(q)}).
     */
    public boolean derivedFrom(String name, CCTsNode scope) {
        if (mentions(name)) {
            return true;
        }
        for (CCTsNode id : descendants("identifier")) {
            for (CCTsNode a : scope.descendants("assignment")) {
                CCTsNode left = a.field("left");
                CCTsNode right = a.field("right");
                if (left != null && right != null && left.text().equals(id.text()) && right.mentions(name)) {
                    return true;
                }
            }
        }
        if (type().equals("identifier")) {
            for (CCTsNode a : scope.descendants("assignment")) {
                CCTsNode left = a.field("left");
                CCTsNode right = a.field("right");
                if (left != null && right != null && left.text().equals(text()) && right.mentions(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** An identifier in this node is a parameter of {@code function}. */
    public boolean usesParameterOf(CCTsNode function) {
        CCTsNode parameters = function == null ? null : function.field("parameters");
        if (parameters == null) {
            return false;
        }
        if (type().equals("identifier") && parameters.mentions(text())) {
            return true;
        }
        for (CCTsNode id : descendants("identifier")) {
            if (parameters.mentions(id.text())) {
                return true;
            }
        }
        return false;
    }

    /** A function or lambda that gives this parameter name a default value ({@code lambda x=x: ...}). */
    public boolean bindsByDefault(String name) {
        CCTsNode parameters = field("parameters");
        if (parameters == null) {
            return false;
        }
        for (CCTsNode d : parameters.descendants("default_parameter")) {
            CCTsNode n = d.field("name");
            if (n != null && n.text().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** The previous named sibling, or null. */
    public CCTsNode previous() {
        TSNode s = node.getPrevNamedSibling();
        return s == null || s.isNull() ? null : new CCTsNode(s, tree);
    }

    /** The next named sibling, or null. */
    public CCTsNode next() {
        TSNode s = node.getNextNamedSibling();
        return s == null || s.isNull() ? null : new CCTsNode(s, tree);
    }

    /** The field name that the parent gives this node ({@code body}, {@code right}), or null. */
    public String fieldName() {
        TSNode p = node.getParent();
        if (p == null || p.isNull()) {
            return null;
        }
        for (int i = 0; i < p.getChildCount(); i++) {
            if (TSNode.eq(p.getChild(i), node)) {
                return p.getFieldNameForChild(i);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CCTsNode n && n.node.getStartByte() == node.getStartByte() && n.node.getEndByte() == node.getEndByte()
                && n.node.getType().equals(node.getType());
    }

    @Override
    public int hashCode() {
        return node.getStartByte() * 31 + node.getEndByte();
    }

    @Override
    public String toString() {
        return text();
    }
}
