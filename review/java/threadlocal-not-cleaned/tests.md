# Tests: java/threadlocal-not-cleaned

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: set without remove (Detect)

covers: example:flag,required:set-without-remove

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  void handle(String u) {
    USER.set(u);  // FLAG
    work();
  }
}
```

## Case: remove in a finally block (none)

covers: required:remove-in-finally

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  void handle(String u) {
    USER.set(u);
    try {
      work();
    } finally {
      USER.remove();
    }
  }
  void inside(String u) {
    try {
      USER.set(u);
      work();
    } finally {
      USER.remove();
    }
  }
}
```

## Case: remove outside a finally block (Detect)

covers: required:remove-outside-finally

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  void handle(String u) {
    USER.set(u);  // FLAG
    work();
    USER.remove();
  }
}
```

## Case: remove of another holder does not count

covers: detect:same-holder

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
  void handle(String u) {
    USER.set(u);  // FLAG
    try {
      work();
    } finally {
      TENANT.remove();
    }
  }
}
```

## Case: removal in a framework callback of the same class (Do not flag: callback)

covers: do-not-flag:callback-cleans,required:after-completion

```java path=src/main/java/C.java
class Interceptor implements HandlerInterceptor {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  public boolean preHandle(HttpServletRequest r, HttpServletResponse s, Object h) {
    USER.set(r.getHeader("user"));
    return true;
  }
  public void afterCompletion(HttpServletRequest r, HttpServletResponse s, Object h, Exception e) {
    USER.remove();
  }
}
```

## Case: a ThreadLocal.withInitial cache is reported at low confidence (Do not flag: cache)

covers: do-not-flag:with-initial-low

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<SimpleDateFormat> FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy"));
  void reset(SimpleDateFormat f) {
    FORMAT.set(f);  // FLAG LOW
  }
}
```

## Case: MDC, security, locale and request holders

covers: detect:holders

```java path=src/main/java/C.java
class C {
  void a(String id) {
    MDC.put("id", id);  // FLAG
  }
  void b(SecurityContext ctx) {
    SecurityContextHolder.setContext(ctx);  // FLAG
  }
  void c(Locale l) {
    LocaleContextHolder.setLocale(l);  // FLAG
  }
  void d(ServletRequestAttributes attrs) {
    RequestContextHolder.setRequestAttributes(attrs);  // FLAG
  }
}
```

## Case: each holder released by its own call

covers: detect:release-names

```java path=src/main/java/C.java
class C {
  void a(String id) {
    MDC.put("id", id);
    try { work(); } finally { MDC.remove("id"); }
  }
  void b(String id) {
    MDC.put("id", id);
    try { work(); } finally { MDC.clear(); }
  }
  void c(SecurityContext ctx) {
    SecurityContextHolder.setContext(ctx);
    try { work(); } finally { SecurityContextHolder.clearContext(); }
  }
  void d(Locale l) {
    LocaleContextHolder.setLocale(l);
    try { work(); } finally { LocaleContextHolder.resetLocaleContext(); }
  }
  void e(ServletRequestAttributes attrs) {
    RequestContextHolder.setRequestAttributes(attrs);
    try { work(); } finally { RequestContextHolder.resetRequestAttributes(); }
  }
}
```

## Case: set(null) clears the value, and other set methods are not holders

covers: detect:holder-only

```java path=src/main/java/C.java
class C {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  void a() {
    USER.set(null);
    SecurityContextHolder.setContext(null);
  }
  void b(Map<String, String> m, Set<String> s) {
    m.put("a", "b");
    s.add("x");
    flags.set(true);
  }
}
```

## Case: test sources (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  private static final ThreadLocal<String> USER = new ThreadLocal<>();
  void f() { USER.set("x"); MDC.put("a", "b"); }
}
```

## Case: removal in a doFinally callback, directly or through a helper (Do not flag: callback)

covers: do-not-flag:callback-cleans

```java path=src/main/java/C.java
class Filter {
  Mono<Void> direct(ServerWebExchange e, WebFilterChain chain) {
    MDC.put("id", "1");
    return chain.filter(e).doFinally(sig -> MDC.remove("id"));
  }
  Mono<Void> viaHelper(ServerWebExchange e, WebFilterChain chain) {
    MDC.put("id", "1");
    return chain.filter(e).doFinally(sig -> {
      restore("id");
    });
  }
  Mono<Void> viaFinallyHelper(String id) {
    MDC.put("trace", id);
    try { work(); } finally { cleanup(); }
  }
  void cleanup() { restore("trace"); }
  void restore(String key) { MDC.remove(key); }
}
```

## Case: a doFinally callback that does not remove the holder

covers: detect:callback-removes

```java path=src/main/java/C.java
class Filter {
  Mono<Void> f(ServerWebExchange e, WebFilterChain chain) {
    MDC.put("id", "1");  // FLAG
    return chain.filter(e).doFinally(sig -> log.info("done"));
  }
}
```

## Case: putting a previous value back (Do not flag: restore branches)

covers: do-not-flag:restore-branches

```java path=src/main/java/C.java
class C {
  static void restore(String key, String previous) {
    if (previous == null) MDC.remove(key);
    else MDC.put(key, previous);
  }
  static void restore2(String key, String previous) {
    if (previous != null) {
      MDC.put(key, previous);
    } else {
      MDC.remove(key);
    }
  }
}
```

## Case: an if whose other branch does not release

covers: detect:restore-branches

```java path=src/main/java/C.java
class C {
  void f(String key, String v) {
    if (v != null) {
      MDC.put(key, v);  // FLAG
    } else {
      log.info("none");
    }
  }
}
```
