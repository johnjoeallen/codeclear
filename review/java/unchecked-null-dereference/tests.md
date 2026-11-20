# Tests: java/unchecked-null-dereference

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a method called on Map.get (Examples: Flag)

covers: example:flag,required:map-get

```java path=src/main/java/C.java
class C {
  private final Map<String, Account> accounts = new HashMap<>();
  String name(String id) {
    return accounts.get(id).getName();  // CANDIDATE
  }
}
```

## Case: getOrDefault and a null check (Examples: OK)

covers: example:ok,do-not-flag:checked,required:checked

```java path=src/main/java/C.java
class C {
  private final Map<String, Account> accounts = new HashMap<>();
  String a(String id) {
    return accounts.getOrDefault(id, EMPTY).getName();
  }
  String b(String id) {
    if (!accounts.containsKey(id)) { return ""; }
    return accounts.get(id).getName();
  }
  String c(String id) {
    return Objects.requireNonNull(accounts.get(id)).getName();
  }
}
```

## Case: a JsonNode

covers: detect:json-node

```java path=src/main/java/C.java
class C {
  String f(JsonNode node) {
    return node.get("name").asText();  // CANDIDATE
  }
}
```

## Case: a repository method that is not Optional

covers: detect:repository

```java path=src/main/java/C.java
class C {
  private final UserRepository users;
  String f(String email) {
    return users.findByEmail(email).getName();  // CANDIDATE
  }
  String g(long id) {
    return users.findById(id).orElseThrow().getName();
  }
}
interface UserRepository {
  User findByEmail(String email);
  Optional<User> findById(long id);
}
```

## Case: a repository method that returns Optional or is NonNull (Do not flag)

covers: do-not-flag:non-null-return,do-not-flag:optional-return,required:optional

```java path=src/main/java/C.java
class C {
  private final UserRepository users;
  String f(String email) {
    return users.findByEmail(email).get().getName();
  }
  String g(String name) {
    return users.findByName(name).getName();
  }
}
interface UserRepository {
  Optional<User> findByEmail(String email);
  @NonNull User findByName(String name);
}
```

## Case: a method call on an ordinary call result, and a test source

covers: detect:other-calls

```java path=src/main/java/C.java
class C {
  String f(Service s) {
    return s.load().getName();
  }
}
```
