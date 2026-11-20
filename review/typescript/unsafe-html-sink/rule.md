# typescript/unsafe-html-sink: innerHTML, dangerouslySetInnerHTML or eval with dynamic data

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | typescript-review: Security |

## Intent
Writing unsanitized data to an HTML sink, or evaluating it as code, allows cross-site scripting or code injection.

## Detect
- Assignment to `.innerHTML` / `.outerHTML`, `insertAdjacentHTML`, `document.write`.
- JSX attribute `dangerouslySetInnerHTML` with `__html` that is not a string literal.
- `eval(...)`, `new Function(...)`, `setTimeout(string, ...)`.
where the value is not a string literal.

## Do not flag
- String literals, or values passed through a recognised sanitizer (`DOMPurify.sanitize`, `sanitizeHtml`).
- Assignment of an empty string (`el.innerHTML = ''`).

## Examples
Flag:
```tsx
<div dangerouslySetInnerHTML={{ __html: comment.body }} />
```
OK:
```tsx
<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(comment.body) }} />
```

## Implementation notes
The detector finds the sink; the AI reviewer decides whether the data is attacker-controlled. Equivalent: ESLint `no-eval`, `react/no-danger`.

## AI prompt
For each sink, trace the value to its origin: can it carry attacker-controlled content, and is it sanitized or escaped on every path? Report only with a concrete injection scenario. Evidence: the sink, the data's source, any sanitizer used.

## Why hybrid
The sink is syntactic, but taint depends on where the data comes from.

## Required tests
Dynamic innerHTML (finding), sanitized (no finding), literal (no finding), `eval(variable)` (finding), empty-string clear (no finding).
