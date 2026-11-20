# Tests: java/collection-modified-during-iteration

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Prelude

```java
import java.util.*; import java.util.concurrent.*;
```

## Case: remove in a for-each (Detect)

covers: example:flag,required:remove-in-foreach

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    for (String s : items) {
      if (s.isEmpty()) {
        items.remove(s);  // FLAG
      }
    }
  }
}
```

## Case: add, clear, addAll and removeAll in a for-each

covers: detect:mutators

```java path=src/main/java/C.java
class C {
  void f(List<String> items, List<String> other) {
    for (String s : items) {
      items.add(s + "!");  // FLAG
      items.addAll(other);  // FLAG
      items.removeAll(other);  // FLAG
      items.clear();  // FLAG
    }
  }
}
```

## Case: put and remove on a map while walking a view of it

covers: detect:map-views

```java path=src/main/java/C.java
class C {
  void f(Map<String, Integer> m) {
    for (String k : m.keySet()) {
      m.put(k + "x", 1);  // FLAG
    }
    for (Integer v : m.values()) {
      m.remove("a");  // FLAG
    }
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      m.clear();  // FLAG
    }
  }
}
```

## Case: remove inside forEach

covers: detect:foreach-lambda,required:foreach

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    items.forEach(s -> items.remove(s));  // FLAG
    items.stream().forEach(s -> items.add(s));  // FLAG
  }
}
```

## Case: a field iterated through this

covers: detect:field

```java path=src/main/java/C.java
class C {
  private final List<String> items = new ArrayList<>();
  void f() {
    for (String s : this.items) {
      this.items.remove(s);  // FLAG
    }
    for (String s : items) {
      items.add(s);  // FLAG
    }
  }
}
```

## Case: an outer collection changed in an inner loop

covers: detect:nested

```java path=src/main/java/C.java
class C {
  void f(List<String> a, List<String> b) {
    for (String x : a) {
      for (String y : b) {
        a.remove(x);  // FLAG
      }
    }
  }
}
```

## Case: removeIf is the fix (Examples: OK)

covers: example:ok,required:remove-if

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    items.removeIf(String::isEmpty);
  }
}
```

## Case: an iterator removes safely (Do not flag: iterator)

covers: do-not-flag:iterator-remove,required:iterator

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    Iterator<String> it = items.iterator();
    while (it.hasNext()) {
      if (it.next().isEmpty()) {
        it.remove();
      }
    }
  }
}
```

## Case: iterating a copy (Do not flag: copy)

covers: do-not-flag:copy-or-concurrent,required:copy

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    for (String s : new ArrayList<>(items)) {
      items.remove(s);
    }
    for (String s : List.copyOf(items)) {
      items.add(s);
    }
    for (String s : items.toArray(new String[0])) {
      items.remove(s);
    }
  }
}
```

## Case: concurrent and copy-on-write collections (Do not flag: concurrent)

covers: do-not-flag:copy-or-concurrent

```java path=src/main/java/C.java
class C {
  void f(CopyOnWriteArrayList<String> a, ConcurrentHashMap<String, Integer> m, ConcurrentLinkedQueue<String> q) {
    for (String s : a) { a.remove(s); }
    for (String k : m.keySet()) { m.remove(k); }
    q.forEach(s -> q.remove(s));
  }
}
```

## Case: the mutation is followed by a break or return (Do not flag: exit)

covers: do-not-flag:exit-after-mutation,required:break

```java path=src/main/java/C.java
class C {
  boolean f(List<String> items, String wanted) {
    for (String s : items) {
      if (s.equals(wanted)) {
        items.remove(s);
        break;
      }
    }
    for (String s : items) {
      if (s.equals(wanted)) {
        items.remove(s);
        return true;
      }
    }
    return false;
  }
}
```

## Case: a mutation that is not the last statement before the exit

covers: detect:exit-immediately

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    for (String s : items) {
      items.remove(s);  // FLAG
      log(s);
      break;
    }
  }
}
```

## Case: a different collection is changed

covers: detect:same-collection

```java path=src/main/java/C.java
class C {
  void f(List<String> items, List<String> out) {
    for (String s : items) {
      out.add(s);
      out.remove(s);
    }
  }
}
```

## Case: an unqualified call and a call outside any loop

covers: detect:scope-needed

```java path=src/main/java/C.java
class C extends ArrayList<String> {
  void f(List<String> copy) {
    for (String s : copy) {
      remove(s);
    }
    copy.remove("x");
  }
}
```

## Case: reading the collection inside the loop is fine

covers: detect:mutators-only

```java path=src/main/java/C.java
class C {
  void f(List<String> items) {
    for (String s : items) {
      if (items.size() > 3 && items.contains(s) && items.get(0) != null) {
        log(items.isEmpty());
      }
    }
  }
}
```

## Case: an unqualified call inside a loop over a copy

covers: detect:scope-needed

```java path=src/main/java/C.java
class C extends ArrayList<String> {
  void f() {
    for (String s : new ArrayList<>(this)) {
      remove(s);
    }
  }
}
```

## Case: put for the key being visited only replaces a value (Do not flag: current key)

covers: do-not-flag:replace-current-key

```java path=src/main/java/C.java
class C {
  void f(Map<String, Integer> m) {
    for (String k : m.keySet()) {
      m.put(k, m.get(k) + 1);
    }
    for (Map.Entry<String, Integer> e : m.entrySet()) {
      m.put(e.getKey(), e.getValue() + 1);
    }
    m.forEach((k, v) -> m.put(k, v + 1));
    m.entrySet().forEach(e -> m.put(e.getKey(), 0));
  }
}
```

## Case: put for a different key does change the map

covers: detect:replace-current-key

```java path=src/main/java/C.java
class C {
  void f(Map<String, Integer> m) {
    for (String k : m.keySet()) {
      m.put(k + "x", 1);  // FLAG
    }
    m.forEach((k, v) -> m.put("other", v));  // FLAG
  }
}
```

## Case: a variable declared as Map but created as a concurrent map (Do not flag: concurrent)

covers: do-not-flag:created-as-concurrent

```java path=src/main/java/C.java
class C {
  private final Map<String, String> cache = new ConcurrentHashMap<>();
  private final List<String> listeners;
  C() {
    this.listeners = new CopyOnWriteArrayList<>();
  }
  void f() {
    for (String k : cache.keySet()) {
      cache.remove(k);
    }
    for (String l : listeners) {
      listeners.remove(l);
    }
  }
}
```

## Case: a variable created as an ordinary class is still flagged

covers: detect:created-as

```java path=src/main/java/C.java
class C {
  private final Map<String, String> cache = new HashMap<>();
  void f() {
    for (String k : cache.keySet()) {
      cache.remove(k);  // FLAG
    }
  }
}
```
