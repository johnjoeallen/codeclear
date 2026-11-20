# Tests: java/completablefuture-blocking-or-unbounded-wait

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: join with no timeout (Examples: Flag)

covers: example:flag,required:join-no-timeout

```java path=src/main/java/C.java
class C {
  Object f(long id) {
    var r = CompletableFuture.supplyAsync(() -> compute(id), pool).join();  // CANDIDATE
    return r;
  }
}
```

## Case: orTimeout and a bounded get (Examples: OK)

covers: example:ok,do-not-flag:bounded,required:or-timeout

```java path=src/main/java/C.java
class C {
  Object f(long id, CompletableFuture<String> fut) throws Exception {
    var a = CompletableFuture.supplyAsync(() -> compute(id), pool).orTimeout(2, TimeUnit.SECONDS).join();
    var b = fut.get(2, TimeUnit.SECONDS);
    var c = fut.completeOnTimeout("x", 1, TimeUnit.SECONDS).join();
    return a;
  }
}
```

## Case: get and join on a variable typed as a future

covers: detect:declared-type

```java path=src/main/java/C.java
class C {
  void f(CompletableFuture<String> fut, Future<String> plain, Map<String, String> map) throws Exception {
    fut.join();  // CANDIDATE
    fut.get();  // CANDIDATE
    plain.get();  // CANDIDATE
    map.get("k");
  }
}
```

## Case: blocking work on the default pool

covers: required:default-pool

```java path=src/main/java/C.java
class C {
  void f() {
    CompletableFuture.supplyAsync(() -> client.getForObject("http://x", String.class));  // CANDIDATE
    CompletableFuture.runAsync(() -> { Thread.sleep(100); });  // CANDIDATE
  }
}
```

## Case: a custom executor and non-blocking work (Do not flag)

covers: do-not-flag:custom-executor,required:custom-executor

```java path=src/main/java/C.java
class C {
  void f() {
    CompletableFuture.supplyAsync(() -> client.getForObject("http://x", String.class), pool);
    CompletableFuture.supplyAsync(() -> a + b);
  }
}
```

## Case: thenCompose that joins another future

covers: detect:then-compose

```java path=src/main/java/C.java
class C {
  void f(CompletableFuture<String> a) {
    a.thenCompose(x -> other(x).join());  // CANDIDATE
    a.thenCompose(x -> other(x));
  }
}
```

## Case: a future known to be complete is left to the AI

covers: do-not-flag:known-complete

```java path=src/main/java/C.java
class C {
  void f(CompletableFuture<Void> a, CompletableFuture<Void> b) {
    CompletableFuture.allOf(a, b).join();  // CANDIDATE
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(CompletableFuture<String> fut) { fut.join(); }
}
```
