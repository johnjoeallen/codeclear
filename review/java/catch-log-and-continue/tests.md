# Tests: java/catch-log-and-continue

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: log only in a method that returns a value (Examples: Detect)

covers: example:flag,required:log-only

```java path=src/main/java/C.java
class C {
  int load() {
    try { return read(); } catch (IOException e) {  // CANDIDATE
      log.error("could not read", e);
    }
    return 0;
  }
}
```

## Case: warn, info and printStackTrace

covers: detect:log-forms

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) {  // CANDIDATE
      log.warn("a failed: {}", e.getMessage());
    }
    try { b(); } catch (Exception e) {  // CANDIDATE
      LOGGER.info("b failed");
      e.printStackTrace();
    }
  }
}
```

## Case: log then rethrow (no candidate)

covers: detect:no-rethrow,required:log-rethrow

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) {
      log.error("failed", e);
      throw new IllegalStateException(e);
    }
  }
}
```

## Case: log then return a failure value

covers: detect:no-return

```java path=src/main/java/C.java
class C {
  boolean f() {
    try { a(); return true; } catch (Exception e) {
      log.error("failed", e);
      return false;
    }
  }
}
```

## Case: log and record the failure in a field

covers: detect:no-assignment

```java path=src/main/java/C.java
class C {
  private Exception failure;
  void f() {
    try { a(); } catch (Exception e) {
      log.error("failed", e);
      failure = e;
    }
  }
}
```

## Case: debug and trace logging only is the empty-catch rule's business

covers: detect:info-or-above

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) {
      log.debug("ignored", e);
    }
  }
}
```

## Case: an empty catch is the empty-catch rule's business

covers: detect:info-or-above

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) { }
  }
}
```

## Case: best-effort cleanup (Do not flag: cleanup)

covers: do-not-flag:cleanup,required:close

```java path=src/main/java/C.java
class C {
  void f(InputStream in, File file) {
    try { in.close(); } catch (IOException e) {
      log.warn("could not close", e);
    }
    try { file.delete(); } catch (SecurityException e) {
      log.warn("could not delete", e);
    }
  }
}
```

## Case: cleanup that does more than close or delete is still a candidate

covers: detect:cleanup-only

```java path=src/main/java/C.java
class C {
  void f(InputStream in) {
    try { in.close(); process(); } catch (IOException e) {  // CANDIDATE
      log.warn("failed", e);
    }
  }
}
```

## Case: top-level boundaries: main, run and listeners (Do not flag: boundary)

covers: do-not-flag:boundary

```java path=src/main/java/C.java
class C implements Runnable {
  public static void main(String[] args) {
    try { a(); } catch (Exception e) { log.error("failed", e); }
  }
  public void run() {
    try { a(); } catch (Exception e) { log.error("failed", e); }
  }
  @KafkaListener(topics = "t")
  void onMessage(String m) {
    try { a(); } catch (Exception e) { log.error("failed", e); }
  }
  @ExceptionHandler(Exception.class)
  void handle(Exception e) {
    try { a(); } catch (Exception x) { log.error("failed", x); }
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source,required:test-path

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    try { a(); } catch (Exception e) { log.error("failed", e); }
  }
}
```

## Case: a catch outside any method

covers: detect:no-method

```java path=src/main/java/C.java
class C {
  static {
    try { a(); } catch (Exception e) {  // CANDIDATE
      log.error("failed", e);
    }
  }
}
```

## Case: a call method is a boundary too

covers: do-not-flag:boundary

```java path=src/main/java/C.java
class C implements Callable<String> {
  public String call() {
    try { a(); } catch (Exception e) { log.error("failed", e); }
    return "x";
  }
}
```

## Case: a try block with no method call

covers: detect:cleanup-needs-calls

```java path=src/main/java/C.java
class C {
  void f(int[] values) {
    try { int x = values[5]; } catch (ArrayIndexOutOfBoundsException e) {  // CANDIDATE
      log.warn("bad index", e);
    }
  }
}
```
