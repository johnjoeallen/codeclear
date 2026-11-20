# Tests: typescript/non-constant-time-secret-compare

A `// FLAG` marker ends each line that must produce a finding.

## Case: a header compared with the expected signature (Examples: Flag)

covers: example:flag,required:signature

```typescript path=src/hook.ts
if (req.headers['x-signature'] === expectedSignature) {  // FLAG
}
```

## Case: timingSafeEqual (Examples: OK)

covers: example:ok,do-not-flag:timing-safe,required:timing-safe

```typescript path=src/hook.ts
crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
await bcrypt.compare(password, hash);
```

## Case: the other names, operators and a computed HMAC

covers: detect:names

```typescript path=src/hook.ts
if (apiKey !== provided) {}  // FLAG
if (digest !== crypto.createHmac('sha256', k).update(b).digest('hex')) {}  // FLAG
if (token == other) {}  // FLAG
if (sig === computed) {}  // FLAG
if (user.hash != expected) {}  // FLAG
```

## Case: comparisons to null, undefined, empty string and literals (Do not flag)

covers: do-not-flag:null-empty,required:null

```typescript path=src/hook.ts
if (token === undefined) {}
if (token === null) {}
if (token === '') {}
if (secret !== 'x') {}
if (token === 0) {}
```

## Case: equality of non-secret identifiers (Do not flag)

covers: do-not-flag:non-secret

```typescript path=src/hook.ts
if (tokenId === otherId) {}
if (userId === req.params.id) {}
if (hashing === true) {}
if (a === b) {}
```

## Case: a test file

covers: detect:test-file

```typescript path=src/hook.test.ts
if (token === expected) {}
```
