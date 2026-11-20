# Tests: common/comment-restates-code

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: three comments that repeat the code (Examples: Flag)

covers: example:flag,required:three-comments

```java path=src/main/java/C.java
class C {
  void f(int counter, List<String> names) {
    // increment counter  // CANDIDATE
    counter++;
    // add name to names
    names.add(name);
    // return counter
    return counter;
  }
}
```

## Case: a comment that says why (Examples: OK)

covers: example:ok,do-not-flag:reason,required:reason

```java path=src/main/java/C.java
class C {
  void f(int counter) {
    // the legacy importer reads this one too, so it must go up first
    counter++;
    // seconds, not milliseconds
    timeout = 30;
    // because the cache is shared
    cache.clear();
  }
}
```

## Case: fewer than three restating comments (Do not flag)

covers: do-not-flag:fewer-than-three

```java path=src/main/java/C.java
class C {
  void f(int counter, List<String> names) {
    // increment counter
    counter++;
    // add name to names
    names.add(name);
  }
}
```

## Case: directives, Javadoc and license headers (Do not flag)

covers: do-not-flag:directives-and-docs

```java path=src/main/java/C.java
/*
 * Copyright 2024 Acme
 */
class C {
  /** Increment counter. */
  void f(int counter) {
    // CHECKSTYLE:OFF increment counter
    counter++;
    // NOPMD set value
    value = 1;
    // eslint-disable add item
    items.add(item);
  }
}
```

## Case: comments that are not about the next statement

covers: detect:unrelated

```java path=src/main/java/C.java
class C {
  void f() {
    // first step
    a();
    // second step
    b();
    // third step
    c();
  }
}
```
