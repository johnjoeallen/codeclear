# config/removed-setting-still-read: Setting removed from configuration but still read by code

| Field | Value |
|---|---|
| Applies to | `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | CONFIG |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | config-api-review: removed configuration key with a remaining consumer |

## Intent
A key taken out of a configuration file makes every reader of it fail at start-up or quietly fall back to a default. The change under review is the config file, so the reader is in a file the change did not touch, and nothing else points at it.

## Detect
Only when reviewing a diff (the file as it was at the base is needed):
- A key that the file had at the diff base and no longer has, while a source file of the project still reads it (`@Value("${old.key}")`, `getProperty("old.key")`, `@ConfigurationProperties(prefix = "old")` with a matching field, `process.env.OLD_KEY`, `os.environ["OLD_KEY"]`), in any spelling Spring accepts (`max-retries`, `maxRetries`, `max_retries`, `MAX_RETRIES`).
- The finding is reported on the line of the nearest setting that remains in the file, and names the old value and the lines that still read the key.

## Do not flag
- A key that moved to another file of the change (it is still defined there), or became a nested key (`a` replaced by `a.b`).
- A reader in test code.

## Examples
Flag: `application.yml` drops `payment.timeout`, but `PaymentClient.java` still has `@Value("${payment.timeout:30}")`.
OK: the key and its readers are removed together.

## Implementation notes
The old file is read with `git show <base>:<path>`; its settings are compared with the current ones. The readers are found by a text search over every source file under the scanned folders (the changed ones and the rest), so a whole-file review, which has no base, never reports this. A key read through a prefix (`@ConfigurationProperties`) is found only when its full dotted name or environment name appears in the source.

## Required tests
Removed key still read (candidate), removed key no longer read (none), key moved to another file (none), key that became a nested key (none), reader only in test code (none), key renamed with reader updated (none), environment-style reader (candidate), no diff base (none).

## AI prompt
"Does the remaining code really need this setting, or does it have a default that makes the removal harmless? Is the key supplied elsewhere (environment variables, deployment manifests, another config file)? Report only if the application would fail or silently behave differently."
Evidence: the old value, the lines that still read the key.

## Why hybrid
The text search finds readers, but whether a default or the environment covers the key is judged by the AI.
