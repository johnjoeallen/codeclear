# Tests: java/check-then-act-on-shared-state

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: containsKey then put on a field map (Examples: Flag)

covers: example:flag,required:contains-then-put

```java path=src/main/java/C.java
class C {
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  void open(String id) {
    if (!sessions.containsKey(id)) {  // CANDIDATE
      sessions.put(id, new Session());
    }
  }
}
```

## Case: putIfAbsent and computeIfAbsent (Examples: OK)

covers: example:ok,do-not-flag:atomic-methods,required:put-if-absent

```java path=src/main/java/C.java
class C {
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  void open(String id) {
    sessions.putIfAbsent(id, new Session());
    sessions.computeIfAbsent(id, k -> new Session());
  }
}
```

## Case: get == null then put, lazy init, and an atomic

covers: detect:shapes

```java path=src/main/java/C.java
class C {
  private Map<String, String> cache = new HashMap<>();
  private Service service;
  private final AtomicInteger count = new AtomicInteger();
  void a(String k) {
    if (cache.get(k) == null) cache.put(k, load(k));  // CANDIDATE
  }
  Service b() {
    if (service == null) service = new Service();  // CANDIDATE
    return service;
  }
  void c(int max) {
    if (count.get() < max) count.incrementAndGet();  // CANDIDATE
  }
}
```

## Case: inside synchronized (Do not flag)

covers: do-not-flag:synchronized,required:synchronized

```java path=src/main/java/C.java
class C {
  private final Map<String, String> cache = new HashMap<>();
  private Service service;
  synchronized void a(String k) {
    if (!cache.containsKey(k)) cache.put(k, load(k));
  }
  Service b() {
    synchronized (this) {
      if (service == null) service = new Service();
    }
    return service;
  }
}
```

## Case: local and parameter maps (Do not flag)

covers: do-not-flag:local

```java path=src/main/java/C.java
class C {
  void a(Map<String, String> m, String k) {
    if (!m.containsKey(k)) m.put(k, "v");
    Map<String, String> local = new HashMap<>();
    if (!local.containsKey(k)) local.put(k, "v");
  }
}
```

## Case: a volatile field

covers: detect:volatile

```java path=src/main/java/C.java
class C {
  private volatile Service service;
  Service b() {
    if (service == null) service = new Service();
    return service;
  }
}
```
