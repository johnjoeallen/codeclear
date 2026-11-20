# Tests: typescript/mutating-props-or-state

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: props changed in place (Examples: Flag)

covers: example:flag,required:props

```tsx path=src/Card.tsx
export function Card(props) {
  props.item.done = true;  // CANDIDATE
  return <div>{props.item.name}</div>;
}
```

## Case: a copy (Examples: OK)

covers: example:ok,do-not-flag:fresh-copy,required:copy

```tsx path=src/Card.tsx
export function Card(props) {
  const [items, setItems] = useState([]);
  const sorted = [...items].sort();
  const other = items.slice().sort();
  const next = { ...props.item, done: true };
  return <div>{sorted.length}</div>;
}
```

## Case: state changed in place

covers: detect:state

```tsx path=src/List.tsx
export function List() {
  const [items, setItems] = useState([]);
  const add = (x) => {
    items.push(x);  // CANDIDATE
    setItems(items);
  };
  const edit = () => {
    items[0].done = true;  // CANDIDATE
  };
  return null;
}
```

## Case: a parameter that is changed and returned

covers: detect:parameter

```typescript path=src/util.ts
export function addTag(post, tag) {
  post.tags.push(tag);  // CANDIDATE
  return post;
}
export function localOnly(tag) {
  const post = { tags: [] };
  post.tags.push(tag);
  return post;
}
```

## Case: locals and refs (Do not flag)

covers: do-not-flag:locals-and-refs

```tsx path=src/Ref.tsx
export function Box() {
  const ref = useRef(null);
  ref.current = 5;
  const local = [];
  local.push(1);
  return null;
}
```

## Case: Immer and Redux Toolkit reducers (Do not flag)

covers: do-not-flag:immer

```typescript path=src/slice.ts
import { createSlice } from '@reduxjs/toolkit';
export const slice = createSlice({
  name: 'todos',
  initialState: [],
  reducers: {
    add(state, action) { state.push(action.payload); },
  },
});
```
