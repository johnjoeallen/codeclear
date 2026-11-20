# Tests: java/missing-request-validation

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a body without @Valid (Examples: Flag)

covers: example:flag,required:no-valid

```java path=src/main/java/C.java
@RestController
class C {
  @PostMapping("/orders")
  Order create(@RequestBody OrderRequest r) { return svc.create(r); }  // CANDIDATE
}
```

## Case: @Valid and @Validated (Examples: OK)

covers: example:ok,required:valid

```java path=src/main/java/C.java
@RestController
class C {
  @PostMapping("/orders")
  Order create(@Valid @RequestBody OrderRequest r) { return svc.create(r); }
  @PutMapping("/orders/{id}")
  Order update(@Validated @RequestBody OrderRequest r) { return svc.update(r); }
}
```

## Case: validated by hand (no candidate)

covers: do-not-flag:programmatic,required:programmatic

```java path=src/main/java/C.java
@RestController
class C {
  private final Validator validator;
  @PostMapping("/orders")
  Order create(@RequestBody OrderRequest r) {
    validator.validate(r);
    return svc.create(r);
  }
}
```

## Case: a read-only filter (Do not flag)

covers: do-not-flag:read-only-filter

```java path=src/main/java/C.java
@RestController
class C {
  @GetMapping("/orders")
  List<Order> search(@RequestBody SearchFilter f) { return svc.search(f); }
}
```

## Case: PUT, PATCH and RequestMapping writes

covers: detect:write-mappings

```java path=src/main/java/C.java
@RestController
class C {
  @PutMapping("/a")
  void a(@RequestBody A a) { }  // CANDIDATE
  @PatchMapping("/b")
  void b(@RequestBody B b) { }  // CANDIDATE
  @RequestMapping(value = "/c", method = RequestMethod.POST)
  void c(@RequestBody C c) { }  // CANDIDATE
}
```

## Case: other parameters and a class that is not a controller

covers: detect:request-body-only

```java path=src/main/java/C.java
@RestController
class C {
  @PostMapping("/a/{id}")
  void a(@PathVariable long id, @RequestParam String q) { }
}
class Plain {
  void b(@RequestBody B b) { }
}
```

## Case: a validation aspect shown in context is left to the AI

covers: do-not-flag:project-wide-validation

```java path=src/main/java/C.java
@RestController
class C {
  @PostMapping("/x")
  void x(@RequestBody X x) { }  // CANDIDATE
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
@RestController
class CTest {
  @PostMapping("/x")
  void x(@RequestBody X x) { }
}
```
