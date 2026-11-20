# typescript/effect-without-cleanup: useEffect starts a timer, subscription or listener without cleanup

| Field | Value |
|---|---|
| Applies to | `**/*.tsx`, `**/*.jsx`, `**/*.ts`, `**/*.js` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ast |
| Source | typescript-review: React |

## Intent
An effect that registers a resource and never releases it leaks the resource and duplicates work on every re-run or remount.

## Detect
A `useEffect` / `useLayoutEffect` callback that calls `setInterval`, `setTimeout`, `addEventListener`, `.subscribe(`, `new WebSocket(` or `new EventSource(` and does not return a function that calls the matching `clearInterval`, `clearTimeout`, `removeEventListener`, `unsubscribe`, `close`.

## Do not flag
- A one-shot `setTimeout` whose callback only sets state; a state update after unmount alone is not a finding.
- Listeners on objects created inside the effect and discarded with it.
- Subscriptions that return their own unsubscribe which the effect returns.

## Examples
Flag:
```tsx
useEffect(() => { setInterval(poll, 1000); }, []);
```
OK:
```tsx
useEffect(() => { const id = setInterval(poll, 1000); return () => clearInterval(id); }, []);
```

## Implementation notes
Anchor to the registration call. No standard ESLint rule covers all cases; use AST.

One finding per `useEffect`/`useLayoutEffect`, anchored to the effect call and not to each registration. The effect callback is searched for `setInterval`, `setTimeout`, `addEventListener`, `.subscribe`, `new WebSocket` and `new EventSource`; the text of its top-level `return` must mention the matching release (`clearInterval`, `clearTimeout`, `removeEventListener`, `unsubscribe`, `close`). A `setTimeout` whose callback only calls `setX(...)`, a listener on an object created inside the effect, and a returned name or member (the subscription's own unsubscribe) are not flagged.

## Required tests
Interval without cleanup (finding), interval with cleanup (no finding), listener without remove (finding), returned unsubscribe (no finding).

## Why deterministic
Registration calls and the presence of a matching release in the returned function are syntactic facts.
