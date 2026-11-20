# Tests: java/retry-without-bound-or-idempotency

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an endless retry (Examples: Flag)

covers: example:flag,required:endless

```java path=src/main/java/C.java
class C {
  void f() {
    while (true) {
      try {
        call();
        return;
      } catch (IOException e) {  // CANDIDATE
        log.warn("again");
      }
    }
  }
}
```

## Case: a counter and a backoff (Examples: OK)

covers: example:ok,do-not-flag:bounded-with-backoff,required:bounded

```java path=src/main/java/C.java
class C {
  void f() throws Exception {
    int attempts = 0;
    while (attempts < 5) {
      try {
        call();
        return;
      } catch (IOException e) {
        attempts++;
        Thread.sleep(100L << attempts);
      }
    }
  }
}
```

## Case: a bounded loop with no pause

covers: detect:no-backoff,required:no-sleep

```java path=src/main/java/C.java
class C {
  void f() {
    for (int i = 0; i < 3; i++) {
      try {
        call();
        return;
      } catch (IOException e) {  // CANDIDATE
        last = e;
      }
    }
  }
}
```

## Case: a retried write, with and without an idempotency key

covers: detect:write,do-not-flag:idempotent

```java path=src/main/java/C.java
class C {
  void a() throws Exception {
    for (int i = 0; i < 3; i++) {
      try {
        gateway.charge(order);
        return;
      } catch (IOException e) {  // CANDIDATE
        Thread.sleep(100);
      }
    }
  }
  void b() throws Exception {
    for (int i = 0; i < 3; i++) {
      try {
        gateway.charge(order, idempotencyKey);
        return;
      } catch (IOException e) {
        Thread.sleep(100);
      }
    }
  }
  String c() throws Exception {
    for (int i = 0; i < 3; i++) {
      try {
        return client.get(url);
      } catch (IOException e) {
        Thread.sleep(100);
      }
    }
    return null;
  }
}
```

## Case: a catch that leaves the loop, and a catch outside any loop (Do not flag)

covers: do-not-flag:leaves-loop

```java path=src/main/java/C.java
class C {
  void a() {
    while (true) {
      try { call(); } catch (IOException e) { throw new IllegalStateException(e); }
    }
  }
  void b() {
    try { call(); } catch (IOException e) { log.warn("x"); }
  }
}
```

## Case: @Retryable

covers: detect:retryable

```java path=src/main/java/C.java
class C {
  @Retryable  // CANDIDATE
  void a() { }
  @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100))
  void b() { }
  @Retryable(retryFor = Exception.class, maxAttempts = 3)  // CANDIDATE
  void c() { }
  @Retryable(retryFor = IOException.class, maxAttempts = 3)
  void d() { }
}
```
