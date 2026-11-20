# Tests: java/exception-cause-dropped

A `// FLAG` marker ends each line that must produce a finding.

## Case: cause omitted (Examples: Flag)

covers: example:flag,required:omitted

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new RepositoryException("load failed");  // FLAG
    }
  }
}
```

## Case: cause passed (Examples: OK)

covers: example:ok,do-not-flag:cause-passed,required:passed

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new RepositoryException("load failed", e);
    }
  }
}
```

## Case: the cause is passed in a different position

covers: do-not-flag:cause-passed

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new RepositoryException(e, "load failed");
    }
  }
}
```

## Case: e.getCause() is passed

covers: do-not-flag:cause-passed,required:get-cause

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (RuntimeException e) {
      throw new RepositoryException("load failed", e.getCause());
    }
  }
}
```

## Case: only the message of the cause is used

covers: detect:message-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new RepositoryException(e.getMessage());  // FLAG
    }
  }
}
```

## Case: nested catch: the inner parameter is the one that counts

covers: detect:nested-catch,required:nested

```java path=src/main/java/C.java
class C {
  void f() {
    try { a(); } catch (IOException outer) {
      try { b(); } catch (SQLException inner) {
        throw new RepositoryException("inner", inner);
      }
      try { c(); } catch (SQLException inner2) {
        throw new RepositoryException("inner2", outer);  // FLAG
      }
    }
  }
}
```

## Case: throw outside a catch

covers: detect:in-catch-only,required:outside-catch

```java path=src/main/java/C.java
class C {
  void f() {
    throw new IllegalStateException("no cause here");
  }
}
```

## Case: rethrowing a variable is not a new exception

covers: detect:new-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (RuntimeException e) {
      throw e;
    }
  }
}
```

## Case: a comment says the cause is discarded on purpose (Do not flag: comment)

covers: do-not-flag:comment

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (AuthException e) {
      // the cause is hidden on purpose: it would leak which account exists
      throw new AccessDeniedException("denied");
    }
  }
}
```

## Case: the exception class in this file has no constructor taking a cause (Do not flag: no cause constructor)

covers: do-not-flag:no-cause-constructor

```java path=src/main/java/C.java
class AppException extends RuntimeException {
  AppException(String message) { super(message); }
}
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new AppException("load failed");
    }
  }
}
```

## Case: the exception class in this file does take a cause

covers: detect:cause-constructor

```java path=src/main/java/C.java
class AppException extends RuntimeException {
  AppException(String message) { super(message); }
  AppException(String message, Throwable cause) { super(message, cause); }
}
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new AppException("load failed");  // FLAG
    }
  }
}
```

## Case: addSuppressed with the caught exception

covers: do-not-flag:cause-passed

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      Failure failure = new Failure("load");
      failure.addSuppressed(e);
      throw new RepositoryException("load failed");
    }
  }
}
```

## Case: the cause is passed through a local variable (Do not flag: cause passed)

covers: do-not-flag:cause-passed

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (ExecutionException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new IllegalStateException(cause);
    }
  }
}
```

## Case: a local variable that holds only the message

covers: detect:message-only

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      String text = e.getMessage();
      throw new RepositoryException(text);  // FLAG
    }
  }
}
```

## Case: a JDK exception with no constructor that takes a cause (Do not flag: no cause constructor)

covers: do-not-flag:no-cause-constructor

```java path=src/main/java/C.java
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
class C {
  void f(Future<?> future) {
    try { future.get(1, TimeUnit.SECONDS); } catch (TimeoutException e) {
      throw new TimeoutException("not ready");
    }
  }
}
```

## Case: a JDK exception that does take a cause

covers: detect:jdk-cause-constructor

```java path=src/main/java/C.java
class C {
  void f() {
    try { load(); } catch (SQLException e) {
      throw new IllegalStateException("load failed");  // FLAG
    }
  }
}
```
