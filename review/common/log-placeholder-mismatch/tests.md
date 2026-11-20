# Tests: common/log-placeholder-mismatch

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: a missing argument (Examples: Flag)

covers: example:flag,required:missing-argument

```java path=src/main/java/C.java
class C {
  void f(long id) {
    log.info("moved {} to {}", id);  // FLAG
  }
}
```

## Case: an extra argument

covers: detect:extra-argument,required:extra-argument

```java path=src/main/java/C.java
class C {
  void f(long id, long target) {
    log.info("moved {}", id, target);  // FLAG
    log.warn("no placeholders", id);  // FLAG
  }
}
```

## Case: matching counts (Examples: OK)

covers: example:ok

```java path=src/main/java/C.java
class C {
  void f(long id, long target) {
    log.info("moved {} to {}", id, target);
    log.debug("started");
    LOGGER.trace("{}", id);
  }
}
```

## Case: a trailing exception is not an argument (Do not flag: trailing exception)

covers: detect:trailing-exception,required:trailing-exception

```java path=src/main/java/C.java
class C {
  void f(long id, Exception e, RuntimeException ex) {
    log.error("failed {}", id, e);
    log.error("failed", e);
    log.warn("failed {} twice", id, ex);
  }
}
```

## Case: an exception that has a placeholder is an argument

covers: detect:trailing-exception

```java path=src/main/java/C.java
class C {
  void f(Exception e) {
    log.error("failed: {}", e);
    log.error("failed: {} {}", e);  // FLAG
  }
}
```

## Case: an exception named like a caught one when its type is unknown

covers: detect:trailing-exception

```java path=src/main/java/C.java
class C {
  void f(long id) {
    try { g(); } catch (UnknownException ex) {
      log.error("failed {}", id, ex);
    }
  }
}
```

## Case: an escaped placeholder is not counted

covers: detect:escaped

```java path=src/main/java/C.java
class C {
  void f(long id) {
    log.info("literal \\{} and {}", id);
    log.info("only literal \\{}");
  }
}
```

## Case: a format that is not a string literal (Do not flag: non-literal format)

covers: do-not-flag:non-literal-format,required:non-literal

```java path=src/main/java/C.java
class C {
  void f(String format, long id) {
    log.info(format, id, id);
    log.info(MESSAGE, id);
    log.info(prefix + "{}", id, id);
  }
}
```

## Case: String.format and printf are not logger calls (Do not flag: not a logger)

covers: do-not-flag:not-a-logger,required:string-format

```java path=src/main/java/C.java
class C {
  void f(long id) {
    String s = String.format("%s and %s", id);
    System.out.printf("moved {} to {}", id);
    out.info("{} {}", id);
  }
}
```

## Case: the order of the arguments is not checked (Do not flag: argument order)

covers: do-not-flag:argument-order

```java path=src/main/java/C.java
class C {
  void f(long id, String target) {
    log.info("moved {} to {}", target, id);
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(long id) {
    log.info("moved {} to {}", id);
  }
}
```

## Case: debug and trace levels are logger calls too

covers: detect:log-levels

```java path=src/main/java/C.java
class C {
  void f(long id) {
    log.debug("a {} b {}", id);  // FLAG
    log.trace("a {}");  // FLAG
  }
}
```

## Case: a method of the logger that is not a log call

covers: detect:log-levels-only

```java path=src/main/java/C.java
class C {
  void f(long id) {
    log.append("moved {} to {}", id);
    logger.setName("x {} y");
  }
}
```

## Case: numbered placeholders belong to other logging styles (Do not flag: numbered)

covers: do-not-flag:numbered-placeholders

```java path=src/main/java/C.java
class C {
  void f(long id) {
    logger.warn("failed: {0}", id);
    Logger.info("pi = {0.00}", Math.PI);
    logger.info("{0} and {1}", id);
  }
}
```

## Case: a single Object[] is the argument array (Do not flag: argument array)

covers: do-not-flag:argument-array

```java path=src/main/java/C.java
class C {
  void f(long a, long b) {
    log.info("a={} b={}", new Object[] { a, b });
    log.info("a={} b={}", new Object[] { a });  // FLAG
  }
}
```

## Case: a caught exception whose class cannot be resolved, and a variable of a generic exception type

covers: detect:trailing-exception

```java path=src/main/java/C.java
class C {
  <E extends Exception> void f(Class<E> type, Exception cause) {
    try { g(); } catch (UnknownAccountException uae) {
      log.error("Username Not Found!", uae);
    }
    E wrapped = type.cast(cause);
    log.error("Exception occurred.", wrapped);
  }
}
```

## Case: a variable of an unknown type that is not an exception is an extra argument

covers: detect:trailing-exception

```java path=src/main/java/C.java
class C {
  void f() {
    CustomFailure failure = null;
    log.error("failed", failure);  // FLAG
  }
}
```
