# Tests: common/log-injection

These cases run the restricted-language rule `log-injection` (a taint rule). A `// FLAG` marker ends each line that must produce a finding.

## Case: direct request parameter in a concatenation (Examples: Flag)

covers: example:flag, required:direct

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    log.info("login " + request.getParameter("user"));  // FLAG
  }
}
```

## Case: sanitised with a helper (Examples: OK)

covers: example:ok, do-not-flag:sanitised, required:sanitised

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user");
    log.info("login {}", sanitize(user));
  }
}
```

## Case: via a local variable

covers: required:local-variable

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user");
    log.info("login {}", user);  // FLAG
  }
}
```

## Case: line breaks stripped with replaceAll

covers: do-not-flag:sanitised

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user").replaceAll("[\\r\\n]", "_");
    log.info("login {}", user);
  }
}
```

## Case: replaceAll that does not touch line breaks

covers: detect:sanitiser-must-neutralise

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user").replaceAll("x", "y");
    log.info("login {}", user);  // FLAG
  }
}
```

## Case: integer parsed value

covers: do-not-flag:validated, required:integer

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    int id = Integer.parseInt(request.getParameter("id"));
    log.info("id {}", id);
  }
}
```

## Case: UUID parsed value

covers: do-not-flag:validated

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    UUID id = UUID.fromString(request.getHeader("X-Id"));
    log.info("id {}", id);
  }
}
```

## Case: enum validated value

covers: do-not-flag:validated

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    Mode m = Mode.valueOf(request.getParameter("mode"));
    log.info("mode {}", m);
  }
}
```

## Case: JSON / structured logger

covers: do-not-flag:structured-logger, required:json-logger
config: logging.structured=true

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    log.info("login " + request.getParameter("user"));
  }
}
```

## Case: @RequestParam parameter logged directly

covers: detect:annotated-parameter

```java path=src/main/java/C.java
class C {
  @GetMapping("/u") void f(@RequestParam String user) {
    log.warn("user {}", user);  // FLAG
  }
}
```

## Case: @PathVariable with a placeholder

covers: detect:annotated-parameter

```java path=src/main/java/C.java
class C {
  @GetMapping("/u/{id}") void f(@PathVariable String id) {
    log.info("id {}", id);  // FLAG
  }
}
```

## Case: getter on a @RequestBody object

covers: detect:getter-chain

```java path=src/main/java/C.java
class C {
  @PostMapping("/u") void f(@RequestBody UserDto dto) {
    log.info("name {}", dto.getName());  // FLAG
  }
}
```

## Case: pass-through string methods keep the taint

covers: detect:pass-through

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user").trim().toLowerCase();
    log.info("login {}", user);  // FLAG
  }
}
```

## Case: String.format keeps the taint

covers: detect:pass-through

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String msg = String.format("user=%s", request.getParameter("user"));
    log.info(msg);  // FLAG
  }
}
```

## Case: constant and field values

covers: detect:non-request

```java path=src/main/java/C.java
class C {
  String name = "svc";
  void f() {
    log.info("started " + name);
    log.info("done");
  }
}
```

## Case: tainted value that is never logged

covers: detect:sink-only

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user");
    store(user);
  }
}
```

## Case: a non-logger receiver named audit

covers: detect:sink-is-logger

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    audit.info(request.getParameter("user"));
  }
}
```

## Case: sanitised after reassignment (strong update)

covers: do-not-flag:sanitised, detect:flow-sensitive

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user");
    user = sanitize(user);
    log.info("login {}", user);
  }
}
```

## Case: overwritten with a constant

covers: do-not-flag:sanitised, detect:flow-sensitive

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String user = request.getParameter("user");
    user = "anonymous";
    log.info("login {}", user);
  }
}
```

## Case: sanitised only on one branch

covers: detect:flow-sensitive

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request, boolean strict) {
    String user = request.getParameter("user");
    if (strict) {
      user = sanitize(user);
    }
    log.info("login {}", user);  // FLAG
  }
}
```

## Case: compound assignment accumulates taint

covers: detect:flow-sensitive

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    String msg = "login ";
    msg += request.getParameter("user");
    log.info(msg);  // FLAG
  }
}
```

## Case: interprocedural: a tainted argument reaches the helper's log call

covers: detect:interprocedural

```java path=src/main/java/C.java
class C {
  void handle(HttpServletRequest request) {
    String user = request.getParameter("user");
    audit(user);
  }
  void audit(String who) {
    log.info("audit " + who);  // FLAG
  }
}
```

## Case: interprocedural: the helper is only called with constants

covers: detect:interprocedural

```java path=src/main/java/C.java
class C {
  void handle() {
    audit("system");
  }
  void audit(String who) {
    log.info("audit " + who);
  }
}
```

## Case: interprocedural: one tainted and one constant caller

covers: detect:interprocedural

```java path=src/main/java/C.java
class C {
  void a(HttpServletRequest request) { audit(request.getParameter("u")); }
  void b() { audit("system"); }
  void audit(String who) {
    log.info("audit " + who);  // FLAG
  }
}
```

## Case: interprocedural: the helper sanitises before logging

covers: do-not-flag:sanitised, detect:interprocedural

```java path=src/main/java/C.java
class C {
  void a(HttpServletRequest request) { audit(request.getParameter("u")); }
  void audit(String who) {
    log.info("audit " + sanitize(who));
  }
}
```

## Case: interprocedural: a tainted value comes back from a helper

covers: detect:interprocedural

```java path=src/main/java/C.java
class C {
  void handle(HttpServletRequest request) {
    log.info("user " + userOf(request));  // FLAG
  }
  String userOf(HttpServletRequest r) {
    return r.getParameter("user");
  }
}
```

## Case: test source

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class C {
  void f(HttpServletRequest request) {
    log.info("login " + request.getParameter("user"));
  }
}
```

## Case: debug level

covers: detect:log-levels

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    log.debug("user " + request.getParameter("u"));  // FLAG
  }
}
```

## Case: trace level

covers: detect:log-levels

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    log.trace("user " + request.getParameter("u"));  // FLAG
  }
}
```

## Case: error level

covers: detect:log-levels

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    log.error("user " + request.getParameter("u"));  // FLAG
  }
}
```

## Case: a logger method that is not a log call

covers: detect:log-levels

```java path=src/main/java/C.java
class C {
  void f(HttpServletRequest request) {
    log.setPrefix(request.getParameter("u"));
  }
}
```
