# typescript/index-as-key: Array index used as React list key

| Field | Value |
|---|---|
| Applies to | `**/*.tsx`, `**/*.jsx` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | typescript-review: React |

## Intent
Index keys make React reuse the wrong component state when items are reordered, inserted or removed.

## Detect
A JSX `key` attribute whose expression is the second parameter of the enclosing `map` callback, or a template or string built from it.

## Do not flag
- Lists that are static and never reorder: only when the array is a literal or a `const` defined in the file.

## Examples
Flag:
```tsx
items.map((item, i) => <Row key={i} item={item} />)
```
OK:
```tsx
items.map((item) => <Row key={item.id} item={item} />)
```

## Implementation notes
Equivalent: ESLint `react/no-array-index-key`.

## Required tests
`key={i}` (finding), `` key={`row-${i}`} `` (finding), `key={item.id}` (no finding), static literal array (no finding).
