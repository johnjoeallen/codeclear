# Tests: typescript/loose-equality

A `// FLAG` marker ends each line that must produce a finding.

## Case: == between two variables (Examples: Flag)

covers: example:flag,required:loose

```typescript path=src/a.ts
if (a == b) {  // FLAG
}
```

## Case: === (Examples: OK)

covers: example:ok,required:strict

```typescript path=src/a.ts
if (a === b || a !== c) {
}
```

## Case: != and mixed types

covers: detect:not-equal

```typescript path=src/a.ts
const x = a != b;  // FLAG
const y = n == "1";  // FLAG
```

## Case: comparison with null (Do not flag)

covers: do-not-flag:null,required:null

```typescript path=src/a.ts
if (x == null || y != null) {
}
```

## Case: operands of the same primitive type (Do not flag)

covers: do-not-flag:same-type

```typescript path=src/a.ts
const a = typeof v == "string";
const b = "a" == "b";
const c = 1 != 2;
```

## Case: JavaScript files too

covers: detect:javascript

```javascript path=src/a.js
if (a == b) {  // FLAG
}
```
