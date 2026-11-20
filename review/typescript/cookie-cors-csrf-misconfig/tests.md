# Tests: typescript/cookie-cors-csrf-misconfig

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a session cookie with no flags (Examples: Flag)

covers: example:flag,required:cookie

```typescript path=src/auth.ts
export function login(res, sid: string) {
  res.cookie('sessionId', sid);  // CANDIDATE
}
```

## Case: a cookie with all the flags, and a preference cookie (Examples: OK)

covers: example:ok,do-not-flag:non-session-cookie,required:flags

```typescript path=src/auth.ts
export function a(res, sid: string) {
  res.cookie('sessionId', sid, { httpOnly: true, secure: true, sameSite: 'lax' });
  res.cookie('theme', 'dark');
}
```

## Case: session middleware options

covers: detect:session-options

```typescript path=src/app.ts
app.use(session({ secret: s, cookie: { secure: false } }));  // CANDIDATE
app.use(session({ secret: s, cookie: { httpOnly: false, sameSite: 'lax' } }));  // CANDIDATE
app.use(session({ secret: s, cookie: { httpOnly: true, secure: true, sameSite: 'lax' } }));
```

## Case: CORS

covers: detect:cors

```typescript path=src/app.ts
app.use(cors({ origin: true }));  // CANDIDATE
app.use(cors({ origin: '*', credentials: true }));  // CANDIDATE
app.use(cors({ origin: req.headers.origin }));  // CANDIDATE
app.use(cors({ origin: 'https://app.acme.io', credentials: true }));
app.use(cors({ origin: '*' }));
```

## Case: trust proxy

covers: detect:trust-proxy

```typescript path=src/app.ts
app.set('trust proxy', true);  // CANDIDATE
app.set('trust proxy', 1);
```

## Case: a state-changing route with cookie sessions and no CSRF protection

covers: detect:csrf

```typescript path=src/routes.ts
import session from 'express-session';
router.post('/transfer', (req, res) => {  // CANDIDATE
  res.send('ok');
});
```

## Case: a bearer-token API, and development-only config (Do not flag)

covers: do-not-flag:bearer-and-dev

```typescript path=src/api.ts
router.post('/items', (req, res) => {
  const token = req.headers.Authorization;
  res.send('ok');
});
if (process.env.NODE_ENV === 'development') {
  app.use(cors({ origin: true }));
}
```
