# Tests: java/missing-network-timeout

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a RestTemplate with no timeout (Examples: Flag)

covers: example:flag,required:rest-template

```java path=src/main/java/C.java
class C {
  RestTemplate rt() {
    return new RestTemplate();  // CANDIDATE
  }
}
```

## Case: a builder with timeouts (Examples: OK)

covers: example:ok,do-not-flag:builder,required:builder

```java path=src/main/java/C.java
class C {
  RestTemplate rt(RestTemplateBuilder b) {
    return b.setConnectTimeout(Duration.ofSeconds(2)).setReadTimeout(Duration.ofSeconds(5)).build();
  }
}
```

## Case: HttpClient, Socket, URL and WebClient

covers: detect:clients

```java path=src/main/java/C.java
class C {
  void f(URL url) throws Exception {
    var a = HttpClient.newHttpClient();  // CANDIDATE
    var b = HttpClient.newBuilder().build();  // CANDIDATE
    var s = new Socket("h", 80);  // CANDIDATE
    var u = url.openConnection();  // CANDIDATE
    var w = WebClient.create();  // CANDIDATE
  }
}
```

## Case: timeouts set in the same class (Do not flag)

covers: do-not-flag:timeouts-in-class

```java path=src/main/java/C.java
class C {
  HttpClient a() { return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(); }
  Socket b() throws Exception { var s = new Socket("h", 80); s.setSoTimeout(1000); return s; }
  URLConnection c(URL u) throws Exception { var x = u.openConnection(); x.setConnectTimeout(1000); x.setReadTimeout(1000); return x; }
}
```

## Case: futures and conditions waited on with no timeout

covers: detect:waits

```java path=src/main/java/C.java
class C {
  void f(Future<String> fut, Condition cond, Map<String, String> m) throws Exception {
    fut.get();  // CANDIDATE
    cond.await();  // CANDIDATE
    fut.get(1, TimeUnit.SECONDS);
    m.get("k");
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  RestTemplate rt() { return new RestTemplate(); }
}
```
