# typescript/mutating-props-or-state: Props, state or shared objects mutated in place

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx`, `**/*.js`, `**/*.jsx` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | typescript-review: Correctness |

## Intent
In-place mutation of props, React state or shared objects skips re-rendering, breaks memoization and changes data other code still holds.

## Detect
- In a React component or hook: assignment to `props.x`, `props.x.y`, a state variable's property, or `state.push/splice/sort/reverse(...)`.
- Parameter mutation: assignment to a property of, or mutating array method on, a function parameter that is then returned or stored.

## Do not flag
- Mutation of locals created in the same function, including `useRef().current`.
- Immer `produce` drafts and Redux Toolkit reducers.
- `sort()` on a fresh copy (`[...a].sort()`).

## Examples
Flag:
```tsx
const [items, setItems] = useState<Item[]>([]);
items.push(x); setItems(items);
```
OK:
```tsx
setItems((prev) => [...prev, x]);
```

## Implementation notes
Equivalent: `react/no-direct-mutation-state`, `no-param-reassign` with `props: true`. Anchor to the mutation.

Detected on an assignment (or `+=`) to a property or index, and on `push`, `pop`, `shift`, `unshift`, `splice`, `sort`, `reverse`, `fill` or `copyWithin`, whose target chain starts at `props`; at a state variable declared in the same function with `useState`/`useReducer`; or at a parameter of the function when the function returns it. A file that mentions `immer`, `produce(`, `createSlice`, `createReducer` or `useImmer` is skipped. A copy (`[...a].sort()`, `a.slice().sort()`) has a call at its root, so it is never matched.

## Required tests
`state.push` then set (finding), spread copy (no finding), ref mutation (no finding), Immer draft (no finding).

## AI prompt
"Is this object owned by the caller, React or a shared cache rather than created locally? Does the mutation cause a missed render or visible side effect for another holder?" Supply: the function, where the object comes from, and its other users.

## Why hybrid
Detecting a mutation is syntactic; ownership of the object decides if it matters.
