# Tests: java/shared-unsafe-collection

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a HashMap mutated in a @Component (Examples: Flag)

covers: example:flag,required:hashmap

```java path=src/main/java/C.java
@Component
class C {
  private final Map<String, Session> sessions = new HashMap<>();  // CANDIDATE
  void open(String id) {
    sessions.put(id, new Session());
  }
}
```

## Case: a ConcurrentHashMap (Examples: OK)

covers: example:ok,do-not-flag:thread-safe,required:concurrent

```java path=src/main/java/C.java
@Component
class C {
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final List<String> log = new CopyOnWriteArrayList<>();
  private final Map<String, String> wrapped = Collections.synchronizedMap(new HashMap<>());
  void open(String id) {
    sessions.put(id, new Session());
    log.add(id);
    wrapped.put(id, id);
  }
}
```

## Case: the other collection types, in a @Service and in a static field

covers: detect:types

```java path=src/main/java/C.java
@Service
class C {
  private final Set<String> seen = new HashSet<>();  // CANDIDATE
  private final List<String> items = new ArrayList<>();  // CANDIDATE
  private final Deque<String> queue = new ArrayDeque<>();  // CANDIDATE
  private final Map<String, String> ordered = new LinkedHashMap<>();  // CANDIDATE
  private final Map<String, String> sorted = new TreeMap<>();  // CANDIDATE
  private final List<String> linked = new LinkedList<>();  // CANDIDATE
  void f(String x) {
    seen.add(x);
    items.add(x);
    queue.push(x);
    ordered.put(x, x);
    sorted.remove(x);
    linked.clear();
  }
}
class Util {
  private static final Map<String, String> CACHE = new HashMap<>();  // CANDIDATE
  static void put(String k) {
    CACHE.put(k, k);
  }
}
```

## Case: declared with the concrete type, and changed through this.

covers: detect:types

```java path=src/main/java/C.java
@RestController
class C {
  private HashMap<String, String> m = new HashMap<>();  // CANDIDATE
  void f(String k) {
    this.m.put(k, k);
  }
}
```

## Case: filled in the constructor or @PostConstruct and never changed afterwards (Do not flag)

covers: do-not-flag:init-only,required:post-construct

```java path=src/main/java/C.java
@Component
class C {
  private final Map<String, String> byId = new HashMap<>();
  private final List<String> names = new ArrayList<>();
  C() {
    byId.put("a", "b");
  }
  @PostConstruct
  void init() {
    names.add("x");
  }
  String get(String id) {
    return byId.get(id);
  }
}
```

## Case: every change is inside synchronized code (Do not flag)

covers: do-not-flag:synchronized,required:synchronized

```java path=src/main/java/C.java
@Component
class C {
  private final Map<String, String> a = new HashMap<>();
  private final List<String> b = new ArrayList<>();
  synchronized void put(String k) {
    a.put(k, k);
  }
  void add(String x) {
    synchronized (this) {
      b.add(x);
    }
  }
}
```

## Case: a collection in a class that is neither static nor a bean

covers: detect:singleton

```java path=src/main/java/C.java
class C {
  private final List<String> items = new ArrayList<>();
  void add(String x) {
    items.add(x);
  }
}
```

## Case: a method local, a read-only use, and test sources

covers: detect:mutation

```java path=src/main/java/C.java
@Component
class C {
  private final List<String> items = new ArrayList<>();
  int size() {
    List<String> local = new ArrayList<>();
    local.add("x");
    return items.size() + local.size();
  }
}
```

```java path=src/test/java/CTest.java
@Component
class CTest {
  private final List<String> items = new ArrayList<>();
  void add(String x) {
    items.add(x);
  }
}
```
