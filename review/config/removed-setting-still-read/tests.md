# Tests: config/removed-setting-still-read

A `base` file is the config file as it was at the diff base. A `# CANDIDATE` marker ends the line the finding is reported on:
the nearest setting that remains.

## Case: a removed key that code still reads (Examples: Flag)

covers: example:flag,detect:removed-key-read

```yaml path=src/main/resources/application.yml base
payment:
  timeout: 30
  url: http://pay
```

```yaml path=src/main/resources/application.yml
payment:
  url: http://pay  # CANDIDATE
```

```java path=src/main/java/PaymentClient.java
class PaymentClient {
  @Value("${payment.timeout:30}")
  int timeout;
}
```

## Case: a removed key that nothing reads (Examples: OK)

covers: example:ok

```yaml path=src/main/resources/application.yml base
payment:
  timeout: 30
  url: http://pay
```

```yaml path=src/main/resources/application.yml
payment:
  url: http://pay
```

```java path=src/main/java/PaymentClient.java
class PaymentClient {
  @Value("${payment.url}")
  String url;
}
```

## Case: a key that moved to another file

covers: do-not-flag:moved

```yaml path=src/main/resources/application.yml base
payment:
  timeout: 30
  url: http://pay
```

```yaml path=src/main/resources/application.yml
payment:
  url: http://pay
```

```yaml path=src/main/resources/application-prod.yml
payment:
  timeout: 60
```

```java path=src/main/java/PaymentClient.java
class PaymentClient {
  @Value("${payment.timeout}")
  int timeout;
}
```

## Case: renamed with the reader updated

covers: do-not-flag:renamed,detect:longer-name

```yaml path=src/main/resources/application.yml base
payment:
  timeout: 30
  url: http://pay
```

```yaml path=src/main/resources/application.yml
payment:
  timeoutMs: 30000
  url: http://pay
```

```java path=src/main/java/PaymentClient.java
class PaymentClient {
  @Value("${payment.timeout-ms}")
  int timeout;
}
```

## Case: an environment-style reader

covers: detect:environment-name

```properties path=src/main/resources/application.properties base
app.mail-host=smtp
app.name=shop
```

```properties path=src/main/resources/application.properties
app.name=shop  # CANDIDATE
```

```typescript path=src/mail.ts
const host = process.env.APP_MAIL_HOST;
```

## Case: no base, so a whole-file review

covers: detect:needs-diff

```yaml path=src/main/resources/application.yml
payment:
  url: http://pay
```

```java path=src/main/java/PaymentClient.java
class PaymentClient {
  @Value("${payment.timeout}")
  int timeout;
}
```

## Case: the key became a nested key

covers: do-not-flag:nested

```yaml path=src/main/resources/application.yml base
payment:
  timeout: 30
```

```yaml path=src/main/resources/application.yml
payment:
  timeout:
    seconds: 30
```

```java path=src/main/java/PaymentClient.java
class PaymentClient {
  @Value("${payment.timeout.seconds}")
  int timeout;
}
```

## Case: only a test reads the key

covers: do-not-flag:test-reader

```yaml path=src/main/resources/application.yml base
payment:
  timeout: 30
  url: http://pay
```

```yaml path=src/main/resources/application.yml
payment:
  url: http://pay
```

```java path=src/test/java/PaymentClientTest.java
class PaymentClientTest {
  @Value("${payment.timeout}")
  int timeout;
}
```
