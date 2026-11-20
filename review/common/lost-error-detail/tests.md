# Tests: common/lost-error-detail

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: only the message is logged (Examples: Flag)

covers: example:flag,required:message-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { run(); } catch (IOException e) {
      log.error("Failed: " + e.getMessage());  // CANDIDATE
    }
  }
}
```

## Case: the exception as the last argument (Examples: OK)

covers: example:ok,do-not-flag:exception-passed,required:passed

```java path=src/main/java/C.java
class C {
  void f(String id) {
    try { run(); } catch (IOException e) {
      log.error("Failed for {}", id, e);
    }
  }
}
```

## Case: warn level and toString

covers: detect:levels

```java path=src/main/java/C.java
class C {
  void f() {
    try { run(); } catch (SQLException e) {
      log.warn("db {}", e.toString());  // CANDIDATE
    }
  }
}
```

## Case: an expected condition where the message is enough (Do not flag)

covers: do-not-flag:expected-condition

```java path=src/main/java/C.java
class C {
  void f(String s) {
    try { parse(s); } catch (NumberFormatException e) {
      log.warn("bad number: " + e.getMessage());
    }
    try { find(); } catch (UserNotFoundException e) {
      log.warn("missing: " + e.getMessage());
    }
  }
}
```

## Case: a block that rethrows the original (Do not flag)

covers: do-not-flag:rethrows

```java path=src/main/java/C.java
class C {
  void f() {
    try { run(); } catch (IOException e) {
      log.error("Failed: " + e.getMessage());
      throw new IllegalStateException("failed", e);
    }
  }
}
```

## Case: info level and a log outside a catch

covers: detect:level-only

```java path=src/main/java/C.java
class C {
  void f(Exception e) {
    try { run(); } catch (IOException ex) {
      log.info("Failed: " + ex.getMessage());
    }
    log.error("x " + e.getMessage());
  }
}
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    try { run(); } catch (IOException e) { log.error("Failed: " + e.getMessage()); }
  }
}
```
