# common/placeholder-default-removed: Default value taken out of a property placeholder

| Field | Value |
|---|---|
| Applies to | `**/*.java`, `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | CONFIG |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | config-api-review: a fallback removed from a placeholder |

## Intent
`${key:default}` keeps the application starting when nothing defines `key`. Changing it to `${key}` makes the application fail at start-up in every environment that relied on the default, and nothing in the diff says which ones do.

## Detect
Only when reviewing a diff (the file as it was at the base is needed):
- In a Java string or a configuration value, a placeholder `${key}` with no default, on a changed line, where the same file had `${key:default}` at the diff base.

## Do not flag
- A placeholder whose key a configuration file of the change now defines.

## Examples
Flag: `@Value("${payment.timeout:30}")` becomes `@Value("${payment.timeout}")`, or `url: ${app.url:http://localhost}` becomes `url: ${app.url}`.
OK: `${key:default}` becomes `${key:other}`, or the default stays.

## Implementation notes
The placeholder is matched by its key in the same file at the diff base; a placeholder removed together with its line is not reported, nor is a key that never had a default. Without a diff there is no base and nothing is reported. Whether an environment supplies the key is judged by the AI reviewer.

## Required tests
Default removed in Java (candidate), default removed in config (candidate), default changed (none), key never had a default (none), key defined in a config file of the change (none), no diff base (none).

## AI prompt
"Is the key defined in every environment this application runs in (config files, environment variables, deployment manifests)? If not, the application now fails at start-up. Report only if the supplied files show no definition."
Evidence: the old and new placeholder, all config files and deployment files in the repository.

## Why hybrid
The removal is found by comparing text, but whether each environment defines the key is outside the diff.
