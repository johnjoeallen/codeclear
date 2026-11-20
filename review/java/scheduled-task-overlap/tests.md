# Tests: java/scheduled-task-overlap

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a fixed-rate job that pulls from a remote client (Examples: Flag)

covers: example:flag,required:fixed-rate-remote

```java path=src/main/java/C.java
class C {
  private final RestTemplate client;
  @Scheduled(fixedRate = 1000)  // CANDIDATE
  void sync() { client.getForObject("http://x", String.class); }
}
```

## Case: fixedDelay with a lock (Examples: OK)

covers: example:ok,do-not-flag:fixed-delay,do-not-flag:lock,required:lock

```java path=src/main/java/C.java
class C {
  private final RestTemplate client;
  @Scheduled(fixedDelay = 60000)
  @SchedulerLock(name = "sync")
  void sync() { client.getForObject("http://x", String.class); }
  @Scheduled(fixedRate = 1000)
  @SchedulerLock(name = "sync2")
  void sync2() { client.getForObject("http://x", String.class); }
}
```

## Case: a short in-memory task (no candidate)

covers: required:short-task

```java path=src/main/java/C.java
class C {
  @Scheduled(fixedRate = 1000)
  void tick() { counter.incrementAndGet(); }
}
```

## Case: sleep, findAll and a loop

covers: detect:slow-work

```java path=src/main/java/C.java
class C {
  @Scheduled(fixedRate = 1000)  // CANDIDATE
  void a() throws Exception { Thread.sleep(5000); }
  @Scheduled(fixedRateString = "${rate}")  // CANDIDATE
  void b() { repo.findAll(); }
  @Scheduled(fixedRate = 1000)  // CANDIDATE
  void c() { for (Item i : items) { process(i); } }
}
```

## Case: cron and fixedDelay are not fixed-rate

covers: detect:fixed-rate-only

```java path=src/main/java/C.java
class C {
  @Scheduled(cron = "0 * * * * *")
  void a() throws Exception { Thread.sleep(5000); }
}
```

## Case: single-instance deployments stated in context stay candidates for the AI to drop

covers: do-not-flag:single-instance

```java path=src/main/java/C.java
class C {
  @Scheduled(fixedRate = 1000)  // CANDIDATE
  void a() throws Exception { Thread.sleep(5000); }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  @Scheduled(fixedRate = 1000)
  void a() throws Exception { Thread.sleep(5000); }
}
```
