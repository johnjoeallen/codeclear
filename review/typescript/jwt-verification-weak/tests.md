# Tests: typescript/jwt-verification-weak

A `// FLAG` marker ends each line that must produce a finding.

## Case: a decoded token used as the user (Examples: Flag)

covers: example:flag,required:decode

```typescript path=src/auth.ts
function mw(req, res, next) {
  req.user = jwt.decode(token);  // FLAG
  next();
}
```

## Case: verify with pinned options (Examples: OK)

covers: example:ok,do-not-flag:pinned,required:pinned

```typescript path=src/auth.ts
jwt.verify(token, key, { algorithms: ['RS256'], issuer: ISS, audience: AUD });
```

## Case: a decoded token that flows into an if, a return or the request

covers: detect:decode-flow

```typescript path=src/auth.ts
function a(req) {
  const claims = jwt.decode(token);  // FLAG
  if (claims.admin) { grant(); }
}
function b(req) {
  const c = jwt.decode(token);  // FLAG
  req.user = c;
}
function d() {
  return jsonwebtoken.decode(token);  // FLAG
}
```

## Case: a decode that is only shown or logged (Do not flag)

covers: do-not-flag:display-only,required:display

```typescript path=src/auth.ts
function show(token) {
  const claims = jwt.decode(token);
  console.log(claims);
  const unused = jwt.decode(token);
}
```

## Case: ignoreExpiration, the none algorithm, and no algorithms

covers: detect:verify

```typescript path=src/auth.ts
jwt.verify(token, key, { algorithms: ['RS256'], ignoreExpiration: true });  // FLAG
jwt.verify(token, key, { algorithms: ['HS256', 'none'] });  // FLAG
jwt.verify(token, key);  // FLAG
jwt.verify(token, key, { issuer: ISS });  // FLAG
jwt.verify(token, key, opts);
jwt.verify(token, key, { ...base });
```

## Case: jose with and without issuer and audience

covers: detect:jose

```typescript path=src/auth.ts
const { payload } = await jwtVerify(token, key);  // FLAG
if (payload.iss !== ISS) {}
await jose.jwtVerify(token, key, { issuer: ISS, audience: AUD });
```

## Case: jose in a file that never reads iss or aud (Do not flag)

covers: do-not-flag:no-claims-read

```typescript path=src/auth2.ts
await jwtVerify(token, key);
```

## Case: a test file

covers: detect:test-file

```typescript path=src/auth.test.ts
jwt.verify(token, key);
```
