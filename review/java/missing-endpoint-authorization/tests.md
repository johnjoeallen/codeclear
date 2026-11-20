# Tests: java/missing-endpoint-authorization

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a delete mapping with no authorization (Examples: Flag)

covers: example:flag,required:no-annotation

```java path=src/main/java/C.java
@RestController
class C {
  @DeleteMapping("/users/{id}")  // CANDIDATE
  void delete(@PathVariable long id) { users.delete(id); }
}
```

## Case: method-level @PreAuthorize (Examples: OK)

covers: example:ok,required:method-level

```java path=src/main/java/C.java
@RestController
class C {
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/users/{id}")
  void delete(@PathVariable long id) { users.delete(id); }
}
```

## Case: class-level authorization (no candidate)

covers: required:class-level

```java path=src/main/java/C.java
@RestController
@PreAuthorize("isAuthenticated()")
class C {
  @GetMapping("/users")
  List<User> all() { return users.all(); }
}
```

## Case: public endpoints by design (Do not flag)

covers: do-not-flag:public-endpoint,required:health

```java path=src/main/java/C.java
@RestController
class C {
  @GetMapping("/health")
  String health() { return "ok"; }
  @PostMapping("/login")
  Token login(@RequestBody Creds c) { return auth.login(c); }
}
```

## Case: an explicit principal or role check in the body

covers: detect:body-check

```java path=src/main/java/C.java
@RestController
class C {
  @GetMapping("/me")
  User me(HttpServletRequest r) { return users.get(r.getUserPrincipal().getName()); }
  @GetMapping("/admin")
  String admin(HttpServletRequest r) { return r.isUserInRole("ADMIN") ? "x" : "no"; }
  @GetMapping("/mine")
  String mine(@AuthenticationPrincipal User u) { return u.name(); }
}
```

## Case: PermitAll and the other mapping kinds

covers: detect:mappings

```java path=src/main/java/C.java
@RestController
class C {
  @PermitAll
  @GetMapping("/open")
  String open() { return "x"; }
  @PostMapping("/a")  // CANDIDATE
  void a() { }
  @PutMapping("/b")  // CANDIDATE
  void b() { }
  @PatchMapping("/c")  // CANDIDATE
  void c() { }
  @RequestMapping("/d")  // CANDIDATE
  void d() { }
}
```

## Case: a class-level mapping and a mapping outside a controller

covers: detect:controller-only

```java path=src/main/java/C.java
@RequestMapping("/api")
class C {
  @GetMapping("/x")
  String x() { return "x"; }
}
```

## Case: a URL-based security configuration is left to the AI

covers: do-not-flag:url-security-config

```java path=src/main/java/C.java
@Controller
class C {
  @GetMapping("/reports")  // CANDIDATE
  String reports() { return "r"; }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
@RestController
class CTest {
  @GetMapping("/x")
  String x() { return "x"; }
}
```
