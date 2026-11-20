# Tests: java/blocking-in-synchronized-or-event-loop

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an HTTP call in a synchronized block (Examples: Flag)

covers: example:flag,required:http-in-synchronized

```java path=src/main/java/C.java
class C {
  private final RestTemplate client;
  void f(String id) {
    synchronized (lock) {
      var r = client.getForObject("http://x", String.class);  // CANDIDATE
      cache.put(id, r);
    }
  }
}
```

## Case: fetch outside the lock (Examples: OK)

covers: example:ok,required:no-blocking

```java path=src/main/java/C.java
class C {
  private final RestTemplate client;
  void f(String id) {
    var r = client.getForObject("http://x", String.class);
    synchronized (lock) {
      cache.put(id, r);
    }
  }
}
```

## Case: a synchronized method and the other blocking calls

covers: detect:blocking-calls

```java path=src/main/java/C.java
class C {
  synchronized void a(Future<String> f, BlockingQueue<String> q) throws Exception {
    Thread.sleep(10);  // CANDIDATE
    f.get();  // CANDIDATE
    q.take();  // CANDIDATE
    Files.readAllBytes(path);  // CANDIDATE
    cache.put("a", "b");
  }
}
```

## Case: block inside a reactive operator

covers: required:block-in-map

```java path=src/main/java/C.java
class C {
  Mono<String> f(Mono<String> in) {
    return in.map(x -> other(x).block());  // CANDIDATE
  }
}
```

## Case: a dedicated scheduler (Do not flag)

covers: do-not-flag:dedicated-scheduler,required:bounded-elastic

```java path=src/main/java/C.java
class C {
  Mono<String> f(Mono<String> in) {
    return in.publishOn(Schedulers.boundedElastic()).map(x -> other(x).block());
  }
  Mono<String> g() {
    return Mono.fromCallable(() -> other().block()).subscribeOn(Schedulers.boundedElastic());
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  synchronized void a() throws Exception { Thread.sleep(10); }
}
```
