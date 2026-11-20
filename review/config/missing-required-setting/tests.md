# Tests: config/missing-required-setting

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a placeholder for a key that nothing defines (Examples: Flag)

covers: example:flag,required:undefined-key

```yaml path=src/main/resources/application.yml
app:
  mail:
    from: ${app.mail.sender}  # CANDIDATE
```

## Case: a key that is defined, a default, and an environment variable (Examples: OK)

covers: example:ok,do-not-flag:defined-or-default-or-env,required:defined

```yaml path=src/main/resources/application.yml
app:
  mail:
    sender: noreply@acme.io
    from: ${app.mail.sender}
    reply: ${app.mail.reply:support@acme.io}
    host: ${MAIL_HOST}
```

## Case: a key defined in another file of the run

covers: detect:other-file

```properties path=src/main/resources/application.properties
app.name=${app.title}
```

```yaml path=src/main/resources/application-prod.yml
app:
  title: Shop
```

## Case: a key spelt another way

covers: detect:relaxed-spelling

```yaml path=src/main/resources/application.yml
app:
  maxRetries: 3
  retry: ${app.max-retries}
```

## Case: several placeholders in one value

covers: detect:several

```yaml path=src/main/resources/application.yml
app:
  url: http://${app.host}:${app.port}/x  # CANDIDATE
  host: localhost
```
