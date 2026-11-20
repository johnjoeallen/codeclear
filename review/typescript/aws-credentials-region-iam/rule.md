# typescript/aws-credentials-region-iam: Credential, region configuration, rotation and IAM scope

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs`, `**/*.json` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: AWS and serverless; node-bug-hunter: AWS and serverless failure windows |

## Intent
Credentials must come from the runtime credential chain, regions must match the resources, long-lived clients must survive rotation, and IAM policies should not exceed the operations the code uses. (Literal keys: `hardcoded-secret`.)

## Why AI
Needs runtime configuration and a supplied policy; IAM can be assessed only when the policy and the needed operations/resources are visible.

## AI prompt
"Do credentials get frozen at startup so rotation breaks long-lived clients? Does the region come from the right source for each resource? If a policy is supplied, does it grant `*` actions/resources beyond the operations in the code? Report only on visible evidence." Evidence: client construction, env/config, IAM policy, operations list.

## Do not flag
- Default credential provider chain, STS-refreshing providers.
- Policies that are not supplied.

## Examples
Flag: policy `"Action": "s3:*", "Resource": "*"` for a Lambda that only does `GetObject` on one bucket.
OK: `s3:GetObject` on `arn:aws:s3:::bucket/prefix/*`.

## Evaluation cases
1. Static session credentials read once into a long-lived client: flag.
2. Wildcard policy with visible narrow usage: flag.
3. No policy supplied: ok.
