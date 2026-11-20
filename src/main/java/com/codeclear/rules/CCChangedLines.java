package com.codeclear.rules;

import java.util.Set;

/** The lines of a file touched by the change under review. Findings must anchor to one of them. */
public interface CCChangedLines {

    boolean contains(int line);

    /** Every line counts as changed. Used for tests and whole-file reviews. */
    static CCChangedLines all() {
        return line -> true;
    }

    static CCChangedLines of(Set<Integer> lines) {
        Set<Integer> copy = Set.copyOf(lines);
        return copy::contains;
    }
}
