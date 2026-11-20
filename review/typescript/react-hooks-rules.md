# typescript/react-hooks-rules: Conditional hooks and wrong dependency arrays

| Field | Value |
|---|---|
| Applies to | `**/*.tsx`, `**/*.jsx`, `**/*.ts`, `**/*.js` |
| Category | CORRECTNESS |
| Severity | HIGH for conditional hooks, MEDIUM for dependencies |
| Confidence | HIGH |
| Detection type | linter |
| Source | typescript-review: React |

## Intent
Hooks must run in the same order on every render. Wrong dependency arrays cause stale values or render loops.

## Detect
- A `use*` hook call inside an `if`, loop, nested function, or after an early `return`.
- `useEffect`, `useMemo`, `useCallback` whose callback reads a reactive value that is missing from the dependency array.

## Do not flag
- Custom hooks that call hooks unconditionally.
- Refs and state setters, which are stable.

## Examples
Flag:
```tsx
if (id) { useEffect(load, []); }
```
OK:
```tsx
useEffect(() => { if (id) load(id); }, [id]);
```

## Implementation notes
Do not write a new detector. Emit ESLint configuration: `react-hooks/rules-of-hooks: error` and
`react-hooks/exhaustive-deps: warn`, and map their messages to findings.

## Required tests
Hook in `if` (finding), missing dependency (finding), complete dependencies (no finding).
