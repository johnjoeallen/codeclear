# Tests: typescript/effect-without-cleanup

A `// FLAG` marker ends each line that must produce a finding.

## Case: setInterval with no cleanup (Examples: Flag)

covers: example:flag,required:interval

```typescript path=src/Poll.ts
useEffect(() => { setInterval(poll, 1000); }, []);  // FLAG
```

## Case: cleared in the returned function (Examples: OK)

covers: example:ok,required:cleared

```typescript path=src/Poll.ts
useEffect(() => { const id = setInterval(poll, 1000); return () => clearInterval(id); }, []);
```

## Case: listeners, subscriptions and connections

covers: detect:registrations,required:listener

```typescript path=src/Poll.ts
useEffect(() => { window.addEventListener('resize', onResize); }, []);  // FLAG
useEffect(() => { store.subscribe(render); }, []);  // FLAG
useLayoutEffect(() => { const ws = new WebSocket(url); ws.onmessage = h; }, []);  // FLAG
useEffect(() => { const src = new EventSource(url); }, []);  // FLAG
```

## Case: the matching release is returned

covers: detect:released

```typescript path=src/Poll.ts
useEffect(() => {
  window.addEventListener('resize', onResize);
  return () => window.removeEventListener('resize', onResize);
}, []);
useEffect(() => {
  const sub = store.subscribe(render);
  return () => sub.unsubscribe();
}, []);
useEffect(() => {
  const ws = new WebSocket(url);
  return () => ws.close();
}, []);
useEffect(() => {
  const t = setTimeout(go, 100);
  return () => clearTimeout(t);
}, []);
```

## Case: a one-shot setTimeout that only sets state (Do not flag)

covers: do-not-flag:one-shot-state,required:set-state

```typescript path=src/Poll.ts
useEffect(() => { setTimeout(() => setVisible(true), 100); }, []);
useEffect(() => { setTimeout(go, 100); }, []);
```

## Case: a listener on an object created inside the effect (Do not flag)

covers: do-not-flag:local-object

```typescript path=src/Poll.ts
useEffect(() => {
  const el = new Image();
  el.addEventListener('load', onLoad);
}, []);
```

## Case: a subscription that returns its own unsubscribe (Do not flag)

covers: do-not-flag:returns-unsubscribe

```typescript path=src/Poll.ts
useEffect(() => {
  const unsubscribe = store.subscribe(render);
  return unsubscribe;
}, []);
useEffect(() => {
  return store.subscribe(render);
}, []);
```

## Case: an effect with no registrations

covers: detect:none

```typescript path=src/Poll.ts
useEffect(() => { document.title = title; }, [title]);
```
