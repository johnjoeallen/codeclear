# config/secret-logged-by-config: Configuration that makes secrets appear in logs

| Field | Value |
|---|---|
| Applies to | `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | hybrid |
| Source | config-api-review: secrets checked into config or logged |

## Intent
Verbose logging settings write request headers, bodies, SQL parameters or whole configuration to logs, which exposes tokens and personal data. Literal secrets are covered by `config/secret-in-config`.

## Detect
Keys from a maintained list set to a verbose value outside dev/test files:
- `logging.level.org.apache.http.wire|org.apache.hc.client5.http.wire: DEBUG|TRACE`
- `logging.level.org.springframework.web.client|org.springframework.web.filter.CommonsRequestLoggingFilter: DEBUG|TRACE`
- `spring.mvc.log-request-details: true`, `logging.level.org.hibernate.type.descriptor.sql: TRACE`
- `management.endpoint.env.show-values: ALWAYS`, `management.endpoint.configprops.show-values: ALWAYS`
- `*.log-headers|log-body|logRequestBody|log-payload: true`

## Do not flag
- Dev, local and test files (`*-dev.*`, `*-local.*`, `src/test/**`).
- Values supplied by placeholders resolved per environment.

## Examples
Flag: `logging.level.org.apache.http.wire: DEBUG` in `application.yml`.
OK: the same key in `application-dev.yml`.

## Implementation notes
Keep the list as data. Parse to key paths, report the key path and profile.

Detected on the keys of the rule as settings: the HTTP wire loggers at `DEBUG`/`TRACE`, Spring web client and request-logging loggers at `DEBUG`/`TRACE`, `spring.mvc.log-request-details: true`, Hibernate SQL parameter binding at `TRACE`, the actuator `env` and `configprops` `show-values: ALWAYS`, and any key ending in `log-headers`, `log-body`, `log-request-body`, `log-response-body`, `log-payload` or `log-request(s)` set to `true`. Files named `*-dev`, `*-local`, `*-test`, files under `src/test` and profiles dev, local and test are skipped.

## Required tests
Each listed key in a base file (candidate), same in dev profile (none), `INFO` level (none).

## AI prompt
"Does this setting, in the profile where it applies, cause credentials, tokens or personal data to be written to logs or exposed by an endpoint? Use the supplied code and config to say which data passes through it. Report only with a concrete data path; otherwise drop."
Evidence: the key and value, the file profile, the consuming code or library, other profile overrides.

## Why hybrid
The key list is deterministic, but harm depends on what data flows through the logger and whether another profile overrides the value.
