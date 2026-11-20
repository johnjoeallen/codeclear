# Tests: java/floating-point-money

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: double amount in arithmetic (Detect)

covers: example:flag,required:double-amount

```java path=src/main/java/C.java
class C {
  double total(double price, int qty) {  // CANDIDATE
    double amount = 0;  // CANDIDATE
    amount += price * qty;
    return amount;
  }
}
```

## Case: fields, Double and float

covers: detect:types

```java path=src/main/java/C.java
class C {
  private double balance = 0;  // CANDIDATE
  private Double fee = 0.0;  // CANDIDATE
  float tax;  // CANDIDATE
  void add(double x) { balance += x; }
  void charge() { fee = fee * 2; }
  float net(float gross) { return gross - tax; }
}
```

## Case: BigDecimal (none)

covers: required:big-decimal

```java path=src/main/java/C.java
class C {
  BigDecimal amount = BigDecimal.ONE;
  BigDecimal total(BigDecimal price, int qty) { return price.multiply(BigDecimal.valueOf(qty)); }
}
```

## Case: names that denote ratios, metrics or scores (Do not flag: ratio)

covers: do-not-flag:ratio-name,required:ratio

```java path=src/main/java/C.java
class C {
  double taxRate = 0.2;
  double discountPercent = 10;
  double costRatio(double a, double b) { double ratio = a / b; return ratio * 2; }
  double priceScore = 1;
  void f() { taxRate = taxRate * 2; discountPercent += 1; priceScore = priceScore + 1; }
}
```

## Case: an unrelated name, and money that is only displayed

covers: detect:name

```java path=src/main/java/C.java
class C {
  double speed = 1;
  void f(double distance) { speed = speed * distance; }
  void show(double amount) { System.out.println("Amount: " + amount); }
}
```

## Case: rounding counts as arithmetic

covers: detect:rounding

```java path=src/main/java/C.java
class C {
  long cents(double price) {  // CANDIDATE
    return Math.round(price);
  }
}
```

## Case: a parameter that is only passed on

covers: detect:arithmetic

```java path=src/main/java/C.java
class C {
  void send(double amount) {
    gateway.charge(amount);
  }
}
```

## Case: test sources (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  double total(double price) { double amount = 0; amount += price * 2; return amount; }
}
```

## Case: every floating-point type, for variables and parameters

covers: detect:types

```java path=src/main/java/C.java
class C {
  void f(
      double amount,  // CANDIDATE
      float fee,  // CANDIDATE
      Double price,  // CANDIDATE
      Float tax) {  // CANDIDATE
    Float cost = 0f;  // CANDIDATE
    cost = cost + amount + fee + price + tax;
  }
}
```

## Case: other types and a ratio parameter

covers: detect:type-and-name

```java path=src/main/java/C.java
class C {
  void f(int amount, long total, double taxRate) {
    amount = amount + 1;
    total = total * 2;
    taxRate = taxRate * 2;
    int cost = 0;
    cost += amount;
  }
}
```

## Case: a count is not an amount (Do not flag: ratio)

covers: do-not-flag:ratio-name

```java path=src/main/java/C.java
class C {
  double totalCount = 0;
  void f(double itemCount) {
    totalCount = totalCount + itemCount;
  }
}
```
