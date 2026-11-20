# Tests: java/integer-overflow-risk

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an int product with time factors (Examples: Flag)

covers: example:flag,required:int-product

```java path=src/main/java/C.java
class C {
  void f(int days) {
    int timeoutMs = days * 24 * 60 * 60 * 1000;  // CANDIDATE
  }
}
```

## Case: a long operand (Examples: OK)

covers: example:ok,do-not-flag:long-operand,required:long-literal

```java path=src/main/java/C.java
class C {
  void f(int days) {
    long timeoutMs = days * 24L * 60 * 60 * 1000;
    long a = (long) days * 60 * 1000;
    long b = TimeUnit.DAYS.toMillis(days);
  }
}
```

## Case: other factors and the outermost product only

covers: detect:factors

```java path=src/main/java/C.java
class C {
  void f(int n, int mb) {
    int bytes = mb * 1024 * 1024;  // CANDIDATE
    int s = n * 3600;  // CANDIDATE
    int yearly = n * 365;  // CANDIDATE
  }
}
```

## Case: constants and unrelated multiplications (Do not flag)

covers: do-not-flag:constants

```java path=src/main/java/C.java
class C {
  static final int DAY_MS = 24 * 60 * 60 * 1000;
  void f(int a, int b) {
    int c = a * b;
    int d = a * 2;
  }
}
```

## Case: a test source

covers: detect:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(int days) { int ms = days * 24 * 60 * 60 * 1000; }
}
```
