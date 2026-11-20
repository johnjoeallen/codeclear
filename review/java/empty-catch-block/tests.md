# Tests: java/empty-catch-block

Generated for api-version 1. A `// FLAG` marker ends each line that must produce a finding.

## Case: empty block (Examples: Flag)

covers: example:flag, required:empty-block

```java path=src/main/java/A.java
class A {
  void f() {
    try { files.delete(path); } catch (IOException e) { }  // FLAG
  }
}
```

## Case: block with only whitespace

covers: required:whitespace-block

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (Exception e) {  // FLAG

    }
  }
}
```

## Case: comment explains why (Do not flag: comment)

covers: do-not-flag:comment, required:comment

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException e) { /* best effort cleanup */ }
  }
}
```

## Case: line comment inside block

covers: do-not-flag:comment

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException e) {
      // cleanup only
    }
  }
}
```

## Case: parameter named ignored (Examples: OK)

covers: example:ok, do-not-flag:ignored-name, required:ignored-param

```java path=src/main/java/A.java
class A {
  void f() {
    try { files.delete(path); } catch (IOException ignored) { }
  }
}
```

## Case: parameter named ignore

covers: do-not-flag:ignored-name

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException ignore) { }
  }
}
```

## Case: parameter named expected

covers: do-not-flag:ignored-name

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException expected) { }
  }
}
```

## Case: debug-only log, no rethrow

covers: detect:debug-only, required:debug-only

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException e) {  // FLAG
      log.debug("failed", e);
    }
  }
}
```

## Case: trace-only log via LOGGER

covers: detect:debug-only

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException e) {  // FLAG
      LOGGER.trace("failed");
    }
  }
}
```

## Case: debug log then rethrow

covers: detect:debug-only

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException e) {
      log.debug("failed", e);
      throw new IllegalStateException(e);
    }
  }
}
```

## Case: debug log then return fallback value

covers: detect:debug-only

```java path=src/main/java/A.java
class A {
  int f() {
    try { return run(); } catch (IOException e) {
      log.debug("failed", e);
      return -1;
    }
  }
}
```

## Case: error-level log is not debug-only

covers: detect:debug-only

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException e) {
      log.error("failed", e);
    }
  }
}
```

## Case: empty block in a test source (Do not flag: tests)

covers: do-not-flag:test-source, required:test-path

```java path=src/test/java/ATest.java
class ATest {
  void f() {
    try { run(); } catch (IOException e) { }
  }
}
```

## Case: InterruptedException with flag restored

covers: do-not-flag:interrupt-restored

```java path=src/main/java/A.java
class A {
  void f() {
    try { Thread.sleep(1); } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
```

## Case: multi-catch empty block

covers: detect:empty-block

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); } catch (IOException | RuntimeException e) { }  // FLAG
  }
}
```

## Case: two catch clauses, only the second empty

covers: detect:empty-block

```java path=src/main/java/A.java
class A {
  void f() {
    try { run(); }
    catch (IOException e) { throw new RuntimeException(e); }
    catch (RuntimeException e) { }  // FLAG
  }
}
```

## Case: InterruptedException swallowed, flag not restored

covers: do-not-flag:interrupt-restored

```java path=src/main/java/A.java
class A {
  void f() {
    try { Thread.sleep(1); } catch (InterruptedException e) {}  // FLAG
  }
}
```

## Mutations

- `empty block (Examples: Flag)` -> `parameter named ignored (Examples: OK)`
