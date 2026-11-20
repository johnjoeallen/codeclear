# Tests: typescript/express-async-handler-rejection

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an async handler with no try (Examples: Flag)

covers: example:flag,required:async-handler

```typescript path=src/routes.ts
import express from 'express';
const app = express();
app.get('/users', async (req, res) => {  // CANDIDATE
  const users = await db.users();
  res.json(users);
});
```

## Case: try/catch and a wrapper (Examples: OK)

covers: example:ok,do-not-flag:try-or-wrapper,required:try-catch

```typescript path=src/routes.ts
import express from 'express';
const app = express();
app.get('/users', async (req, res, next) => {
  try {
    res.json(await db.users());
  } catch (e) {
    next(e);
  }
});
```

```typescript path=src/wrapped.ts
import express from 'express';
import asyncHandler from 'express-async-handler';
const router = express.Router();
router.get('/users', asyncHandler(async (req, res) => {
  res.json(await db.users());
}));
```

## Case: post, use and function expressions

covers: detect:registrations

```typescript path=src/routes.ts
import express from 'express';
router.post('/x', async function (req, res) {  // CANDIDATE
  await save(req.body);
  res.sendStatus(204);
});
app.use(async (req, res, next) => {  // CANDIDATE
  await auth(req);
  next();
});
```

## Case: handlers that do not await, and sync handlers

covers: detect:no-await

```typescript path=src/routes.ts
import express from 'express';
app.get('/a', async (req, res) => { res.send('ok'); });
app.get('/b', (req, res) => { res.send('ok'); });
```

## Case: frameworks that handle rejections (Do not flag)

covers: do-not-flag:other-frameworks

```typescript path=src/fastify.ts
import Fastify from 'fastify';
const app = Fastify();
app.get('/users', async (req, res) => {
  return await db.users();
});
```
