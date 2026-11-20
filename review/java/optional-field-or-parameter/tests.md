# Tests: java/optional-field-or-parameter

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: an Optional field in a DTO (Detect)

covers: example:flag,required:field

```java path=src/main/java/C.java
class OrderDto {
  private Optional<String> note;  // CANDIDATE
  private String id;
}
```

## Case: data classes of other kinds

covers: detect:data-class

```java path=src/main/java/C.java
@Entity
class Order {
  private Optional<String> note;  // CANDIDATE
}
@ConfigurationProperties("app")
class AppProperties {
  private Optional<String> name;  // CANDIDATE
}
class Report implements Serializable {
  private Optional<String> title;  // CANDIDATE
}
@Data
class Row {
  private Optional<String> value;  // CANDIDATE
}
```

## Case: an Optional field of an ordinary class, and a static one

covers: detect:data-class-only

```java path=src/main/java/C.java
class OrderService {
  private Optional<String> cached;
}
class OrderEvent {
  private static Optional<String> DEFAULT = Optional.empty();
}
```

## Case: an Optional parameter of a public method (Detect)

covers: detect:parameter,required:parameter

```java path=src/main/java/C.java
class OrderService {
  public void update(Optional<String> note, String id) {  // CANDIDATE
  }
}
```

## Case: an Optional return type and a local Optional (Do not flag: return type, private and local)

covers: do-not-flag:optional-return,do-not-flag:private-or-local,required:return,required:local

```java path=src/main/java/C.java
class OrderService {
  public Optional<String> find(String id) {
    Optional<String> found = Optional.empty();
    return found;
  }
  private void update(Optional<String> note) {
  }
  void packagePrivate(Optional<String> note) {
  }
}
```

## Case: a lambda parameter (not a method) and an interface method (public)

covers: detect:public-method

```java path=src/main/java/C.java
interface Lookup {
  String find(Optional<String> key);  // CANDIDATE
}
class A {
  void f(List<Optional<String>> all) {
    all.forEach((Optional<String> o) -> { });
  }
}
```

## Case: test sources

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class OrderDto {
  private Optional<String> note;
  public void update(Optional<String> note) { }
}
```
