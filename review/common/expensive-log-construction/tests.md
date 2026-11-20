# Tests: common/expensive-log-construction

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: serialising an object in a debug message (Examples: Flag)

covers: example:flag,required:eager-serialisation

```java path=src/main/java/C.java
class C {
  void f(Order order) {
    log.debug("payload " + mapper.writeValueAsString(order));  // CANDIDATE
  }
}
```

## Case: guarded by a level check (Examples: OK)

covers: example:ok,do-not-flag:guarded,required:guarded

```java path=src/main/java/C.java
class C {
  void f(Order order) {
    if (log.isDebugEnabled()) {
      log.debug("payload {}", mapper.writeValueAsString(order));
    }
  }
}
```

## Case: a supplier lambda and a method reference

covers: do-not-flag:lazy,required:supplier

```java path=src/main/java/C.java
class C {
  void f(Order order) {
    logger.debug(() -> "payload " + mapper.writeValueAsString(order));
    logger.trace(order::summary);
  }
}
```

## Case: plain variables and trivial getters (Do not flag)

covers: do-not-flag:trivial-arguments,required:plain-getter

```java path=src/main/java/C.java
class C {
  void f(Order order, long id) {
    log.debug("order {} for {}", id, order.getCustomerId());
    log.debug("state " + order.isOpen() + " " + id);
  }
}
```

## Case: error level and info level (Do not flag: normally enabled)

covers: do-not-flag:error-level,required:error-level

```java path=src/main/java/C.java
class C {
  void f(Order order) {
    log.error("payload " + mapper.writeValueAsString(order));
    log.warn("payload {}", String.format("%s", order));
    log.info("payload {}", toJson(order));
  }
}
```

## Case: other eager forms and trace level

covers: detect:forms

```java path=src/main/java/C.java
class C {
  void f(Order order) {
    log.trace("order {}", String.format("%s-%s", order.getId(), order.getName()));  // CANDIDATE
    log.debug("order {}", toJson(order));  // CANDIDATE
    LOGGER.debug("items " + order.items().size());  // CANDIDATE
  }
}
```

## Case: a guard around a different level of the else branch is not a guard

covers: detect:guard-scope

```java path=src/main/java/C.java
class C {
  void f(Order order) {
    if (log.isDebugEnabled()) {
      prepare();
    } else {
      log.debug("payload {}", toJson(order));  // CANDIDATE
    }
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(Order order) {
    log.debug("payload " + toJson(order));
  }
}
```
