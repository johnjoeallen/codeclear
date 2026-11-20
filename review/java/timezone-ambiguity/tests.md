# Tests: java/timezone-ambiguity

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: ZoneId.systemDefault() and TimeZone.getDefault() (Detect)

covers: example:flag,required:system-default

```java path=src/main/java/C.java
class C {
  void f() {
    ZoneId zone = ZoneId.systemDefault();  // CANDIDATE
    TimeZone tz = TimeZone.getDefault();  // CANDIDATE
    ZonedDateTime t = now.atZone(ZoneId.systemDefault());  // CANDIDATE
  }
}
```

## Case: LocalDateTime.now() and LocalDate.now() without a zone or clock

covers: detect:now

```java path=src/main/java/C.java
class C {
  void f() {
    LocalDateTime a = LocalDateTime.now();  // CANDIDATE
    LocalDate b = LocalDate.now();  // CANDIDATE
  }
}
```

## Case: an explicit zone or clock (Do not flag: fixed zone)

covers: do-not-flag:fixed-zone,required:utc

```java path=src/main/java/C.java
class C {
  void f(Clock clock) {
    LocalDateTime a = LocalDateTime.now(ZoneOffset.UTC);
    LocalDateTime b = LocalDateTime.now(clock);
    ZonedDateTime c = now.atZone(ZoneOffset.UTC);
    Instant d = Instant.now();
    ZoneId e = ZoneId.of("Europe/Dublin");
  }
}
```

## Case: other classes with the same method names

covers: detect:receiver

```java path=src/main/java/C.java
class C {
  void f() {
    Object a = Clocks.now();
    Object b = Other.systemDefault();
    Object c = Settings.getDefault();
    Object d = Instant.now();
  }
}
```

## Case: a SimpleDateFormat without setTimeZone

covers: detect:simple-date-format

```java path=src/main/java/C.java
class C {
  String f(Date d) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");  // CANDIDATE
    return fmt.format(d);
  }
}
```

## Case: a SimpleDateFormat with setTimeZone, or created outside a method

covers: detect:set-time-zone

```java path=src/main/java/C.java
class C {
  String f(Date d) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt.format(d);
  }
}
class D {
  private static final SimpleDateFormat SHARED = new SimpleDateFormat("yyyy");  // CANDIDATE
}
```

## Case: a LocalDateTime in an event DTO (Detect)

covers: detect:local-date-time-field,required:local-date-time

```java path=src/main/java/C.java
class OrderEvent {
  private LocalDateTime occurredAt;  // CANDIDATE
  private LocalDate day;  // CANDIDATE
  private Instant at;
  private OffsetDateTime sent;
  private static LocalDate EPOCH = LocalDate.EPOCH;
}
```

## Case: a LocalDateTime in a class that is not stored or exchanged

covers: detect:data-class-only

```java path=src/main/java/C.java
class Scheduler {
  private LocalDateTime next;
}
```

## Case: test sources (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
@Data
class RowTest {
  private LocalDateTime when;
  void f() {
    ZoneId z = ZoneId.systemDefault();
    LocalDateTime n = LocalDateTime.now();
    Object o = new SimpleDateFormat("yyyy");
  }
}
```

## Case: a class other than SimpleDateFormat is not a date format

covers: detect:type-name

```java path=src/main/java/C.java
class C {
  void f() {
    Object o = new Other();
    Object p = new SimpleTimeZone(0, "x");
  }
}
```
