package com.codeclear.rules;

import com.github.javaparser.ast.expr.Expression;

/**
 * Read-only facade over the project, exposed to scripts as {@code model}. Scripts never parse files or touch
 * the filesystem; whatever they need beyond the file's own tree comes from here.
 */
public interface CCCodeModel {

    /**
     * The qualified type name of an expression ({@code java.util.Optional}, {@code int}), or {@code null} when it
     * cannot be resolved. Rules must treat {@code null} as "unknown": do not report, or report at LOW confidence.
     */
    String typeOf(Expression expression);
}
