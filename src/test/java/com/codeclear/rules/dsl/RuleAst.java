package com.codeclear.rules.dsl;

import java.util.List;

/** A parsed rule: one {@code for} binding, ordered filter clauses, and ordered flags (first match wins per node). */
record RuleAst(String id, String var, String kind, List<Clause> clauses, List<Flag> flags) {

    sealed interface Clause {
        record Where(Expr e) implements Clause { }
        record Unless(Expr e) implements Clause { }
        record Require(Expr e) implements Clause { }
        /** {@code param} is null for a plain value, set for a one-argument named predicate. */
        record Let(String name, String param, Expr body) implements Clause { }
    }

    /** {@code message} parts are Strings or Exprs (from {@code {...}} placeholders). */
    record Flag(Expr at, Expr when, Expr confidence, List<Object> message) { }
}
