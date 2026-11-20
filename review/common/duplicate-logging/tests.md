# Tests: common/duplicate-logging

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: log and rethrow (Examples: Flag)

covers: example:flag,required:log-rethrow

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (SQLException e) {
      log.error("query failed", e);  // CANDIDATE
      throw new DataException(e);
    }
  }
}
```

## Case: throw with the cause only (Examples: OK)

covers: example:ok,required:cause-only

```java path=src/main/java/C.java
class C {
  void f(long id) {
    try { a(); } catch (SQLException e) {
      throw new DataException("query failed for " + id, e);
    }
  }
}
```

## Case: log then return (no candidate)

covers: do-not-flag:handled,required:log-return

```java path=src/main/java/C.java
class C {
  int f() {
    try { return a(); } catch (SQLException e) {
      log.error("query failed", e);
      return 0;
    }
  }
}
```

## Case: debug and trace logs, and a log without the exception (Do not flag)

covers: do-not-flag:low-level-or-no-exception,required:debug-rethrow

```java path=src/main/java/C.java
class C {
  void f(long id) {
    try { a(); } catch (SQLException e) {
      log.debug("query failed", e);
      log.trace("query failed {}", e);
      log.error("while saving order {}", id);
      throw e;
    }
  }
}
```

## Case: a wrapper that has the exception as its cause

covers: detect:wrap

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {
      LOGGER.warn("failed: {}", e.getMessage());  // CANDIDATE
      throw new IllegalStateException("could not read", e);
    }
  }
}
```

## Case: a throw that does not carry the exception

covers: detect:same-exception

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {
      log.error("failed", e);
      throw new IllegalStateException("could not read");
    }
  }
}
```

## Case: boundary methods and controller advice (Do not flag: designated logging point)

covers: do-not-flag:boundary

```java path=src/main/java/C.java
class C {
  @ExceptionHandler(IOException.class)
  void handle() {
    try { a(); } catch (IOException e) { log.error("failed", e); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "x", e); }
  }
  @KafkaListener(topics = "t")
  void onMessage(String m) {
    try { a(); } catch (IOException e) { log.error("failed", e); throw new IllegalStateException(e); }
  }
}
```

```java path=src/main/java/Errors.java
@RestControllerAdvice
class Errors {
  void translate() {
    try { a(); } catch (IOException e) { log.error("failed", e); throw new IllegalStateException(e); }
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    try { a(); } catch (IOException e) { log.error("failed", e); throw e; }
  }
}
```
