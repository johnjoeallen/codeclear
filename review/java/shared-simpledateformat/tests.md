# Tests: java/shared-simpledateformat

A `// FLAG` marker ends each line that must produce a finding.

## Case: static field (Examples: Flag)

covers: example:flag,required:static

```java path=src/main/java/C.java
class C {
  private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd");  // FLAG
}
```

## Case: the java.time formatter is the fix (Examples: OK)

covers: example:ok

```java path=src/main/java/C.java
class C {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
}
```

## Case: instance field of a @Service (Detect)

covers: detect:singleton,required:service

```java path=src/main/java/C.java
@Service
class C {
  private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");  // FLAG
}
```

## Case: DateFormat in a @Component, @RestController and @Configuration

covers: detect:singleton

```java path=src/main/java/C.java
@Component
class A {
  DateFormat a = DateFormat.getDateInstance();  // FLAG
}
@RestController
class B {
  SimpleDateFormat b = new SimpleDateFormat("HH");  // FLAG
}
@Configuration
class D {
  SimpleDateFormat d = new SimpleDateFormat("mm");  // FLAG
}
```

## Case: instance field of a plain class

covers: detect:singleton-only

```java path=src/main/java/C.java
class C {
  private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
}
```

## Case: a fully qualified type

covers: detect:qualified

```java path=src/main/java/C.java
class C {
  static java.text.SimpleDateFormat FMT = new java.text.SimpleDateFormat("yyyy");  // FLAG
}
```

## Case: a general type initialised with a SimpleDateFormat

covers: detect:initializer

```java path=src/main/java/C.java
class C {
  static Format FMT = new SimpleDateFormat("yyyy");  // FLAG
}
```

## Case: local variable (Do not flag: local)

covers: do-not-flag:local-variable,required:local

```java path=src/main/java/C.java
class C {
  void f() {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    fmt.format(new Date());
  }
}
```

## Case: wrapped in ThreadLocal (Do not flag: ThreadLocal)

covers: do-not-flag:threadlocal,required:threadlocal

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<SimpleDateFormat> FMT =
      ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
}
```

## Case: anonymous ThreadLocal subclass

covers: do-not-flag:threadlocal

```java path=src/main/java/C.java
class C {
  static final ThreadLocal<SimpleDateFormat> FMT = new ThreadLocal<SimpleDateFormat>() {
    protected SimpleDateFormat initialValue() { return new SimpleDateFormat("yyyy"); }
  };
}
```

## Case: private final and only used inside synchronized code reports at LOW (Do not flag: synchronized)

covers: do-not-flag:synchronized-only

```java path=src/main/java/C.java
@Service
class C {
  private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy");  // FLAG LOW
  String f(Date d) {
    synchronized (fmt) {
      return fmt.format(d);
    }
  }
}
```

## Case: private final but also used outside synchronized code keeps the rule's confidence

covers: detect:synchronized-only

```java path=src/main/java/C.java
@Service
class C {
  private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy");  // FLAG
  String f(Date d) {
    synchronized (fmt) {
      return fmt.format(d);
    }
  }
  String g(Date d) {
    return fmt.format(d);
  }
}
```

## Case: a declared SimpleDateFormat field assigned later

covers: detect:declared-type

```java path=src/main/java/C.java
@Service
class C {
  private SimpleDateFormat fmt;  // FLAG
  C() { fmt = new SimpleDateFormat("yyyy"); }
}
```
