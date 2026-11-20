# Tests: java/interrupt-flag-not-restored

A `// FLAG` marker ends each line that must produce a finding.

## Case: log only (Examples: Flag)

covers: example:flag,required:log-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { queue.take(); } catch (InterruptedException e) { log.warn("interrupted"); }  // FLAG
  }
}
```

## Case: flag restored (Examples: OK)

covers: example:ok,required:restore

```java path=src/main/java/C.java
class C {
  void f() {
    try { queue.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
  }
}
```

## Case: flag restored after logging

covers: detect:restore

```java path=src/main/java/C.java
class C {
  void f() {
    try { queue.take(); } catch (InterruptedException e) {
      log.warn("interrupted");
      Thread.currentThread().interrupt();
    }
  }
}
```

## Case: a bare return is not enough

covers: detect:bare-return

```java path=src/main/java/C.java
class C {
  int f() {
    try { queue.take(); } catch (InterruptedException e) {  // FLAG
      return -1;
    }
    return 0;
  }
}
```

## Case: rethrown (Do not flag: rethrow)

covers: do-not-flag:rethrow-or-wrap,required:rethrow

```java path=src/main/java/C.java
class C {
  void f() throws InterruptedException {
    try { queue.take(); } catch (InterruptedException e) { throw e; }
  }
}
```

## Case: wrapped with the cause (Do not flag: wrap)

covers: do-not-flag:rethrow-or-wrap,required:wrapped

```java path=src/main/java/C.java
class C {
  void f() {
    try { queue.take(); } catch (InterruptedException e) { throw new IllegalStateException("interrupted", e); }
  }
}
```

## Case: multi-catch including InterruptedException

covers: detect:multi-catch,required:multi-catch

```java path=src/main/java/C.java
class C {
  void f() {
    try { queue.take(); } catch (IOException | InterruptedException e) { log.warn("failed"); }  // FLAG
  }
}
```

## Case: catch of another exception

covers: detect:only-interrupted

```java path=src/main/java/C.java
class C {
  void f() {
    try { queue.take(); } catch (IOException e) { log.warn("failed"); }
  }
}
```

## Case: a main method (Do not flag: main)

covers: do-not-flag:main-or-test

```java path=src/main/java/C.java
class C {
  public static void main(String[] args) {
    try { Thread.sleep(10); } catch (InterruptedException e) { log.warn("interrupted"); }
  }
}
```

## Case: a test source (Do not flag: test)

covers: do-not-flag:main-or-test

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    try { Thread.sleep(10); } catch (InterruptedException e) { log.warn("interrupted"); }
  }
}
```

## Case: two catches, only the second wrong

covers: detect:per-catch

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    try { b(); } catch (InterruptedException e) { log.warn("x"); }  // FLAG
  }
}
```

## Case: the interruption is recorded and the flag restored after the loop (Do not flag: deferred restore)

covers: do-not-flag:deferred-restore

```java path=src/main/java/C.java
class C {
  void f(List<Thread> threads) {
    boolean interrupted = false;
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
```

## Case: the interruption is recorded but never restored

covers: detect:deferred-restore

```java path=src/main/java/C.java
class C {
  boolean f(Thread t) {
    boolean interrupted = false;
    try {
      t.join();
    } catch (InterruptedException e) {  // FLAG
      interrupted = true;
    }
    return interrupted;
  }
}
```

## Case: the flag is restored before the catch, not after

covers: detect:deferred-restore

```java path=src/main/java/C.java
class C {
  void f(Thread t) {
    Thread.currentThread().interrupt();
    boolean seen = false;
    try {
      t.join();
    } catch (InterruptedException e) {  // FLAG
      seen = true;
    }
  }
}
```
