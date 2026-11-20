# java/pagination-batching-boundary-error: Off-by-one or lost items in pagination and batching

| Field | Value |
|---|---|
| Applies to | `**/*.java` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | java-bug-hunter: Exceptions, resources and control flow |

## Intent
Pagination and batching code commonly drops the last partial page, repeats the boundary item, loops forever on an empty page, or mixes zero-based and one-based pages.

## Why AI
Correctness depends on the intended page semantics (0/1-based, inclusive limits, sort stability) and data shapes that must be traced by hand.

## AI prompt
"Walk through the paging/batching logic with these inputs: empty result, exactly one full page, one page plus one item, and the last partial batch. Report only an input that yields a lost item, duplicate, infinite loop or exception, showing the trace." Evidence: the loop/paging method, page size source, repository query ordering and Pageable semantics.

## Do not flag
- Logic using framework-managed paging (`Slice.hasNext()`, `Page.hasNext()`) correctly.
- Cases where the trace cannot be computed from shown code.

## Examples
Flag: `for (int i = 0; i < items.size() / size; i++) process(items.subList(i*size, (i+1)*size));` (drops the remainder).
OK: `for (int i = 0; i < items.size(); i += size) process(items.subList(i, Math.min(i + size, items.size())));`.

## Evaluation cases
1. Division-based batch count dropping remainder - report.
2. `Math.min` end index with step loop - no report.
3. Page loop `while (!page.isEmpty())` without incrementing page - report.
4. Uses `Page.hasNext()` correctly - no report.
