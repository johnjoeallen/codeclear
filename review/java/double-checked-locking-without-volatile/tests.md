# Tests: java/double-checked-locking-without-volatile

A `// FLAG` marker ends each line that must produce a finding.

## Case: non-volatile double-checked locking (Detect)

covers: example:flag,required:non-volatile

```java path=src/main/java/C.java
class Singleton {
  private static Singleton instance;
  static Singleton get() {
    if (instance == null) {  // FLAG
      synchronized (Singleton.class) {
        if (instance == null) {
          instance = new Singleton();
        }
      }
    }
    return instance;
  }
}
```

## Case: an instance field checked through this, null on the left

covers: detect:this-and-order

```java path=src/main/java/C.java
class Cache {
  private Map<String, String> data;
  Map<String, String> data() {
    if (null == this.data) {  // FLAG
      synchronized (this) {
        if (this.data == null) {
          this.data = new HashMap<>();
        }
      }
    }
    return data;
  }
}
```

## Case: a volatile field (Do not flag: volatile)

covers: do-not-flag:volatile,required:volatile

```java path=src/main/java/C.java
class Singleton {
  private static volatile Singleton instance;
  static Singleton get() {
    if (instance == null) {
      synchronized (Singleton.class) {
        if (instance == null) {
          instance = new Singleton();
        }
      }
    }
    return instance;
  }
}
```

## Case: a single check inside the synchronized block

covers: detect:two-checks,required:single-check

```java path=src/main/java/C.java
class Singleton {
  private static Singleton instance;
  static Singleton get() {
    synchronized (Singleton.class) {
      if (instance == null) {
        instance = new Singleton();
      }
    }
    return instance;
  }
}
```

## Case: a check outside only, no second check

covers: detect:two-checks

```java path=src/main/java/C.java
class Singleton {
  private static Singleton instance;
  static Singleton get() {
    if (instance == null) {
      synchronized (Singleton.class) {
        instance = new Singleton();
      }
    }
    return instance;
  }
}
```

## Case: the holder-class idiom (Do not flag: holder idiom)

covers: do-not-flag:holder-or-enum-or-final,required:holder

```java path=src/main/java/C.java
class Singleton {
  private static class Holder {
    static final Singleton INSTANCE = new Singleton();
  }
  static Singleton get() {
    return Holder.INSTANCE;
  }
}
```

## Case: an enum singleton

covers: do-not-flag:holder-or-enum-or-final

```java path=src/main/java/C.java
enum Registry {
  INSTANCE;
  void register(String s) { }
}
```

## Case: a primitive field (Do not flag: primitive)

covers: do-not-flag:primitive

```java path=src/main/java/C.java
class Counter {
  private int value;
  int value() {
    if (value == 0) {
      synchronized (this) {
        if (value == 0) {
          value = compute();
        }
      }
    }
    return value;
  }
}
```

## Case: a local variable is not a field

covers: detect:field-only

```java path=src/main/java/C.java
class C {
  Object f() {
    Object local = null;
    if (local == null) {
      synchronized (this) {
        if (local == null) {
          local = new Object();
        }
      }
    }
    return local;
  }
}
```

## Case: the two checks are on different fields

covers: detect:same-field

```java path=src/main/java/C.java
class C {
  private Object a;
  private Object b;
  Object f() {
    if (a == null) {
      synchronized (this) {
        if (b == null) {
          b = new Object();
        }
      }
    }
    return a;
  }
}
```
