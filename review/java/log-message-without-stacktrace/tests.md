# Tests: java/log-message-without-stacktrace

A `// FLAG` marker ends each line that must produce a finding.

## Case: getMessage only (Detect)

covers: example:flag,required:message-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception e) {
      log.warn("failed: " + e.getMessage());  // FLAG
    }
  }
}
```

## Case: message as a placeholder argument, toString, error level

covers: detect:message-forms

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (RuntimeException e) {
      log.error("failed {}", e.getMessage());  // FLAG
      log.error("failed " + e.toString());  // FLAG
      LOGGER.warn(e.getMessage());  // FLAG
    }
  }
}
```

## Case: Throwable and unchecked types

covers: detect:exception-types

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Throwable t) {
      log.error("x " + t.getMessage());  // FLAG
    }
    try { g(); } catch (IllegalStateException e) {
      log.warn("x " + e.getMessage());  // FLAG
    }
    try { g(); } catch (IOException | RuntimeException e) {
      log.warn("x " + e.getMessage());  // FLAG
    }
  }
}
```

## Case: message plus the exception (Do not flag: throwable passed)

covers: do-not-flag:throwable-passed,required:message-plus-e

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception e) {
      log.warn("failed: " + e.getMessage(), e);
      log.error("failed {}", e.getMessage(), e);
      log.error("failed", e);
    }
  }
}
```

## Case: specific expected exceptions (Do not flag: specific exceptions)

covers: do-not-flag:specific-exception,required:number-format

```java path=src/main/java/C.java
class C {
  void f(String s) {
    try { Integer.parseInt(s); } catch (NumberFormatException e) {
      log.warn("not a number: " + e.getMessage());
    }
    try { parse(s); } catch (ParseException e) {
      log.warn("bad input: " + e.getMessage());
    }
    try { get(); } catch (TimeoutException e) {
      log.error("timed out: " + e.getMessage());
    }
  }
}
```

## Case: console output is not a logger call

covers: detect:logger-only,required:println

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
```

## Case: info and debug levels

covers: detect:warn-error-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception e) {
      log.info("failed: " + e.getMessage());
      log.debug("failed: " + e.getMessage());
    }
  }
}
```

## Case: a logger call outside a catch, and a message of another exception

covers: detect:in-catch,detect:own-exception

```java path=src/main/java/C.java
class C {
  void f(Exception other) {
    log.warn("x " + other.getMessage());
    try { g(); } catch (Exception e) {
      log.warn("other: " + other.getMessage());
    }
  }
}
```

## Case: test sources (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    try { g(); } catch (Exception e) {
      log.warn("failed: " + e.getMessage());
    }
  }
}
```

## Case: warn on a receiver that is not a logger

covers: detect:logger-only

```java path=src/main/java/C.java
class C {
  void f(Alerts alerts) {
    try { g(); } catch (Exception e) {
      alerts.warn("failed: " + e.getMessage());
      this.error("failed: " + e.getMessage());
    }
  }
}
```

## Case: only the class name of the exception is logged

covers: detect:message-calls-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception ex) {
      log.warn("failed: type={}", ex.getClass().getSimpleName());
    }
  }
}
```

## Case: getLocalizedMessage counts as the message

covers: detect:message-forms

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception e) {
      log.warn("failed: " + e.getLocalizedMessage());  // FLAG
    }
  }
}
```
