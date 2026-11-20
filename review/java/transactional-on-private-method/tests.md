# Tests: java/transactional-on-private-method

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: private annotated method (Examples: Flag)

covers: example:flag,required:private

```java path=src/main/java/C.java
class OrderService {
  @Transactional
  private void save(Order o) {  // FLAG
  }
}
```

## Case: public annotated method (Examples: OK)

covers: example:ok,required:public

```java path=src/main/java/C.java
@Service
class OrderService {
  @Transactional
  public void save(Order o) {
  }
}
```

## Case: a final annotated method of a Spring bean

covers: detect:final

```java path=src/main/java/C.java
@Service
class OrderService {
  @Transactional
  public final void save(Order o) {  // FLAG
  }
}
```

## Case: a final annotated method of a class that is not a bean

covers: detect:final-bean-only

```java path=src/main/java/C.java
class Plain {
  @Transactional
  public final void save(Order o) {
  }
}
```

## Case: a self-call from a non-transactional method (Detect)

covers: detect:self-invocation,required:self-call

```java path=src/main/java/C.java
@Service
class OrderService {
  public void process(Order o) {
    save(o);  // FLAG MEDIUM
  }
  @Transactional
  public void save(Order o) { }
}
```

## Case: a self-call through this

covers: detect:self-invocation

```java path=src/main/java/C.java
@Service
class OrderService {
  public void process(Order o) {
    this.save(o);  // FLAG MEDIUM
  }
  @Transactional
  public void save(Order o) { }
}
```

## Case: a self-call from a transactional method (Do not flag: transactional caller)

covers: do-not-flag:transactional-caller,required:transactional-caller

```java path=src/main/java/C.java
@Service
class OrderService {
  @Transactional
  public void process(Order o) {
    save(o);
  }
  @Transactional
  public void save(Order o) { }
}
```

## Case: a self-call from a class that is transactional

covers: do-not-flag:transactional-caller

```java path=src/main/java/C.java
@Service
@Transactional
class OrderService {
  public void process(Order o) {
    save(o);
  }
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void save(Order o) { }
}
```

## Case: a call through another bean goes through the proxy

covers: detect:own-class

```java path=src/main/java/C.java
@Service
class OrderService {
  private final PaymentService payments;
  public void process(Order o) {
    payments.save(o);
  }
}
```

## Case: a call to a method that is not transactional, and an overload with another parameter count

covers: detect:target-transactional

```java path=src/main/java/C.java
@Service
class OrderService {
  public void process(Order o) {
    plain(o);
    save(o, 1);
  }
  void plain(Order o) { }
  @Transactional
  public void save(Order o) { }
  public void save(Order o, int n) { }
}
```

## Case: AspectJ weaving needs no proxy (Do not flag: AspectJ)

covers: do-not-flag:aspectj,required:aspectj
config: transaction.mode=aspectj

```java path=src/main/java/C.java
@Service
class OrderService {
  @Transactional
  private void save(Order o) { }
  @Transactional
  public final void other(Order o) { }
  public void process(Order o) {
    save(o);
  }
}
```

## Case: a private method with several annotations

covers: detect:private-annotations

```java path=src/main/java/C.java
class OrderService {
  @Deprecated
  @Transactional
  private void save(Order o) {  // FLAG
  }
  @Deprecated
  private void other(Order o) {
  }
}
```

## Case: a constructor calling a transactional method of its own class

covers: detect:self-invocation

```java path=src/main/java/C.java
@Service
class OrderService {
  OrderService() {
    save(null);  // FLAG MEDIUM
  }
  @Transactional
  public void save(Order o) { }
}
```
