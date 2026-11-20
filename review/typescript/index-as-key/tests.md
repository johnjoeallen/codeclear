# Tests: typescript/index-as-key

A `// FLAG` marker ends each line that must produce a finding.

## Case: the index as the key (Examples: Flag)

covers: example:flag,required:index

```tsx path=src/List.tsx
export const List = ({ items }) => (
  <ul>
    {items.map((item, i) => <li key={i}>{item.name}</li>)}  // FLAG
  </ul>
);
```

## Case: a stable id (Examples: OK)

covers: example:ok,required:id

```tsx path=src/List.tsx
export const List = ({ items }) => (
  <ul>{items.map((item, i) => <li key={item.id}>{item.name}</li>)}</ul>
);
```

## Case: templates built from the index, and a function expression

covers: detect:built-from-index

```tsx path=src/List.tsx
const a = rows.map((r, idx) => <Row key={`row-${idx}`} />);  // FLAG
const b = rows.map(function (r, n) { return <Row key={"k" + n} />; });  // FLAG
const c = rows.map((r, i) => <Row key={`${r.id}-${i}`} />);  // FLAG
```

## Case: a static list (Do not flag)

covers: do-not-flag:static-list,required:static

```tsx path=src/List.tsx
const STEPS = ['one', 'two', 'three'];
const a = STEPS.map((s, i) => <li key={i}>{s}</li>);
const b = ['a', 'b'].map((s, i) => <li key={i}>{s}</li>);
```

## Case: a list that can change is not static

covers: detect:non-const

```tsx path=src/List.tsx
let steps = ['one', 'two'];
const a = steps.map((s, i) => <li key={i}>{s}</li>);  // FLAG
```

## Case: other attributes and other calls

covers: detect:key-only

```tsx path=src/List.tsx
const a = items.map((item, i) => <li id={i}>{item}</li>);
const b = items.map((item) => <li key={item}>{item}</li>);
const c = items.forEach((item, i) => <li key={i}>{item}</li>);
```

## Case: JavaScript with JSX

covers: detect:jsx

```jsx path=src/List.jsx
const a = items.map((item, i) => <li key={i}>{item}</li>);  // FLAG
```
