# java/rolling-deploy-incompatibility: Contract change unsafe while old and new versions coexist

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Spring and distributed-service behavior |

## Intent
During a rolling deployment old and new application versions run together against the same database and message contracts. Renamed or dropped columns, changed message fields and required new fields break the old (or new) version.

## Why AI
It needs the deployment model, migration order and consumer versions, and must prefer additive reasoning without inferring deployment order that is not given.

## AI prompt
"Identify database schema, entity mapping, or message contract changes in this diff. State whether the old version still works against the new schema/messages and vice versa, using only the deployment facts shown. Report only a concrete incompatibility (name the column/field and which version fails). Recommend an additive/expand-contract step when applicable." Evidence: entity/message diff, migration scripts, deployment notes or ticket.

## Do not flag
- Additive nullable columns and optional message fields.
- Environments with stated downtime deployment.
- Incompatibilities requiring unstated deployment order.

## Examples
Flag: column `status` renamed to `state` in a migration and entity in the same MR, with rolling deployment stated.
OK: new `state` column added; code writes both; old column dropped in a later release.

## Evaluation cases
1. Column rename with rolling deployment - report.
2. New nullable column - no report.
3. New required message field consumed by old consumers (shown) - report.
4. Deployment model unknown, additive change - no report.
