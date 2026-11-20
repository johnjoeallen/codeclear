package com.codeclear.rules.dsl;

import java.util.List;
import java.util.regex.Pattern;

/** Expression tree of the experimental rule language. Immutable, so mutants can be built by rebuilding. */
sealed interface Expr {
    record Str(String v) implements Expr { }
    record Rx(String src, Pattern p) implements Expr { }
    record Bool(boolean v) implements Expr { }
    record Lst(List<Expr> items) implements Expr { }
    record Id(String name) implements Expr { }
    record Field(Expr target, String name) implements Expr { }
    record FactCall(Expr target, String name, List<Expr> args) implements Expr { }
    record Apply(String fn, List<Expr> args) implements Expr { }
    record Not(Expr e) implements Expr { }
    record And(Expr l, Expr r) implements Expr { }
    record Or(Expr l, Expr r) implements Expr { }
    record Implies(Expr l, Expr r) implements Expr { }
    record Ternary(Expr c, Expr a, Expr b) implements Expr { }
    record In(Expr l, Expr r) implements Expr { }
    record Matches(Expr l, Rx r) implements Expr { }
    record IsKind(Expr l, String kind, boolean neg) implements Expr { }
    record IsEmpty(Expr l, boolean neg) implements Expr { }
    record Exists(Expr l) implements Expr { }
    record Inside(Expr l, String kind) implements Expr { }
    record Quant(boolean all, Expr l, String pred) implements Expr { }
    record Eq(Expr l, Expr r, boolean neg) implements Expr { }
    record Num(long v) implements Expr { }
    record Size(Expr l) implements Expr { }
    /** A numeric comparison: {@code op} is one of {@code ==}, {@code !=}, {@code >}, {@code >=}, {@code <}, {@code <=}. */
    record Cmp(Expr l, String op, Expr r) implements Expr { }
}
