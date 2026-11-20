# java/aws-overbroad-iam-policy: IAM policy broader than the operations used

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | java-bug-hunter: AWS SDK |

## Intent
IAM policies using `*` for actions or resources grant far more than the code needs, widening the blast radius of a compromise.

## Detect
In a policy supplied in the Java source (CDK `PolicyStatement`, JSON policy strings, `PolicyDocument` builders): `Action` or `actions` containing `"*"` or `"<service>:*"`, or `Resource` equal to `"*"` together with data-plane actions.

## Do not flag
- `Resource: "*"` for actions that require it (`sts:GetCallerIdentity`, `logs:CreateLogGroup`, `xray:Put*`).
- Policies not in the supplied context.
- Test sources.

## Examples
Flag:
```java
PolicyStatement.Builder.create().actions(List.of("s3:*")).resources(List.of("*")).build();
```
OK: `actions(List.of("s3:GetObject")).resources(List.of(bucket.arnForObjects("reports/*")))`.

## Implementation notes
AST/regex on policy builders and string literals. Anchor to the statement.

Detected on string literals: an action `"*"` or `"<service>:*"` passed to `actions`/`addActions`, a JSON policy string with such an `Action`, and `"*"` passed to `resources`/`addResources` (or a JSON `Resource`) unless the statement names only actions that need it (`sts:GetCallerIdentity`, `logs:Create*`, `xray:Put*`). A policy built from constants or variables is not followed.

## Required tests
`s3:*` on `*` (candidate), scoped action and resource (none), `sts:GetCallerIdentity` on `*` (none).

## Why hybrid
The narrowest correct scope depends on the operations and resources the code actually uses.

## AI prompt
"Compare the granted actions/resources with the AWS calls this code makes. Propose the minimal scope and report only if the policy grants materially more." Evidence: the policy, the SDK calls and resource names in the module.
