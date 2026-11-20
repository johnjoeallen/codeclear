# config/api-contract-breaking-change: OpenAPI change that breaks existing clients

| Field | Value |
|---|---|
| Applies to | OpenAPI / Swagger `**/*.yaml`, `**/*.yml`, `**/*.json` |
| Category | API_CONTRACT |
| Severity | HIGH |
| Confidence | HIGH |
| Detection type | ast |
| Source | config-api-review: incompatible API field or status changes |

## Intent
Removing or tightening something clients rely on breaks them. This is detectable by comparing the old and new spec.

## Detect
Compare the spec at the merge base with the spec in the change. Report:
- removed path or HTTP method;
- removed response field, or response field type changed;
- optional request field made required; new required request parameter;
- enum value removed from a response, or added to a request where clients validate;
- success status code changed or removed (`200` to `201`);
- `nullable` changed from true to false on a response.

## Do not flag
- Additions of optional fields, paths or response enum values.
- Specs that declare a new major version path (`/v2/`).
- Operations marked `deprecated: true` before the change in the base version.

## Examples
Flag: removing `email` from the `User` response schema.
OK: adding optional `nickname` to the `User` response schema.

## Implementation notes
Do not write a differ. Use `oasdiff` or `openapi-diff` and map its breaking-change output to findings. The detector needs the base version of the file as well as the new one.

## Required tests
One fixture pair for each bullet in Detect, plus an additive change (no finding).

## Why deterministic
Comparing two parsed spec versions for removals and tightening is mechanical and needs no intent. Clients not shown in the supplied files are not speculated about.
