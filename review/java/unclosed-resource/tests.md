# Tests: java/unclosed-resource

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a stream that is never closed (Examples: Flag)

covers: example:flag,required:unclosed

```java path=src/main/java/C.java
class C {
  byte[] f(File file) throws IOException {
    InputStream in = new FileInputStream(file);  // CANDIDATE
    return in.readAllBytes();
  }
}
```

## Case: try-with-resources (Examples: OK)

covers: example:ok,required:try-with-resources

```java path=src/main/java/C.java
class C {
  byte[] f(File file) throws IOException {
    try (InputStream in = new FileInputStream(file)) {
      return in.readAllBytes();
    }
  }
}
```

## Case: closed in a finally

covers: required:finally

```java path=src/main/java/C.java
class C {
  void f(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    try {
      in.read();
    } finally {
      in.close();
    }
  }
}
```

## Case: returned, stored in a field, or handed to another method (Do not flag: ownership)

covers: do-not-flag:ownership,required:returned

```java path=src/main/java/C.java
class C {
  private InputStream held;
  InputStream a(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    return in;
  }
  void b(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    this.held = in;
  }
  void c(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    consume(in);
  }
  BufferedReader d(File file) throws IOException {
    FileReader r = new FileReader(file);
    return new BufferedReader(r);
  }
}
```

## Case: the other resources

covers: detect:creations

```java path=src/main/java/C.java
class C {
  void f(Path p, DataSource ds) throws Exception {
    var a = Files.newBufferedReader(p);  // CANDIDATE
    var b = Files.lines(p);  // CANDIDATE
    var c = Files.walk(p);  // CANDIDATE
    var d = ds.getConnection();  // CANDIDATE
    var e = new Socket("host", 80);  // CANDIDATE
    var g = new FileWriter("x");  // CANDIDATE
  }
}
```

## Case: resources with a no-op close, and framework-managed ones (Do not flag)

covers: do-not-flag:no-op-close,do-not-flag:framework-managed

```java path=src/main/java/C.java
class C {
  void f(byte[] data) {
    var a = new ByteArrayInputStream(data);
    var b = new StringReader("x");
    var c = jdbcTemplate.queryForList("select 1");
    var d = restTemplate.getForEntity("http://x", String.class);
  }
}
```

## Case: not held in a local variable

covers: detect:local-only

```java path=src/main/java/C.java
class C {
  private final InputStream in = new FileInputStream("x");
  void f() throws Exception {
    new FileInputStream("y").read();
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f(File file) throws IOException {
    InputStream in = new FileInputStream(file);
  }
}
```
