# config/insecure-default: Setting that disables security or exposes internals

| Field | Value |
|---|---|
| Applies to | `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | regex |
| Source | config-api-review: unsafe defaults |

## Intent
Defaults ship to every environment. A setting that disables authentication, verification or hides logs is unsafe where it is not overridden.

## Detect
Key/value pairs from a maintained list, for example:
- `management.endpoints.web.exposure.include: "*"`
- `management.endpoint.health.show-details: always` (outside a profile file for `dev` / `local`)
- `server.ssl.enabled: false`, `*.ssl.verify: false`, `*.tls.insecure-skip-verify: true`, `trustAllCertificates: true`
- `spring.security.*.enabled: false`, `*.auth.enabled: false`, `*.csrf.enabled: false`
- `logging.level.root: OFF`, `spring.h2.console.enabled: true`
- `spring.jpa.hibernate.ddl-auto: create|create-drop|update` outside dev/test profiles
- `cors.allowed-origins: "*"` together with `allow-credentials: true`

## Do not flag
- Files whose name or profile marks them local or test: `*-dev.*`, `*-local.*`, `*-test.*`, `src/test/**`.
- Values supplied from environment placeholders.

## Examples
Flag:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
```
OK:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

## Implementation notes
Store the list as data (YAML) so rules can be added without code changes. Report the key path and the file profile.

## Required tests
Each listed key (finding), the same key in `application-dev.yml` (no finding), env placeholder (no finding).

## Why deterministic
Each entry is a fixed key and value on a maintained list, in a file whose profile is known from its name.
