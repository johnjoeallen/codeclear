# Tests: common/sensitive-data-in-log

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a password in a log call (Examples: Flag)

covers: example:flag,required:password

```java path=src/main/java/C.java
class C {
  void f(String user, String password) {
    log.info("login {} {}", user, password);  // CANDIDATE
  }
}
```

## Case: only the word in a message, and a masked value (Examples: OK)

covers: example:ok,do-not-flag:word-in-text,do-not-flag:masked,required:text-only

```java path=src/main/java/C.java
class C {
  void f(String user, String password) {
    log.info("password reset requested for {}", user);
    log.info("token refreshed");
    log.info("login {} {}", user, mask(password));
    log.info("login {}", redact(password));
  }
}
```

## Case: other secret names, getters, concatenation and console output

covers: detect:names

```java path=src/main/java/C.java
class C {
  void f(User u, String apiKey, String authorization) {
    log.debug("key " + apiKey);  // CANDIDATE
    logger.warn("auth {}", authorization);  // CANDIDATE
    log.info("user {}", u.getPassword());  // CANDIDATE
    System.out.println("token=" + u.getToken());  // CANDIDATE
    log.error("card {}", cardNumber);  // CANDIDATE
  }
}
```

## Case: a whole request, headers or user

covers: detect:whole-object,required:request

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request, Map<String, String> headers, User user) {
    log.info("got {}", request);  // CANDIDATE
    log.info("headers {}", headers);  // CANDIDATE
    log.info("user {}", user);  // CANDIDATE
    log.info("method {}", request.getMethod());
  }
}
```

## Case: ordinary values and calls that are not logging

covers: detect:ordinary

```java path=src/main/java/C.java
class C {
  void f(String id, int count) {
    log.info("processed {} items for {}", count, id);
    list.add(password);
  }
}
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```java path=src/test/java/CTest.java
class CTest {
  void f(String password) { log.info("login {}", password); }
}
```
