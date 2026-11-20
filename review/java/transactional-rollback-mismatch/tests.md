# Tests: java/transactional-rollback-mismatch

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a checked exception and no rollbackFor (Examples: Flag)

covers: example:flag,required:checked-throws

```java path=src/main/java/C.java
class C {
  @Transactional
  void run() throws IOException {  // CANDIDATE
    repo.save(a);
    export();
  }
}
```

## Case: rollbackFor (Examples: OK)

covers: example:ok,do-not-flag:rollback-for,required:rollback-for

```java path=src/main/java/C.java
class C {
  @Transactional(rollbackFor = IOException.class)
  void run() throws IOException {
    repo.save(a);
    export();
  }
  @Transactional(noRollbackFor = IOException.class)
  void other() throws IOException {
    export();
  }
}
```

## Case: a swallowed failure after a write

covers: required:swallowed-catch

```java path=src/main/java/C.java
class C {
  @Transactional
  void run() {  // CANDIDATE
    repo.save(a);
    try { notifyAll(a); } catch (Exception e) { log.warn("x", e); }
  }
}
```

## Case: setRollbackOnly in the catch (Do not flag)

covers: do-not-flag:set-rollback-only,required:set-rollback-only

```java path=src/main/java/C.java
class C {
  @Transactional
  void run() {
    repo.save(a);
    try { notifyAll(a); } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
  }
}
```

## Case: a catch that rethrows, and a catch with no write

covers: detect:rethrow

```java path=src/main/java/C.java
class C {
  @Transactional
  void a() {
    repo.save(x);
    try { b(); } catch (Exception e) { throw new IllegalStateException(e); }
  }
  @Transactional
  void c() {
    try { d(); } catch (Exception e) { log.warn("x", e); }
  }
}
```

## Case: wrapping into a checked exception

covers: detect:wrap-checked

```java path=src/main/java/C.java
class C {
  @Transactional
  void a() throws IOException {  // CANDIDATE
    try { b(); } catch (IllegalStateException e) { throw new IOException(e); }
  }
}
```

## Case: a method that is not transactional, and a transactional class

covers: detect:transactional-only

```java path=src/main/java/C.java
class C {
  void a() throws IOException { repo.save(x); }
}
@Transactional
class D {
  void b() throws IOException {  // CANDIDATE
    export();
  }
}
```

## Case: an intentional noRollbackFor documented in a comment (Do not flag)

covers: do-not-flag:documented-no-rollback

```java path=src/main/java/C.java
class C {
  // noRollbackFor: the audit write must survive a failed export
  @Transactional(noRollbackFor = IOException.class)
  void run() throws IOException {
    audit.save(a);
    export();
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  @Transactional
  void run() throws IOException { repo.save(a); }
}
```
