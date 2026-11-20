# Tests: java/unconsumed-lazy-stream

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: a stream that ends in an intermediate operation (Detect)

covers: example:flag,required:no-terminal

```java path=src/main/java/C.java
class C {
  void f(List<String> names) {
    names.stream().map(String::trim);  // FLAG
    names.stream().filter(n -> !n.isEmpty()).peek(this::log);  // FLAG
  }
}
```

## Case: other intermediate operations and stream sources

covers: detect:operations

```java path=src/main/java/C.java
class C {
  void f(List<String> names, int[] numbers) {
    names.parallelStream().sorted();  // FLAG
    names.stream().distinct();  // FLAG
    names.stream().limit(3);  // FLAG
    Stream.of(1, 2).flatMap(x -> Stream.of(x));  // FLAG
    Arrays.stream(numbers).boxed();  // FLAG
  }
}
```

## Case: a terminal operation consumes the stream (Do not flag: consumed)

covers: do-not-flag:stream-consumed,required:collect

```java path=src/main/java/C.java
class C {
  void f(List<String> names) {
    names.stream().map(String::trim).forEach(this::log);
    List<String> a = names.stream().map(String::trim).collect(Collectors.toList());
    long n = names.stream().filter(String::isEmpty).count();
  }
  Stream<String> g(List<String> names) {
    return names.stream().map(String::trim);
  }
  void h(List<String> names) {
    consume(names.stream().map(String::trim));
    Stream<String> kept = names.stream().filter(n -> true);
    kept.forEach(this::log);
  }
}
```

## Case: Optional and reactive chains (Do not flag: not a stream)

covers: do-not-flag:non-stream-chain,required:optional

```java path=src/main/java/C.java
class C {
  void f(Optional<String> name, Flux<String> flux, Mono<String> mono) {
    name.map(String::trim);
    flux.map(String::trim);
    mono.filter(s -> true);
  }
}
```

## Case: a map or peek with a side effect before a short-circuiting terminal operation

covers: detect:side-effect,required:peek-count

```java path=src/main/java/C.java
class C {
  void f(List<String> names, List<String> seen, Repo repo) {
    long a = names.stream().peek(seen::add).count();  // FLAG
    names.stream().map(n -> { seen.add(n); return n; }).findFirst();  // FLAG
    names.stream().map(n -> repo.save(n)).anyMatch(Objects::nonNull);  // FLAG
    names.stream().peek(n -> counter = counter + 1).findAny();  // FLAG
    names.stream().peek(n -> events.send(n)).allMatch(x -> true);  // FLAG
    names.stream().peek(n -> items.put(n, n)).noneMatch(x -> false);  // FLAG
  }
}
```

## Case: a peek that only logs, or a side effect before forEach (Do not flag: logging peek)

covers: do-not-flag:logging-peek,required:logging-peek

```java path=src/main/java/C.java
class C {
  void f(List<String> names, List<String> seen) {
    long a = names.stream().peek(n -> log.info("saw {}", n)).count();
    names.stream().map(n -> n.trim()).findFirst();
    names.stream().peek(seen::add).forEach(this::log);
    names.stream().map(n -> { List<String> local = new ArrayList<>(); local.add(n); return local; }).count();
  }
}
```

## Case: a Stream local that is never used

covers: detect:unused-local

```java path=src/main/java/C.java
class C {
  void f(List<String> names) {
    Stream<String> unused = names.stream().map(String::trim);  // FLAG
    IntStream numbers = IntStream.range(0, 3);  // FLAG
    Stream<String> used = names.stream();
    used.forEach(this::log);
    List<String> notAStream = new ArrayList<>();
  }
}
```

## Case: test sources

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(List<String> names) {
    names.stream().map(String::trim);
  }
}
```

## Case: a non-stream chain with a side effect before count (Do not flag: not a stream)

covers: do-not-flag:non-stream-chain

```java path=src/main/java/C.java
class C {
  void f(Flux<String> flux, List<String> seen) {
    flux.map(n -> { seen.add(n); return n; }).count();
  }
}
```

## Case: every stream type for locals

covers: detect:stream-types

```java path=src/main/java/C.java
class C {
  void f() {
    LongStream a = LongStream.range(0, 3);  // FLAG
    DoubleStream b = DoubleStream.of(1.0);  // FLAG
  }
}
```

## Case: test sources for the terminal and local forms

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(List<String> names, List<String> seen) {
    Stream<String> unused = names.stream();
    names.stream().peek(seen::add).count();
  }
}
```

## Case: the body of an expression lambda is its value, not a discarded statement (Do not flag: consumed)

covers: do-not-flag:stream-consumed

```java path=src/main/java/C.java
class C {
  void f(List<Window> windows) {
    windows.stream().flatMap(w -> w.states().stream().filter(e -> e.count() > 0).map(e -> toRow(w, e))).forEach(this::use);
    Function<List<String>, Stream<String>> g = l -> l.stream().map(String::trim);
  }
}
```

## Case: a stream used only as the scope of a method reference (Do not flag: consumed)

covers: do-not-flag:stream-consumed

```java path=src/main/java/C.java
class C {
  void f(Path root) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      for (Path p : (Iterable<Path>) walk::iterator) {
        use(p);
      }
    }
  }
}
```
