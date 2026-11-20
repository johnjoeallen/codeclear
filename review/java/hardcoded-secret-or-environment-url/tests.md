# Tests: java/hardcoded-secret-or-environment-url

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a hardcoded password (Examples: Flag)

covers: example:flag,required:secret

```java path=src/main/java/C.java
class C {
  private String password = "Pr0dPassw0rd!";  // CANDIDATE
}
```

## Case: from the environment (Examples: OK)

covers: example:ok,do-not-flag:placeholder,required:env

```java path=src/main/java/C.java
class C {
  private String password = System.getenv("DB_PASSWORD");
  @Value("${app.password}") private String fromConfig;
  private String empty = "";
  private String masked = "changeme-please";
  private static final String PASSWORD_FIELD = "password";
}
```

## Case: known formats

covers: detect:formats

```java path=src/main/java/C.java
class C {
  void f() {
    use("AKIAIOSFODNN7EXAMPL1");  // CANDIDATE
    use("-----BEGIN RSA PRIVATE KEY-----");  // CANDIDATE
    use("Bearer abcdefghijklmnopqrstuvwxyz");  // CANDIDATE
  }
}
```

## Case: environment URLs

covers: detect:urls,required:url

```java path=src/main/java/C.java
class C {
  String a = "http://localhost:8080/api";  // CANDIDATE
  String b = "https://10.0.0.5/api";  // CANDIDATE
  String c = "https://api.staging.acme.io/v1";  // CANDIDATE
  String d = "https://billing.internal/x";  // CANDIDATE
  String e = "https://api.acme.io/v1";
}
```

## Case: schema URLs and configuration classes (Do not flag)

covers: do-not-flag:schema-and-config

```java path=src/main/java/C.java
class C {
  String ns = "http://www.w3.org/2001/XMLSchema";
  String x = "http://maven.apache.org/POM/4.0.0";
}
@Configuration
class Cfg {
  String url = "http://localhost:8080";
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  String password = "Pr0dPassw0rd!";
  String url = "http://localhost:8080";
}
```
