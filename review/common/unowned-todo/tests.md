# Tests: common/unowned-todo

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: a plain TODO (Examples: Flag)

covers: example:flag,required:plain-todo

```java path=src/main/java/C.java
class C {
  // TODO handle retries  // FLAG
  void f() { }
}
```

## Case: FIXME, HACK and XXX markers

covers: detect:markers

```java path=src/main/java/C.java
class C {
  // FIXME broken on Windows  // FLAG
  // HACK  // FLAG
  // XXX revisit  // FLAG
  void f() { }
}
```

## Case: block, Javadoc and trailing comments, and a multi-line comment reported at its first line

covers: detect:comment-kinds

```java path=src/main/java/C.java
class C {
  /* TODO block comment */  // FLAG
  /** TODO javadoc */  // FLAG
  void f() {
    int x = 1; // TODO trailing  // FLAG
    /*  // FLAG
     * more text
     * TODO inside a long comment
     */
  }
}
```

## Case: with an owner (Examples: OK)

covers: example:ok,do-not-flag:ticket-or-owner,required:owner

```java path=src/main/java/C.java
class C {
  // TODO(jane): handle retries
  // FIXME (bob) later
}
```

## Case: with a ticket (Do not flag: ticket)

covers: do-not-flag:ticket-or-owner,required:ticket

```java path=src/main/java/C.java
class C {
  // TODO PAY-1234 handle retries
  // TODO #123
  // FIXME see https://example.com/issues/9
  // HACK: ABC-7
}
```

## Case: inside a string literal (Do not flag: string)

covers: do-not-flag:string-literal,required:string

```java path=src/main/java/C.java
class C {
  String s = "TODO write me";
  String t = "// TODO also not a comment";
}
```

## Case: a marker that already existed, outside the changed lines (Do not flag: unchanged)

covers: do-not-flag:unchanged-line,required:unchanged
changed: 4

```java path=src/main/java/C.java
class C {
  // TODO old marker
  void f() { }
  // TODO new marker  // FLAG
}
```

## Case: the whole file changed

covers: detect:changed-lines
changed: 1-3

```java path=src/main/java/C.java
class C {
  // TODO first  // FLAG
  // TODO second  // FLAG
}
```

## Case: words that merely contain a marker

covers: detect:whole-word

```java path=src/main/java/C.java
class C {
  // todos are tracked elsewhere
  // the hacker news feed
  // MAXXXIMUM
}
```

## Case: ordinary comments

covers: detect:marker-needed

```java path=src/main/java/C.java
class C {
  // retries are handled by the caller
  /** The value. */
  int x;
}
```
