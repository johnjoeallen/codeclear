# Tests: java/mutable-shared-state

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a field of a singleton service assigned in a method (Examples: Flag)

covers: example:flag,required:mutated-field

```java path=src/main/java/C.java
@Service
class Cart {
  private int total;  // CANDIDATE
  void add(Item i) {
    total = total + i.price();
  }
}
```

## Case: a final injected dependency (Examples: OK)

covers: example:ok,do-not-flag:injected,required:final-dependency

```java path=src/main/java/C.java
@Service
class Cart {
  private final ItemRepository repo;
  private final List<Item> allowed = new ArrayList<>();
  Cart(ItemRepository repo) { this.repo = repo; }
  Item find(long id) { return repo.findById(id); }
}
```

## Case: a non-final static field that is reassigned, and one that is only set when declared

covers: detect:static-assigned

```java path=src/main/java/C.java
class C {
  private static int counter;  // CANDIDATE
  private static String name = "x";
  static void next() { counter++; }
}
```

## Case: static mutable collections that are modified in a method

covers: detect:static-collection

```java path=src/main/java/C.java
class C {
  private static final Map<String, String> CACHE = new HashMap<>();  // CANDIDATE
  private static final List<String> NAMES = new ArrayList<>();
  private static List<String> seen = new ArrayList<>();  // CANDIDATE
  static void put(String k, String v) { CACHE.put(k, v); }
  static void remember(String s) { seen.add(s); }
  static List<String> names() { return NAMES; }
}
```

## Case: assignment this.field in a controller

covers: detect:this-assignment

```java path=src/main/java/C.java
@RestController
class C {
  private String lastUser;  // CANDIDATE
  @GetMapping("/x")
  String get(String user) {
    this.lastUser = user;
    return user;
  }
}
```

## Case: state set in a constructor or @PostConstruct only (no candidate)

covers: detect:init-only

```java path=src/main/java/C.java
@Component
class C {
  private String config;
  private Map<String, String> table;
  C() { config = "x"; }
  @PostConstruct
  void init() { table = new HashMap<>(); table.put("a", "b"); }
}
```

## Case: thread-safe types and volatile (Do not flag)

covers: do-not-flag:thread-safe

```java path=src/main/java/C.java
@Service
class C {
  private final AtomicInteger hits = new AtomicInteger();
  private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();
  private volatile boolean ready;
  private static AtomicLong total = new AtomicLong();
  void f() { hits.incrementAndGet(); CACHE.put("a", "b"); ready = true; total = new AtomicLong(); }
}
```

## Case: prototype, request and session scoped beans (Do not flag: scope)

covers: do-not-flag:scope

```java path=src/main/java/C.java
@Service
@Scope("prototype")
class A {
  private int total;
  void add(int n) { total += n; }
}
```

```java path=src/main/java/B.java
@Component
@RequestScope
class B {
  private int total;
  void add(int n) { total = n; }
}
```

## Case: synchronized writes

covers: detect:synchronized

```java path=src/main/java/C.java
@Service
class C {
  private int total;
  synchronized void add(int n) { total = n; }
}
```

## Case: a class that is not a singleton bean

covers: detect:plain-class

```java path=src/main/java/C.java
class C {
  private int total;
  void add(int n) { total = n; }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
@Service
class CTest {
  private int total;
  void add(int n) { total = n; }
}
```
