# Tests: common/placeholder-default-removed

A `base` file is the file as it was at the diff base.

## Case: the default is taken out in Java (Examples: Flag)

covers: example:flag,detect:java

```java path=src/main/java/P.java base
class P {
  @Value("${payment.timeout:30}")
  int timeout;
}
```

```java path=src/main/java/P.java
class P {
  @Value("${payment.timeout}")  // CANDIDATE
  int timeout;
}
```

## Case: the default is taken out in a config file

covers: detect:config

```yaml path=src/main/resources/application.yml base
app:
  url: ${app.base-url:http://localhost}
```

```yaml path=src/main/resources/application.yml
app:
  url: ${app.base-url}  # CANDIDATE
```

## Case: the default is changed or kept (Examples: OK)

covers: example:ok

```java path=src/main/java/P.java base
class P {
  @Value("${payment.timeout:30}")
  int timeout;
  @Value("${payment.retries:3}")
  int retries;
}
```

```java path=src/main/java/P.java
class P {
  @Value("${payment.timeout:60}")
  int timeout;
  @Value("${payment.retries:3}")
  int retries;
}
```

## Case: the key never had a default

covers: detect:never-had-default

```java path=src/main/java/P.java base
class P {
  @Value("${payment.timeout}")
  int timeout;
}
```

```java path=src/main/java/P.java
class P {
  @Value("${payment.timeout}")
  int timeout;
}
```

## Case: the key is now defined in a config file

covers: do-not-flag:defined

```java path=src/main/java/P.java base
class P {
  @Value("${payment.timeout:30}")
  int timeout;
}
```

```java path=src/main/java/P.java
class P {
  @Value("${payment.timeout}")
  int timeout;
}
```

```yaml path=src/main/resources/application.yml
payment:
  timeout: 30
```

## Case: no diff base, so a whole-file review

covers: detect:needs-diff

```java path=src/main/java/P.java
class P {
  @Value("${payment.timeout}")
  int timeout;
}
```
