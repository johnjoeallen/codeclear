# python/aws-credentials-region-iam: Credential/region lookup, rotation and IAM scope

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: AWS and serverless; python-bug-hunter: AWS and serverless failure windows |

## Intent
Explicit long-lived credentials or fixed region override the default provider chain and break rotation or deployment targets; credentials cached beyond expiry; IAM policies grant broader actions/resources than the operations the code needs. Hard-coded secrets are covered by `hardcoded-credentials`.

## Why AI
Needs IAM policy documents, the operations/resources used, and deployment region/role setup; assess IAM only when both are supplied.

## AI prompt
"Compare the supplied IAM policy with the operations and resources the code uses. Does it grant wildcard or unrelated access? Do client constructors bypass the default credential/region chain or cache credentials past expiry?" Supply the policy, code and deployment config. Report only with a concrete trigger scenario; do not assess IAM without a policy; otherwise NO_FINDING.

## Do not flag
- Default provider chain; tightly scoped policies; region from configuration.

## Examples
Flag:
```json
{"Effect": "Allow", "Action": "s3:*", "Resource": "*"}     // code only calls GetObject on one bucket
```
OK:
```json
{"Effect": "Allow", "Action": "s3:GetObject", "Resource": "arn:aws:s3:::reports/*"}
```

## Evaluation cases
1. Above flag example -> flag.
2. `boto3.client("s3", region_name="us-east-1")` while deployed in eu-west-1 (shown) -> flag.
3. `boto3.client("s3")` with role-based credentials -> no finding.
4. Policy not supplied -> no finding.
