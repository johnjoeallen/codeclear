# config/environment-specific-behaviour-change: Config value changes behaviour between environments

| Field | Value |
|---|---|
| Applies to | `**/*.yaml`, `**/*.yml`, `**/*.properties`, `**/*.json` |
| Category | CONFIG |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | config-api-review: config values that unintentionally change behavior between environments |

## Intent
A change in a base file, or in one profile file, that alters behaviour in production (or another environment) in a way the author did not intend, for example a dev-only URL, pool size, feature flag or timeout landing in a shared file.

## Why AI
Intent and the effective value per environment need the whole profile hierarchy, deployment overrides and the consumer's semantics. A key diff cannot show whether the change was meant for one environment.

## AI prompt
"Compute the effective value of each changed key for every environment visible in the supplied files (base, profile files, deployment manifests, environment variable maps). Report only if the change makes an environment behave differently from what the MR description or neighbouring config indicates, naming the key, the environment and the before/after effective values. If the other environments' configuration is not supplied, report nothing."
Evidence: the config diff, all sibling profile files, deployment or Helm values that override keys, the MR description, and the consumer of the key.

## Do not flag
- Intentional, documented per-environment differences.
- Generated artifacts and lock files.
- Environments whose configuration is not shown.

## Examples
Flag: base `application.yml` changes `payments.endpoint` to `https://sandbox.pay.example`, and `application-prod.yml` does not override it.
OK: the same change in `application-dev.yml`.

## Evaluation cases
1. Timeout lowered in base file, prod profile overrides it: ok.
2. Feature flag enabled in base file, no prod override, MR says "dev only": flag.
3. Pool size changed in `application-prod.yml` only, description says prod tuning: ok.
4. Hostname with `localhost` added to base file: flag.
