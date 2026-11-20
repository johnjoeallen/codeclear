# Tests: java/broad-catch-swallowed

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: return null from a catch of Exception (Examples: Flag)

covers: example:flag,required:return-null

```java path=src/main/java/C.java
class C {
  Order f(long id) {
    try { return client.fetch(id); } catch (Exception e) { return null; }  // CANDIDATE
  }
}
```

## Case: a specific exception that is wrapped and thrown (Examples: OK)

covers: example:ok,required:specific-type

```java path=src/main/java/C.java
class C {
  Order f(long id) {
    try { return client.fetch(id); } catch (IOException e) { throw new FetchException(id, e); }
  }
}
```

## Case: rethrow (no candidate)

covers: required:rethrow,do-not-flag:rethrows-error

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) { throw new IllegalStateException(e); }
    try { b(); } catch (Throwable t) {
      if (t instanceof Error) { throw (Error) t; }
      cleanup();
    }
  }
}
```

## Case: the other success-looking values and the other broad types

covers: detect:values

```java path=src/main/java/C.java
class C {
  boolean f() {
    try { a(); return true; } catch (RuntimeException e) { return true; }  // CANDIDATE
  }
  List<String> g() {
    try { return b(); } catch (Throwable t) { return Collections.emptyList(); }  // CANDIDATE
  }
  Optional<String> h() {
    try { return c(); } catch (Exception e) { return Optional.empty(); }  // CANDIDATE
  }
}
```

## Case: falling through to code after the try

covers: detect:fall-through

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) {  // CANDIDATE
      count++;
    }
    next();
  }
}
```

## Case: a return of a failure value (no candidate)

covers: detect:failure-value

```java path=src/main/java/C.java
class C {
  Result f() {
    try { return Result.ok(a()); } catch (Exception e) { return Result.failed(e.getMessage()); }
  }
}
```

## Case: boundary methods (Do not flag: boundary)

covers: do-not-flag:boundary

```java path=src/main/java/C.java
class C {
  public static void main(String[] args) {
    try { run(); } catch (Exception e) { System.exit(1); }
  }
  public void run() {
    try { a(); } catch (Exception e) { count++; }
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<String> handle() {
    try { a(); } catch (Exception e) { return null; }
    return null;
  }
  @Scheduled(fixedRate = 1000)
  void tick() {
    try { a(); } catch (Exception e) { return; }
  }
  @KafkaListener(topics = "t")
  void onMessage(String m) {
    try { a(); } catch (Exception e) { return; }
  }
}
```

## Case: the failure is recorded in a field (Do not flag: records the failure)

covers: do-not-flag:records-failure

```java path=src/main/java/C.java
class C {
  private Exception lastFailure;
  boolean f() {
    try { a(); return true; } catch (Exception e) { lastFailure = e; return false; }
  }
}
```

## Case: an empty or log-only block belongs to other rules

covers: detect:other-rules

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (Exception e) { }
    try { b(); } catch (Exception e) { log.error("b failed", e); }
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  Order f() {
    try { return client.fetch(1); } catch (Exception e) { return null; }
  }
}
```
