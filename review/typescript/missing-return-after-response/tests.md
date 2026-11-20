# Tests: typescript/missing-return-after-response

A `// FLAG` marker ends each line that must produce a finding.

## Case: a response that is not returned (Examples: Flag)

covers: example:flag,required:no-return

```typescript path=src/handler.ts
async function handler(req, res) {
  if (!req.body.id) {
    res.status(400).json({ error: 'id required' });  // FLAG
  }
  const row = await db.find(req.body.id);
  res.json(row);
}
```

## Case: the branch returns (Examples: OK)

covers: example:ok,do-not-flag:returns,required:returns

```typescript path=src/handler.ts
async function handler(req, res) {
  if (!req.body.id) {
    return res.status(400).json({ error: 'id required' });
  }
  if (!ok) {
    res.sendStatus(403);
    return;
  }
  const row = await db.find(req.body.id);
  res.json(row);
}
```

## Case: next(err), redirect and a single-statement branch

covers: detect:responses

```typescript path=src/handler.ts
function mw(req, res, next) {
  if (!req.user) next(new Error('no user'));  // FLAG
  if (req.old) res.redirect('/new');  // FLAG
  res.send('ok');
}
```

## Case: the final statement of the handler, and an if/else (Do not flag)

covers: do-not-flag:final-statement,required:final

```typescript path=src/handler.ts
function handler(req, res) {
  work();
  if (!req.ok) {
    res.status(400).end();
  }
}
function other(req, res) {
  if (a) {
    res.send('a');
  } else {
    res.send('b');
  }
}
```

## Case: a function that is not a handler

covers: detect:handler-only

```typescript path=src/handler.ts
async function helper(x) {
  if (x) {
    log.send('x');
  }
  await save(x);
}
```

## Case: an arrow handler

covers: detect:arrow

```typescript path=src/handler.ts
const h = async (req, res) => {
  if (bad) {
    res.status(500).send('bad');  // FLAG
  }
  await audit(req);
};
```
