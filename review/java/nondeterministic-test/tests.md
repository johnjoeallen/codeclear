# Tests: java/nondeterministic-test

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: now() in an assertion (Examples: Flag)

covers: example:flag,required:now

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    assertEquals(LocalDate.now().plusDays(1), svc.nextDay());  // CANDIDATE
  }
}
```

## Case: a fixed clock and a fixed date (Examples: OK)

covers: example:ok,do-not-flag:fixed-clock,required:fixed-clock

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    assertEquals(LocalDate.of(2024, 1, 2), new Svc(fixedClock).nextDay());
    var now = Instant.now(fixedClock);
    var t = LocalDateTime.now(clock);
  }
}
```

## Case: a seeded Random (Do not flag)

covers: do-not-flag:seeded-random,required:seeded-random

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    var r = new Random(42);
    var d = new Date(0L);
  }
}
```

## Case: the other sources of variation

covers: detect:sources,required:sleep

```java path=src/test/java/CTest.java
class CTest {
  void f() throws Exception {
    var a = System.currentTimeMillis();  // CANDIDATE
    var b = new Random();  // CANDIDATE
    var c = new Date();  // CANDIDATE
    var d = Math.random();  // CANDIDATE
    var e = UUID.randomUUID();  // CANDIDATE
    var g = ThreadLocalRandom.current();  // CANDIDATE
    Thread.sleep(500);  // CANDIDATE
  }
}
```

## Case: a real remote host, and localhost (Do not flag: localhost)

covers: do-not-flag:localhost,required:wiremock

```java path=src/test/java/CTest.java
class CTest {
  void f() throws Exception {
    var a = new URL("https://api.example.com/x");  // CANDIDATE
    var b = new URL("http://localhost:8089/x");
    var c = new URL("http://127.0.0.1:8089/x");
  }
}
```

## Case: time used only for a unique name is left to the AI

covers: do-not-flag:not-asserted

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    var name = "user-" + System.currentTimeMillis();  // CANDIDATE
  }
}
```

## Case: production code is not checked

covers: detect:test-only

```java path=src/main/java/C.java
class C {
  long f() { return System.currentTimeMillis(); }
}
```
