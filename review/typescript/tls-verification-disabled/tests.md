# Tests: typescript/tls-verification-disabled

A `// FLAG` marker ends each line that must produce a finding.

## Case: rejectUnauthorized false (Examples: Flag)

covers: example:flag,required:reject-unauthorized

```typescript path=src/client.ts
const agent = new https.Agent({ rejectUnauthorized: false });  // FLAG
```

## Case: verification left on (Examples: OK)

covers: example:ok,required:enabled

```typescript path=src/client.ts
const agent = new https.Agent({ rejectUnauthorized: true, ca });
```

## Case: the environment variable and the other options

covers: detect:options

```typescript path=src/client.ts
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';  // FLAG
process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";  // FLAG
const o = {
  strictSSL: false,  // FLAG
  checkServerIdentity: () => undefined,  // FLAG
  "rejectUnauthorized": false,  // FLAG
};
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '1';
```

## Case: test files and fixtures (Do not flag)

covers: do-not-flag:test-files,required:test-file

```typescript path=src/client.spec.ts
const agent = new https.Agent({ rejectUnauthorized: false });
```

```typescript path=test/fixtures/server.ts
const agent = new https.Agent({ rejectUnauthorized: false });
```

## Case: bound to NODE_ENV development (Do not flag)

covers: do-not-flag:development-only

```typescript path=src/client.ts
if (process.env.NODE_ENV === 'development') {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}
```

## Case: a production branch is still reported

covers: detect:production-branch

```typescript path=src/client.ts
if (process.env.NODE_ENV === 'production') {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';  // FLAG
}
```
