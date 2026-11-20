# Tests: java/log-and-rethrow

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: log and rethrow (Detect)

covers: example:flag,required:log-rethrow

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {  // CANDIDATE
      log.error("failed", e);
      throw e;
    }
  }
}
```

## Case: log and wrap (Detect)

covers: detect:wrap,required:log-wrap

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {  // CANDIDATE
      log.warn("failed: " + e.getMessage());
      throw new IllegalStateException("could not read", e);
    }
  }
}
```

## Case: rethrow only (no candidate)

covers: detect:log-needed,required:rethrow-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
```

## Case: log only is the catch-log-and-continue rule's business

covers: detect:throw-needed,required:log-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {
      log.error("failed", e);
    }
  }
}
```

## Case: a log call that adds context without the exception (Do not flag: adds context)

covers: do-not-flag:log-adds-context

```java path=src/main/java/C.java
class C {
  void f(long id) {
    try { save(id); } catch (SQLException e) {
      log.error("while saving order {}", id);
      throw e;
    }
  }
}
```

## Case: info and debug levels

covers: detect:warn-error-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException e) {
      log.info("failed", e);
      log.debug("failed", e);
      throw e;
    }
  }
}
```

## Case: boundary methods that log and translate (Do not flag: boundary)

covers: do-not-flag:boundary

```java path=src/main/java/C.java
class C {
  @ExceptionHandler(IOException.class)
  void handle(HttpServletRequest r) {
    try { a(); } catch (IOException e) { log.error("failed", e); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY); }
  }
  @KafkaListener(topics = "t")
  void onMessage(String m) {
    try { a(); } catch (IOException e) { log.error("failed", e); throw new IllegalStateException(e); }
  }
}
```

## Case: a class that is controller advice

covers: do-not-flag:boundary

```java path=src/main/java/C.java
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

## Case: a log call about another exception

covers: detect:same-exception

```java path=src/main/java/C.java
class C {
  void f(Exception other) {
    try { a(); } catch (IOException e) {
      log.error("other failed", other);
      throw e;
    }
  }
}
```

## Case: a catch outside any method (a static initializer)

covers: detect:no-enclosing-method

```java path=src/main/java/C.java
class C {
  static {
    try { a(); } catch (IOException e) {  // CANDIDATE
      log.error("failed", e);
      throw new ExceptionInInitializerError(e);
    }
  }
}
```
