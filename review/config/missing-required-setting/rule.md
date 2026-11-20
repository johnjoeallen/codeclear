# config/missing-required-setting: Config key with no matching consumer or value

| Field | Value |
|---|---|
| Applies to | `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | CONFIG |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | config-api-review: mismatch between a configuration key and its consumer |

## Intent
A renamed or removed key whose consumer still reads the old name fails at startup or silently uses a default.

## Detect
Within the changed files:
- A key removed or renamed in config while a changed or existing source file still references the old key (`@Value("${old.key}")`, `@ConfigurationProperties(prefix = "old")`, `process.env.OLD_KEY`, `os.environ["OLD_KEY"]`).
- A new `@Value("${key}")` or `os.environ["KEY"]` with no default and no definition in any config file in the repository.

## Do not flag
- Keys provided by the environment, documented in `README`/deployment files, or having a default (`${key:default}`, `os.environ.get`).

## Examples
Flag: `application.yml` renames `payment.timeout` to `payment.timeoutMs`, but code still has `@Value("${payment.timeout}")`.
OK: both renamed together.

## Implementation notes
Candidate generation is a repository search; the AI reviewer judges whether the key is provided elsewhere (`hybrid`).

## Required tests
Rename without consumer update (finding), rename with update (no finding), new key with default (no finding), new key without definition (finding).

## AI prompt
"Is the key used by the consumer provided elsewhere (environment variables, deployment manifests, Helm values, documented secrets)? Would the application fail at startup or silently use a wrong value? Report only if the supplied files show no source for the key."
Evidence: the config diff, the consuming code line, all config files and deployment files in the repository.

## Why hybrid
Key/consumer mismatches are found by search, but whether the key is supplied by the environment is outside the repository text.
