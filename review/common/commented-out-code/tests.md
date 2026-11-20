# Tests: common/commented-out-code

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: disabled code (Examples: Flag)

covers: example:flag,required:code-lines

```java path=src/main/java/C.java
class C {
  void f(User user) {
    // if (user == null) {  // CANDIDATE
    //     return;
    // }
    work();
  }
}
```

## Case: a prose comment and an example (Examples: OK)

covers: example:ok,required:prose

```java path=src/main/java/C.java
class C {
  // Example: if (user == null) return;
  // The user may be missing when the session has expired,
  // so the caller must handle that case.
  void f() { }
}
```

## Case: an assignment block, and a block comment

covers: detect:forms

```java path=src/main/java/C.java
class C {
  void f() {
    // int a = compute();  // CANDIDATE
    // int b = a + 1;
    // use(a, b);
    /*  // CANDIDATE
    int x = 1;
    int y = 2;
    run(x, y);
    */
  }
}
```

## Case: a block that explains itself (Do not flag)

covers: do-not-flag:explained,required:explanation

```java path=src/main/java/C.java
class C {
  void f() {
    // kept because the legacy importer still calls it:
    // int a = compute();
    // use(a);
    work();
  }
}
```

## Case: Javadoc with a code example and a license header (Do not flag)

covers: do-not-flag:javadoc,do-not-flag:license

```java path=src/main/java/C.java
/*
 * Copyright 2024 Acme
 * Licensed under the Apache License, Version 2.0
 * limitations under the License;
 */
class C {
  /**
   * Usage:
   * C c = new C();
   * c.f();
   * c.g();
   */
  void f() { }
}
```

## Case: two lines are not a block, and prose with a few code-like words

covers: detect:three-lines

```java path=src/main/java/C.java
class C {
  void f() {
    // int a = compute();
    // use(a);
    work();
    // the first step of the import
    // then the second step
    // then the third
    more();
  }
}
```
