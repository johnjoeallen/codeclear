# Tests: java/test-without-meaningful-assertion

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: no assertion (Detect)

covers: example:flag,required:no-assertion

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void createsAnOrder() {  // FLAG
    service.create(new Order());
  }
}
```

## Case: only assertNotNull (Detect)

covers: detect:weak,required:not-null-only

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void loads() {  // FLAG
    Order o = service.load(1);
    assertNotNull(o);
    assertNotNull(o.id());
  }
}
```

## Case: only assertThat(x).isNotNull()

covers: detect:weak

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void loads() {  // FLAG
    assertThat(service.load(1)).isNotNull();
  }
}
```

## Case: a real assertion next to a not-null check

covers: detect:weak-needs-all

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void loads() {
    Order o = service.load(1);
    assertNotNull(o);
    assertEquals(1, o.id());
  }
  @Test
  void chain() {
    assertThat(service.load(1)).isNotNull().isEqualTo(expected);
    assertThat(service.name()).isEqualTo("x");
  }
}
```

## Case: JUnit 5 annotations other than @Test

covers: detect:annotations

```java path=src/test/java/CTest.java
class CTest {
  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void a(int n) {  // FLAG
    service.run(n);
  }
  @RepeatedTest(3)
  void b() {  // FLAG
    service.run(1);
  }
}
```

## Case: assertThrows and assertDoesNotThrow (Do not flag: exception assertions)

covers: do-not-flag:exception-assertion,required:assert-throws

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void fails() {
    assertThrows(IllegalStateException.class, () -> service.run());
  }
  @Test
  void works() {
    assertDoesNotThrow(() -> service.run());
  }
}
```

## Case: @Test(expected = ...) (Do not flag: exception assertions)

covers: do-not-flag:exception-assertion

```java path=src/test/java/CTest.java
class CTest {
  @Test(expected = IllegalStateException.class)
  public void fails() {
    service.run();
  }
}
```

## Case: an @Test without an expected member is still checked

covers: detect:expected-member

```java path=src/test/java/CTest.java
class CTest {
  @Test(timeout = 100)
  public void slow() {  // FLAG
    service.run();
  }
}
```

## Case: assertions delegated to a helper (Do not flag: helper)

covers: do-not-flag:helper,required:helper

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void a() { verifyOrder(1, 2); }
  @Test
  void b() { checkTotals(order); }
  @Test
  void c() { expectNoErrors(); }
  @Test
  void d() { assertValid(order); }
}
```

## Case: Mockito, BDD, StepVerifier and MockMvc assertions

covers: detect:other-assertions

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void a() {
    service.run();
    verify(repo).save(any());
  }
  @Test
  void b() {
    service.run();
    then(repo).should().save(any());
  }
  @Test
  void c() {
    StepVerifier.create(flux).expectNext(1).verifyComplete();
  }
  @Test
  void d() throws Exception {
    mockMvc.perform(get("/x")).andExpect(status().isOk());
  }
  @Test
  void e() {
    fail("not yet");
  }
}
```

## Case: the context-loads smoke test (Do not flag: smoke test)

covers: do-not-flag:context-loads

```java path=src/test/java/CTest.java
class AppTests {
  @Test
  void contextLoads() {
  }
}
```

## Case: methods that are not tests

covers: detect:test-annotation

```java path=src/test/java/CTest.java
class CTest {
  void helper() {
    service.run();
  }
  @BeforeEach
  void setUp() {
    service.reset();
  }
}
```

## Case: a helper of the same class that asserts, whatever it is called (Do not flag: helper)

covers: do-not-flag:helper

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void a() { testAdding(1, 2); }
  @Test
  void b() { this.parse("x"); }
  @Test
  void c() { outer(1); }
  void testAdding(int x, int y) { assertEquals(x, y); }
  void parse(String s) { assertThat(s).isEqualTo("x"); }
  void outer(int x) { testAdding(x, x); }
}
```

## Case: a helper that does not assert, or has another parameter count

covers: detect:helper-asserts

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void a() {  // FLAG
    build(1);
  }
  @Test
  void b() {  // FLAG
    testAdding(1);
  }
  @Test
  void c() {  // FLAG
    other.testAdding(1, 2);
  }
  void build(int x) { service.create(x); }
  void testAdding(int x, int y) { assertEquals(x, y); }
}
```

## Case: a helper with a varargs parameter

covers: do-not-flag:helper

```java path=src/test/java/CTest.java
class CTest {
  @Test
  void none() { check(); }
  @Test
  void many() { check(1, 2, 3); }
  void check(double... values) { assertEquals(0, values.length); }
}
```
