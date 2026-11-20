# Tests: java/unbounded-findall

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: findAll() on a repository (Examples: Flag)

covers: example:flag,required:findall

```java path=src/main/java/C.java
class C {
  private final OrderRepository orderRepository;
  List<Order> all() {
    return orderRepository.findAll();  // CANDIDATE
  }
}
```

## Case: findAll with a pageable or a specification (Examples: OK)

covers: example:ok,do-not-flag:paged,required:findall-pageable

```java path=src/main/java/C.java
class C {
  private final OrderRepository orderRepository;
  Page<Order> page() {
    return orderRepository.findAll(PageRequest.of(0, 100));
  }
  Page<Order> filtered(Specification<Order> spec, Pageable pageable) {
    return orderRepository.findAll(spec, pageable);
  }
}
```

## Case: a method called findAll on something that is not a repository

covers: detect:repository-type

```java path=src/main/java/C.java
class C {
  private final OrderCache cache;
  List<Order> all() {
    return cache.findAll();
  }
}
```

## Case: repositories of reference tables

covers: do-not-flag:reference-table

```java path=src/main/java/C.java
class C {
  private final OrderTypeRepository types;
  private final StatusRepository statuses;
  private final TenantConfigRepository configs;
  void load() {
    types.findAll();
    statuses.findAll();
    configs.findAll();
  }
}
```

## Case: a repository held as a parameter or a local

covers: detect:repository-type

```java path=src/main/java/C.java
class C {
  List<Order> byParameter(OrderRepository repo) {
    return repo.findAll();  // CANDIDATE
  }
  List<Order> byLocal() {
    OrderRepository repo = lookup();
    return repo.findAll();  // CANDIDATE
  }
}
```

## Case: Files read methods on a path (Detect)

covers: detect:files,required:files-read

```java path=src/main/java/C.java
class C {
  byte[] bytes(Path p) throws IOException {
    return Files.readAllBytes(p);  // CANDIDATE
  }
  String text(Path p) throws IOException {
    return Files.readString(p);  // CANDIDATE
  }
  List<String> lines(Path p) throws IOException {
    return Files.readAllLines(p);  // CANDIDATE
  }
}
```

## Case: InputStream.readAllBytes

covers: detect:input-stream

```java path=src/main/java/C.java
class C {
  byte[] drain(InputStream in) throws IOException {
    return in.readAllBytes();  // CANDIDATE
  }
  byte[] other(Reader reader) throws IOException {
    return reader.readAllBytes();
  }
}
```

## Case: classpath resources and configuration files (Do not flag)

covers: do-not-flag:classpath,required:classpath

```java path=src/main/java/C.java
class C {
  String template() throws Exception {
    return Files.readString(Path.of(getClass().getResource("/t.txt").toURI()));
  }
  byte[] cfg() throws IOException {
    return Files.readAllBytes(Path.of("config/app.properties"));
  }
  Properties yaml() throws IOException {
    return Files.readString(Path.of("settings.yml"));
  }
}
```

## Case: test sources (Do not flag)

covers: do-not-flag:tests

```java path=src/test/java/CTest.java
class CTest {
  void t(OrderRepository repo, Path p) throws IOException {
    repo.findAll();
    Files.readString(p);
  }
}
```

## Case: the stream types that are matched, and a call that is not a read-all

covers: detect:input-stream

```java path=src/main/java/C.java
class C {
  byte[] a(FileInputStream in) throws IOException {
    return in.readAllBytes();  // CANDIDATE
  }
  byte[] b(BufferedInputStream in) throws IOException {
    return in.readAllBytes();  // CANDIDATE
  }
  int c(InputStream in) throws IOException {
    return in.read();
  }
  Object d(OrderRepository repo) {
    return repo.findById(1L);
  }
  Object e(Files files) {
    return files.size();
  }
}
```
