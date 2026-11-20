# Tests: java/optional-get-without-check

Generated for api-version 1. A `// FLAG` marker ends each line that must produce a finding.

## Prelude

```java
import java.util.*; import java.util.concurrent.*; import java.util.concurrent.atomic.*; import java.util.function.*; class User {} interface UserRepo { Optional<User> findById(long id); }
```

## Case: chained findById().get() (Examples: Flag)

covers: example:flag, required:chained-get

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = repo.findById(id).get();  // FLAG
  }
}
```

## Case: orElseThrow (Examples: OK)

covers: example:ok, required:orElseThrow

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = repo.findById(id).orElseThrow(() -> new IllegalStateException());
  }
}
```

## Case: get on parameter with no guard

covers: detect:get

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = o.get();  // FLAG
  }
}
```

## Case: guarded: if (isPresent) then-branch

covers: do-not-flag:isPresent, required:isPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isPresent()) {
      User u = o.get();
    }
  }
}
```

## Case: guarded: isEmpty then return

covers: do-not-flag:isEmpty-exit

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isEmpty()) return;
    User u = o.get();
  }
}
```

## Case: guarded: isEmpty then throw

covers: do-not-flag:isEmpty-exit

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isEmpty()) { throw new IllegalStateException(); }
    User u = o.get();
  }
}
```

## Case: guarded: !isPresent then throw

covers: do-not-flag:isEmpty-exit

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (!o.isPresent()) throw new IllegalStateException();
    User u = o.get();
  }
}
```

## Case: guarded: else branch of isEmpty

covers: do-not-flag:isEmpty-exit

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isEmpty()) { n = 1; } else { User u = o.get(); }
  }
}
```

## Case: guarded: ternary

covers: do-not-flag:isPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = o.isPresent() ? o.get() : null;
  }
}
```

## Case: guarded: && short circuit

covers: do-not-flag:isPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    boolean b = o.isPresent() && o.get() != null;
  }
}
```

## Case: guarded: || short circuit with isEmpty

covers: do-not-flag:isEmpty-exit

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    boolean b = o.isEmpty() || o.get() != null;
  }
}
```

## Case: guarded: inside ifPresent lambda

covers: do-not-flag:ifPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    o.ifPresent(x -> System.out.println(o.get()));
  }
}
```

## Case: guarded: assert isPresent

covers: do-not-flag:isPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    assert o.isPresent();
    User u = o.get();
  }
}
```

## Case: guarded: continue in loop

covers: do-not-flag:isEmpty-exit

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    for (int i = 0; i < 3; i++) {
      if (o.isEmpty()) continue;
      User u = o.get();
    }
  }
}
```

## Case: guarded: conjunction of two conditions

covers: do-not-flag:isPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (n > 0 && o.isPresent()) { User u = o.get(); }
  }
}
```

## Case: get before the guard

covers: detect:earlier-guard

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = o.get();  // FLAG
    if (o.isPresent()) { n = 1; }
  }
}
```

## Case: guard on a different variable

covers: detect:same-variable

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    Optional<User> p = Optional.empty();
    if (p.isPresent()) { User u = o.get(); }  // FLAG
  }
}
```

## Case: isPresent checked but get is outside the guarded branch

covers: detect:dominating

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isPresent()) { n = 1; }
    User u = o.get();  // FLAG
  }
}
```

## Case: isEmpty without early exit

covers: detect:dominating

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isEmpty()) { n = 1; }
    User u = o.get();  // FLAG
  }
}
```

## Case: get in the wrong branch of isPresent

covers: detect:dominating

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (o.isPresent()) { n = 1; } else { User u = o.get(); }  // FLAG
  }
}
```

## Case: OptionalInt.getAsInt unguarded

covers: detect:optional-primitives

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    int x = oi.getAsInt();  // FLAG
  }
}
```

## Case: OptionalInt.getAsInt guarded

covers: do-not-flag:isPresent

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    if (oi.isPresent()) { int x = oi.getAsInt(); }
  }
}
```

## Case: Future.get is not an Optional

covers: do-not-flag:Future, required:Future

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    Future<User> fut = null;
    User u = fut.get();
  }
}
```

## Case: Supplier.get is not an Optional

covers: do-not-flag:Supplier

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    Supplier<User> sup = null;
    User u = sup.get();
  }
}
```

## Case: AtomicReference.get is not an Optional

covers: do-not-flag:AtomicReference

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    AtomicReference<User> ref = new AtomicReference<>();
    User u = ref.get();
  }
}
```

## Case: Map.get is not an Optional

covers: do-not-flag:non-optional

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    Map<String, User> m = new HashMap<>();
    User u = m.get("k");
  }
}
```

## Case: test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/STest.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = repo.findById(id).get();
  }
}
```

## Case: unresolved repository type: heuristic, LOW confidence

covers: detect:fallback-heuristic

```java prelude
import com.acme.UserRepo;
```

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    User u = repo.findById(id).get();  // FLAG LOW
  }
}
```

## Case: unresolved receiver, no Optional signal: no finding

covers: detect:fallback-heuristic

```java prelude
import com.acme.UserRepo;
```

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    Object x = thing.get();
  }
}
```

## Case: unresolved declared Optional variable: heuristic, LOW

covers: detect:fallback-heuristic

```java prelude
import com.acme.UserRepo;
```

```java path=src/main/java/S.java
class S {
  UserRepo repo; Thing thing; int n;
  void f(long id, Optional<User> o, OptionalInt oi) throws Exception {
    Optional<Missing> m = lookup();
    Object x = m.get();  // FLAG LOW
  }
}
```

## Case: OptionalLong.getAsLong unguarded

covers: detect:optional-primitives

```java path=src/main/java/S.java
class S {
  void f(OptionalLong ol) {
    long x = ol.getAsLong();  // FLAG
  }
}
```

## Case: OptionalDouble.getAsDouble unguarded

covers: detect:optional-primitives

```java path=src/main/java/S.java
class S {
  void f(OptionalDouble od) {
    double x = od.getAsDouble();  // FLAG
  }
}
```

## Case: resolved Optional from a method not on the producer-name list

covers: detect:get

```java path=src/main/java/S.java
class S {
  R r;
  void f(long id) {
    User u = r.lookup(id).get();  // FLAG
  }
}
interface R { Optional<User> lookup(long id); }
```

## Mutations

- `chained findById().get() (Examples: Flag)` -> `orElseThrow (Examples: OK)`
- `get on parameter with no guard` -> `guarded: if (isPresent) then-branch`
- `get on parameter with no guard` -> `guarded: isEmpty then return`
