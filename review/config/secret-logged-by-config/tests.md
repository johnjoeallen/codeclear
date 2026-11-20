# Tests: config/secret-logged-by-config

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: the HTTP wire logger at DEBUG (Examples: Flag)

covers: example:flag,required:wire

```yaml path=src/main/resources/application.yml
logging:
  level:
    org.apache.http.wire: DEBUG  # CANDIDATE
```

## Case: ordinary log levels (Examples: OK)

covers: example:ok,required:ordinary

```yaml path=src/main/resources/application.yml
logging:
  level:
    root: INFO
    org.apache.http.wire: WARN
    com.acme: DEBUG
```

## Case: each listed key

covers: detect:keys

```properties path=src/main/resources/application.properties
logging.level.org.apache.hc.client5.http.wire=TRACE  # CANDIDATE
logging.level.org.springframework.web.client=DEBUG  # CANDIDATE
logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG  # CANDIDATE
spring.mvc.log-request-details=true  # CANDIDATE
logging.level.org.hibernate.type.descriptor.sql=TRACE  # CANDIDATE
management.endpoint.env.show-values=ALWAYS  # CANDIDATE
management.endpoint.configprops.show-values=ALWAYS  # CANDIDATE
client.log-headers=true  # CANDIDATE
client.log-body=true  # CANDIDATE
```

## Case: dev, local and test files and profiles (Do not flag)

covers: do-not-flag:dev-files,required:dev

```yaml path=src/main/resources/application-dev.yml
spring:
  mvc:
    log-request-details: true
```

```yaml path=src/main/resources/application-local.yml
logging:
  level:
    org.apache.http.wire: TRACE
```

```yaml path=src/test/resources/application.yml
client:
  log-body: true
```

## Case: values from the environment (Do not flag)

covers: do-not-flag:placeholder

```yaml path=src/main/resources/application.yml
logging:
  level:
    org.apache.http.wire: ${WIRE_LEVEL:INFO}
client:
  log-body: ${LOG_BODY:false}
```
