# Tests: java/generic-exception-in-public-api

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: throws Exception on a public method (Detect)

covers: example:flag,required:throws-exception

```java path=src/main/java/C.java
public class Service {
  public void load() throws Exception {  // CANDIDATE
  }
}
```

## Case: throws Throwable, qualified names and several exceptions

covers: detect:throws-throwable

```java path=src/main/java/C.java
public class Service {
  public void a() throws Throwable {  // CANDIDATE
  }
  public void b() throws java.lang.Exception {  // CANDIDATE
  }
  public void c() throws java.io.IOException, Exception {  // CANDIDATE
  }
}
```

## Case: throw new RuntimeException in a public method (Detect)

covers: detect:throw-runtime,required:throw-runtime

```java path=src/main/java/C.java
public class Service {
  public void load() {
    throw new RuntimeException("failed");  // CANDIDATE
  }
}
```

## Case: throw new Exception and Throwable

covers: detect:throw-generic

```java path=src/main/java/C.java
public class Service {
  public void a() throws Exception {  // CANDIDATE
    throw new Exception("x");  // CANDIDATE
  }
  public void b() {
    throw new Throwable("y");  // CANDIDATE
  }
}
```

## Case: specific exceptions are fine

covers: detect:generic-only

```java path=src/main/java/C.java
public class Service {
  public void a() throws java.io.IOException, java.sql.SQLException {
  }
  public void b() {
    throw new IllegalStateException("x");
  }
}
```

## Case: an override of an interface method (Do not flag: override)

covers: do-not-flag:override,required:override

```java path=src/main/java/C.java
public class Task implements Callable<String> {
  @Override
  public String call() throws Exception {
    return "x";
  }
  @Override
  public void close() {
    throw new RuntimeException("x");
  }
}
```

## Case: private and package-private methods (Do not flag: not public)

covers: do-not-flag:not-public-api,required:private

```java path=src/main/java/C.java
public class Service {
  private void a() throws Exception {
    throw new RuntimeException("x");
  }
  void b() throws Exception {
    throw new RuntimeException("y");
  }
  protected void c() throws Exception {
  }
}
```

## Case: a public method of a type that is not public

covers: do-not-flag:not-public-api

```java path=src/main/java/C.java
class Hidden {
  public void a() throws Exception {
    throw new RuntimeException("x");
  }
}
```

## Case: a main method

covers: do-not-flag:not-public-api

```java path=src/main/java/C.java
public class App {
  public static void main(String[] args) throws Exception {
    throw new RuntimeException("x");
  }
}
```

## Case: test sources

covers: do-not-flag:not-public-api

```java path=src/test/java/ServiceTest.java
public class ServiceTest {
  public void load() throws Exception {
    throw new RuntimeException("x");
  }
}
```

## Case: a public interface declares throws Exception

covers: detect:interface

```java path=src/main/java/C.java
public interface Loader {
  void load() throws Exception;  // CANDIDATE
}
```

## Case: a constructor and an initializer are not methods

covers: detect:methods-only

```java path=src/main/java/C.java
public class Service {
  public Service() {
    throw new RuntimeException("x");
  }
  static {
    if (true) throw new RuntimeException("y");
  }
}
```

## Case: rethrowing a caught exception is not a new generic one

covers: detect:new-only

```java path=src/main/java/C.java
public class Service {
  public void f() {
    try { a(); } catch (RuntimeException e) {
      throw e;
    }
  }
}
```

## Case: framework entry points (Do not flag: framework signature)

covers: do-not-flag:framework-entry

```java path=src/main/java/C.java
public class Config {
  @Bean
  public SecurityFilterChain chain(HttpSecurity http) throws Exception {
    throw new RuntimeException("x");
  }
  @Around("execution(* *(..))")
  public Object around(ProceedingJoinPoint p) throws Throwable {
    throw new RuntimeException("x");
  }
  @PostConstruct
  public void init() throws Exception {
    throw new RuntimeException("x");
  }
  @Test
  public void works() throws Exception {
    throw new RuntimeException("x");
  }
}
```

## Case: lifecycle and entry-point methods by name (Do not flag: framework signature)

covers: do-not-flag:framework-entry

```java path=src/main/java/C.java
public class Runner {
  public void run(String... args) throws Exception {
    throw new RuntimeException("x");
  }
  public String call() throws Exception {
    throw new RuntimeException("x");
  }
  public void close() throws Exception {
    throw new RuntimeException("x");
  }
  public void afterPropertiesSet() throws Exception {
    throw new RuntimeException("x");
  }
  public void destroy() throws Exception {
    throw new RuntimeException("x");
  }
  public static void main(String[] args) throws Exception {
    throw new RuntimeException("x");
  }
}
```

## Case: an override with any name

covers: do-not-flag:override

```java path=src/main/java/C.java
public class Impl implements Loader {
  @Override
  public Object load() throws Exception {
    throw new RuntimeException("x");
  }
}
```

## Case: a public method that is not a framework entry point

covers: detect:framework-entry-only

```java path=src/main/java/C.java
public class Config {
  @Deprecated
  public void load() throws Exception {  // CANDIDATE
  }
}
```
