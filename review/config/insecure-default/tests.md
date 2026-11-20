# Tests: config/insecure-default

A `# FLAG` marker ends each line that must produce a finding.

## Case: every actuator endpoint exposed (Examples: Flag)

covers: example:flag,required:exposure

```yaml path=src/main/resources/application.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"  # FLAG
```

## Case: only some endpoints (Examples: OK)

covers: example:ok,required:safe-value

```yaml path=src/main/resources/application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

## Case: each listed key

covers: required:listed-keys,detect:keys

```yaml path=src/main/resources/application.yml
management:
  endpoint:
    health:
      show-details: always  # FLAG
server:
  ssl:
    enabled: false  # FLAG
client:
  ssl:
    verify: false  # FLAG
  tls:
    insecure-skip-verify: true  # FLAG
  trustAllCertificates: true  # FLAG
spring:
  security:
    oauth2:
      enabled: false  # FLAG
  h2:
    console:
      enabled: true  # FLAG
  jpa:
    hibernate:
      ddl-auto: update  # FLAG
app:
  auth:
    enabled: false  # FLAG
  csrf:
    enabled: false  # FLAG
logging:
  level:
    root: OFF  # FLAG
```

## Case: properties files and camel-case keys

covers: detect:properties

```properties path=src/main/resources/application.properties
server.ssl.enabled=false  # FLAG
spring.jpa.hibernate.ddl-auto=create-drop  # FLAG
spring.jpa.hibernate.ddl-auto=validate
client.trustAllCertificates=true  # FLAG
```

## Case: CORS with every origin and credentials

covers: detect:cors

```yaml path=src/main/resources/application.yml
cors:
  allowed-origins: "*"  # FLAG
  allow-credentials: true
```

```yaml path=src/main/resources/other.yml
cors:
  allowed-origins: "*"
  allow-credentials: false
```

## Case: the same keys in a dev, local or test file (Do not flag)

covers: do-not-flag:dev-profile,required:dev-profile

```yaml path=src/main/resources/application-dev.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

```yaml path=src/main/resources/application-local.yml
server:
  ssl:
    enabled: false
```

```yaml path=src/test/resources/application.yml
spring:
  h2:
    console:
      enabled: true
```

## Case: a document for a dev profile inside a multi-document file

covers: detect:multi-document

```yaml path=src/main/resources/application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
---
spring:
  config:
    activate:
      on-profile: dev
  jpa:
    hibernate:
      ddl-auto: update
```

## Case: values from the environment (Do not flag)

covers: do-not-flag:env-placeholder,required:env-placeholder

```yaml path=src/main/resources/application.yml
server:
  ssl:
    enabled: ${SSL_ENABLED:false}
management:
  endpoints:
    web:
      exposure:
        include: ${EXPOSED:*}
```

## Case: JSON

covers: detect:json

```json path=config/settings.json
{
  "server": { "ssl": { "enabled": false } }  // FLAG
}
```
