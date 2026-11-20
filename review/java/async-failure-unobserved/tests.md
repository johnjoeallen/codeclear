# Tests: java/async-failure-unobserved

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an @Async void method (Examples: Flag)

covers: example:flag,required:async-void

```java path=src/main/java/C.java
class C {
  @Async
  public void send(Mail m) {  // CANDIDATE
    mailer.send(m);
  }
}
```

## Case: a method that catches, and one that returns a future (Examples: OK)

covers: example:ok,do-not-flag:try-catch,required:try-catch

```java path=src/main/java/C.java
class C {
  @Async
  public void send(Mail m) {
    try { mailer.send(m); } catch (Exception e) { log.error("send failed", e); }
  }
  @Async
  public CompletableFuture<Void> later(Mail m) {
    return CompletableFuture.completedFuture(null);
  }
}
```

## Case: discarded executor and CompletableFuture tasks

covers: detect:discarded

```java path=src/main/java/C.java
class C {
  private final ExecutorService pool;
  void f() {
    pool.submit(() -> work());  // CANDIDATE
    pool.execute(() -> work());  // CANDIDATE
    CompletableFuture.runAsync(() -> work());  // CANDIDATE
    CompletableFuture.supplyAsync(() -> work()).thenAccept(r -> use(r));  // CANDIDATE
  }
}
```

## Case: futures that are returned, stored, joined or handled (Do not flag)

covers: do-not-flag:future-handled,required:stored

```java path=src/main/java/C.java
class C {
  private final ExecutorService pool;
  Future<?> a() { return pool.submit(() -> work()); }
  void b() {
    Future<?> f = pool.submit(() -> work());
    CompletableFuture.supplyAsync(() -> work()).exceptionally(e -> null);
    CompletableFuture.runAsync(() -> work()).whenComplete((r, e) -> log(e));
    CompletableFuture.runAsync(() -> work()).join();
  }
}
```

## Case: a task that catches and logs (Do not flag)

covers: do-not-flag:task-catches

```java path=src/main/java/C.java
class C {
  private final ExecutorService pool;
  void f() {
    pool.submit(() -> { try { work(); } catch (Exception e) { log.error("x", e); } });
  }
}
```

## Case: a handler configured elsewhere in the project (Do not flag)

covers: do-not-flag:handler-configured

```java path=src/main/java/C.java
class C {
  @Async
  public void send(Mail m) { mailer.send(m); }
}
```

```java path=src/main/java/Cfg.java
class Cfg implements AsyncConfigurer {
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() { return (ex, m, p) -> log.error("x", ex); }
}
```
