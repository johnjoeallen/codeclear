# Tests: java/config-properties-without-validation

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an unvalidated properties class (Examples: Flag)

covers: example:flag,required:unvalidated

```java path=src/main/java/C.java
@ConfigurationProperties("payments")
class PaymentProps {
  String baseUrl;  // CANDIDATE
  int timeoutMs;  // CANDIDATE
  String currency;
}
```

## Case: @Validated with constraints (Examples: OK)

covers: example:ok,do-not-flag:validated,required:validated

```java path=src/main/java/C.java
@Validated
@ConfigurationProperties("payments")
class PaymentProps {
  @NotBlank String baseUrl;
  @Min(1) int timeoutMs;
}
```

## Case: a constraint on the field of a class without @Validated

covers: detect:field-constraint

```java path=src/main/java/C.java
@ConfigurationProperties("payments")
class PaymentProps {
  @NotBlank String baseUrl;
  @Positive int timeoutMs;
}
```

## Case: optional properties with a default value (Do not flag)

covers: do-not-flag:default-value

```java path=src/main/java/C.java
@ConfigurationProperties("payments")
class PaymentProps {
  int timeoutMs = 5000;
  String baseUrl = "http://localhost";
}
```

## Case: @Value with a zero, null or empty default

covers: detect:value-default

```java path=src/main/java/C.java
class C {
  @Value("${client.timeout:0}")  // CANDIDATE
  private int timeout;
  @Value("${client.url:}")  // CANDIDATE
  private String url;
  @Value("${client.size:10}")
  private int size;
  @Value("${client.name:}")
  private String name;
}
```

## Case: a property that is not used on a failure-prone path

covers: do-not-flag:not-failure-prone

```java path=src/main/java/C.java
@ConfigurationProperties("ui")
class UiProps {
  String title;
  String greeting;
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
@ConfigurationProperties("x")
class CTest {
  int timeoutMs;
}
```
