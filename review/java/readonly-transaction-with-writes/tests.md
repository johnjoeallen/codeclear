# Tests: java/readonly-transaction-with-writes

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: save in a readOnly method (Examples: Flag)

covers: example:flag,required:save

```java path=src/main/java/C.java
class C {
  private final OrderRepository repo;
  @Transactional(readOnly = true)
  Order get(long id) {
    Order o = repo.findById(id).orElseThrow();
    o.setLastRead(now());
    return repo.save(o);  // CANDIDATE
  }
}
```

## Case: a read-only query (Required: none)

covers: required:read-only

```java path=src/main/java/C.java
class C {
  private final OrderRepository repo;
  @Transactional(readOnly = true)
  Order get(long id) {
    return repo.findById(id).orElseThrow();
  }
}
```

## Case: other write methods on repositories, the entity manager and JDBC

covers: detect:write-calls

```java path=src/main/java/C.java
class C {
  private final OrderRepository repo;
  private final EntityManager em;
  private final JdbcTemplate jdbc;
  @Transactional(readOnly = true)
  void f(Order o) {
    repo.saveAll(List.of(o));  // CANDIDATE
    repo.deleteById(1L);  // CANDIDATE
    em.persist(o);  // CANDIDATE
    em.merge(o);  // CANDIDATE
    em.flush();  // CANDIDATE
    jdbc.update("update t set x = 1");  // CANDIDATE
    jdbc.batchUpdate("insert into t values (?)", rows);  // CANDIDATE
  }
}
```

## Case: a readOnly class, with a method that overrides it (Do not flag)

covers: detect:class-level,do-not-flag:override,required:override

```java path=src/main/java/C.java
@Transactional(readOnly = true)
class C {
  private final OrderRepository repo;
  void inherited(Order o) {
    repo.save(o);  // CANDIDATE
  }
  @Transactional
  void write(Order o) {
    repo.save(o);
  }
  @Transactional(readOnly = false)
  void explicit(Order o) {
    repo.save(o);
  }
}
```

## Case: a method outside any transaction, and a plain @Transactional

covers: detect:readonly-only

```java path=src/main/java/C.java
class C {
  private final OrderRepository repo;
  void none(Order o) {
    repo.save(o);
  }
  @Transactional
  void writable(Order o) {
    repo.save(o);
  }
}
```

## Case: writes to a cache or a metrics registry (Do not flag)

covers: do-not-flag:non-transactional-resources

```java path=src/main/java/C.java
class C {
  private final LocalCache cache;
  private final MetricRegistry metrics;
  @Transactional(readOnly = true)
  void f(Order o) {
    cache.save(o);
    metrics.flush();
    cache.delete(o);
  }
}
```

## Case: test sources (Do not flag)

covers: do-not-flag:tests

```java path=src/test/java/CTest.java
class CTest {
  private final OrderRepository repo;
  @Transactional(readOnly = true)
  void t(Order o) {
    repo.save(o);
  }
}
```

## Case: every write method name, and calls that are not writes

covers: detect:write-calls

```java path=src/main/java/C.java
class C {
  private final OrderRepository repo;
  private final JdbcTemplate jdbc;
  private final Notifier notifier;
  @Transactional(readOnly = true)
  void f(Order o) {
    repo.saveAndFlush(o);  // CANDIDATE
    repo.delete(o);  // CANDIDATE
    repo.deleteAll();  // CANDIDATE
    repo.deleteAllById(ids);  // CANDIDATE
    repo.deleteInBatch(list);  // CANDIDATE
    repo.remove(o);  // CANDIDATE
    jdbc.query("select 1", mapper);
    notifier.update(o);
    notifier.batchUpdate(o);
    repo.count();
  }
}
```
