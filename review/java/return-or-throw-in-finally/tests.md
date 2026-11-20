# Tests: java/return-or-throw-in-finally

A `// FLAG` marker ends each line that must produce a finding.

## Case: return in finally (Examples: Flag)

covers: example:flag,required:return

```java path=src/main/java/C.java
class C {
  int f() {
    int result = 1;
    try { work(); } finally { return result; }  // FLAG
  }
}
```

## Case: cleanup in finally (Examples: OK)

covers: example:ok

```java path=src/main/java/C.java
class C {
  void f() {
    try { work(); } finally { cleanup(); }
  }
}
```

## Case: throw in finally

covers: detect:throw,required:throw

```java path=src/main/java/C.java
class C {
  void f() {
    try { work(); } finally { throw new IllegalStateException("x"); }  // FLAG
  }
}
```

## Case: return or throw in try and catch are fine

covers: detect:finally-only

```java path=src/main/java/C.java
class C {
  int f() {
    try { return work(); } catch (RuntimeException e) { throw e; } finally { cleanup(); }
  }
}
```

## Case: break and continue that leave the finally

covers: detect:break-continue

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    for (String s : items) {
      try { work(s); } finally { break; }  // FLAG
    }
    for (String s : items) {
      try { work(s); } finally { continue; }  // FLAG
    }
  }
}
```

## Case: break inside a loop that is inside the finally

covers: detect:break-continue

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    try { work(); } finally {
      for (String s : items) {
        if (s.isEmpty()) { break; }
        if (s.equals("x")) { continue; }
      }
    }
  }
}
```

## Case: break out of a switch inside the finally

covers: detect:break-continue

```java path=src/main/java/C.java
class C {
  void f(int x) {
    try { work(); } finally {
      switch (x) {
        case 1: cleanup(); break;
        default: break;
      }
    }
  }
}
```

## Case: a throw caught by a nested try (Do not flag: nested caught throw)

covers: do-not-flag:nested-caught-throw,required:nested-caught

```java path=src/main/java/C.java
class C {
  void f() {
    try { work(); } finally {
      try { cleanup(); throw new IllegalStateException("x"); } catch (IllegalStateException e) { log.warn("ignored"); }
    }
  }
}
```

## Case: a nested try without a catch lets the throw escape

covers: detect:nested-uncaught-throw

```java path=src/main/java/C.java
class C {
  void f() {
    try { work(); } finally {
      try { 
        throw new IllegalStateException("x");  // FLAG
      } finally { cleanup(); }
    }
  }
}
```

## Case: return in a lambda declared in the finally (Do not flag: lambda)

covers: do-not-flag:lambda-return,required:lambda

```java path=src/main/java/C.java
class C {
  void f() {
    try { work(); } finally {
      Supplier<Integer> s = () -> { return 1; };
      cleanup(s);
    }
  }
}
```

## Case: return in an anonymous class method declared in the finally

covers: do-not-flag:lambda-return

```java path=src/main/java/C.java
class C {
  void f() {
    try { work(); } finally {
      Runnable r = new Runnable() {
        public void run() { return; }
      };
      cleanup(r);
    }
  }
}
```

## Case: a return after the try statement

covers: detect:finally-only

```java path=src/main/java/C.java
class C {
  int f() {
    try { work(); } finally { cleanup(); }
    return 1;
  }
}
```
