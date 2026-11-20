# Tests: common/hardcoded-credential

A `// FLAG` marker ends each line that must produce a finding.

## Case: a hardcoded key (Examples: Flag)

covers: example:flag,required:hardcoded-key

```java path=src/main/java/C.java
class C {
  private static final String API_KEY = "sk_live_9f8a7d6c5b4a";  // FLAG
}
```

## Case: an environment lookup (Examples: OK)

covers: example:ok,do-not-flag:env-lookup,required:env-lookup

```java path=src/main/java/C.java
class C {
  private final String apiKey = System.getenv("API_KEY");
  private final String password = config.get("db.password");
}
```

## Case: a token inside a comment

covers: required:comment

```java path=src/main/java/C.java
class C {
  // old key: AKIAIOSFODNN7EXAMPL1  // FLAG
  // password = "hunter2hunter2"  // FLAG
  void f() { }
}
```

## Case: placeholders and empty strings (Do not flag)

covers: do-not-flag:placeholder,required:placeholder

```java path=src/main/java/C.java
class C {
  String password = "changeme";
  String token = "<token>";
  String secret = "${SECRET}";
  String apiKey = "";
  String key2 = "xxxxxxxxxxxx";
}
```

## Case: constants that name a key (Do not flag)

covers: do-not-flag:key-name

```java path=src/main/java/C.java
class C {
  static final String PASSWORD_FIELD = "password";
  static final String TOKEN_HEADER = "x-api-token";
  static final String SECRET_KEY_NAME = "client_secret";
}
```

## Case: known token formats and a URL with a password

covers: detect:formats

```java path=src/main/java/C.java
class C {
  void f() {
    use("AKIAIOSFODNN7EXAMPL1");  // FLAG
    use("ghp_abcdefghijklmnopqrstuvwxyz0123456789");  // FLAG
    use("xoxb-123456-abcdef");  // FLAG
    use("-----BEGIN RSA PRIVATE KEY-----");  // FLAG
    use("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0");  // FLAG
    use("jdbc:postgresql://admin:s3cr3tpw@db.internal.local/x");  // FLAG
  }
}
```

## Case: a short secret-named literal, and an assignment

covers: detect:assignment

```java path=src/main/java/C.java
class C {
  String password;
  String token = "short";
  void f() {
    password = "correct-horse-battery";  // FLAG
  }
}
```

## Case: test files with obviously fake values (Do not flag)

covers: do-not-flag:fake-in-tests

```java path=src/test/java/CTest.java
class CTest {
  String password = "test-password-123";
  String token = "dummy-token-value";
}
```

## Case: the finding never contains the value

covers: required:masked-output

```java path=src/main/java/C.java
class C {
  String apiKey = "sk_live_9f8a7d6c5b4a";  // FLAG
}
```
