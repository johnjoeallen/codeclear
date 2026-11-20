# Tests: java/bigdecimal-misuse

A `// FLAG` marker ends each line that must produce a finding.

## Prelude

```java
import java.math.BigDecimal;
```

## Case: new BigDecimal(0.1) (Examples: Flag)

covers: example:flag,required:double-literal

```java path=src/main/java/C.java
class C {
  BigDecimal f() {
    return new BigDecimal(0.1);  // FLAG
  }
}
```

## Case: float literal, double and float variables, double expression

covers: detect:double-or-float

```java path=src/main/java/C.java
class C {
  void f(double d, float fl, int n) {
    BigDecimal a = new BigDecimal(1.5f);  // FLAG
    BigDecimal b = new BigDecimal(d);  // FLAG
    BigDecimal c = new BigDecimal(fl);  // FLAG
    BigDecimal e = new BigDecimal(n * 0.5);  // FLAG
    BigDecimal g = new BigDecimal(-0.25);  // FLAG
  }
}
```

## Case: a String, valueOf and integer arguments (Do not flag: safe construction)

covers: do-not-flag:safe-construction,required:string,required:value-of

```java path=src/main/java/C.java
class C {
  void f(int n, long l) {
    BigDecimal a = new BigDecimal("0.1");
    BigDecimal b = BigDecimal.valueOf(0.1);
    BigDecimal c = new BigDecimal(5);
    BigDecimal d = new BigDecimal(5L);
    BigDecimal e = new BigDecimal(n);
    BigDecimal g = new BigDecimal(l);
  }
}
```

## Case: a constructor with no argument or another class

covers: detect:big-decimal-only

```java path=src/main/java/C.java
class C {
  void f(double d) {
    Object o = new Object();
    Double x = new Double(d);
    BigDecimal y = new BigDecimal("1");
  }
}
```

## Case: equals on a BigDecimal (Examples: Flag)

covers: example:flag,required:equals

```java path=src/main/java/C.java
class C {
  boolean f(BigDecimal price, BigDecimal other) {
    return price.equals(other);  // FLAG
  }
}
```

## Case: compareTo is the fix (Examples: OK)

covers: example:ok,required:compare-to

```java path=src/main/java/C.java
class C {
  boolean f(BigDecimal price, BigDecimal other) {
    return price.compareTo(other) == 0;
  }
}
```

## Case: equals on other types

covers: detect:big-decimal-only

```java path=src/main/java/C.java
class C {
  boolean f(String s, Integer i, Object o) {
    return s.equals("x") || i.equals(2) || o.equals(s) || java.util.Objects.equals(s, o);
  }
}
```

## Case: equals inside an equals implementation that compares scale (Do not flag: equals method)

covers: do-not-flag:equals-method

```java path=src/main/java/C.java
class Money {
  BigDecimal amount;
  @Override
  public boolean equals(Object other) {
    return other instanceof Money m && amount.equals(m.amount);
  }
}
```

## Case: the same call outside equals is reported

covers: detect:equals-method-only

```java path=src/main/java/C.java
class Money {
  BigDecimal amount;
  boolean same(Money m) {
    return amount.equals(m.amount);  // FLAG
  }
}
```
