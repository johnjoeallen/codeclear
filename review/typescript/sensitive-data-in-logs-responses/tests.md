# Tests: typescript/sensitive-data-in-logs-responses

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a token in a log call (Examples: Flag)

covers: example:flag,required:token

```typescript path=src/auth.ts
export function login(user: string, password: string) {
  logger.info('login', user, password);  // CANDIDATE
}
```

## Case: masked values and plain messages (Examples: OK)

covers: example:ok,do-not-flag:masked,required:masked

```typescript path=src/auth.ts
export function login(user: string, password: string, tokenCount: number) {
  logger.info('login', user, mask(password));
  logger.info('tokens', tokenCount);
  console.log('password reset requested');
}
```

## Case: whole requests and headers

covers: detect:whole-objects

```typescript path=src/auth.ts
export function a(req, user) {
  console.log(req);  // CANDIDATE
  console.log(req.headers);  // CANDIDATE
  logger.debug('body', req.body);  // CANDIDATE
  logger.info('user', user);  // CANDIDATE
  console.log(req.headers.authorization);  // CANDIDATE
}
```

## Case: a response with the stack or the raw error

covers: detect:responses

```typescript path=src/api.ts
export function a(req, res) {
  try { run(); } catch (err) {
    res.status(500).json({ error: err.message });  // CANDIDATE
    res.send(err.stack);  // CANDIDATE
    res.json({ err });  // CANDIDATE
  }
}
export function b(req, res) {
  try { run(); } catch (err) {
    logger.error('failed', err);
    res.status(500).json({ error: 'internal' });
  }
}
```

## Case: debug output behind a NODE_ENV check (Do not flag)

covers: do-not-flag:non-production-debug

```typescript path=src/api.ts
export function a(req, res) {
  try { run(); } catch (err) {
    if (process.env.NODE_ENV !== 'production') {
      res.status(500).json({ stack: err.stack });
    }
  }
}
```
