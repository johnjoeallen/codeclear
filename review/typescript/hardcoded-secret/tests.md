# Tests: typescript/hardcoded-secret

A `// FLAG` marker ends each line that must produce a finding.

## Case: a live key (Examples: Flag)

covers: example:flag,required:live-key

```typescript path=src/pay.ts
const apiKey = 'sk_live_51H8abcDEF123456789';  // FLAG
```

## Case: from the environment (Examples: OK)

covers: example:ok,do-not-flag:env,required:env

```typescript path=src/pay.ts
const apiKey = process.env.STRIPE_KEY;
const password = config.get('db.password');
```

## Case: known formats in any string

covers: detect:formats

```typescript path=src/pay.ts
use('AKIAIOSFODNN7EXAMPL1');  // FLAG
use('ghp_abcdefghijklmnopqrstuvwxyz0123456789');  // FLAG
use('xoxb-1234-abcd');  // FLAG
use('-----BEGIN RSA PRIVATE KEY-----');  // FLAG
use('eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTYifQ.abc');  // FLAG
```

## Case: secret-named properties, assignments and fields

covers: detect:names,required:name-based

```typescript path=src/pay.ts
const o = { password: "abcdefgh12345" };  // FLAG
this.secret = `abcdefghij1234`;  // FLAG
config.clientSecret = 'Zx9aB3kLm0pQ7rS2';  // FLAG
class C { private token = 'a1b2c3d4e5f6g7h8'; }  // FLAG
let privateKey = "abc123def456ghi789";  // FLAG
```

## Case: placeholders, messages and short values (Do not flag)

covers: do-not-flag:placeholder,required:placeholder

```typescript path=src/pay.ts
const password = 'changeme123456';
const token = '<your-token-here>';
const apiKey = 'xxxxxxxxxxxx1';
const secret = 'example-secret-1234';
const msg = { password: 'Password is required' };
const short = { token: 'abc123' };
const empty = { secret: '' };
```

## Case: type names and enum values (Do not flag)

covers: do-not-flag:enum-and-types

```typescript path=src/pay.ts
enum Kind { Token = 'access_token_kind' }
type Auth = { token: string };
const route = { path: 'reset-password-link' };
```

## Case: a test file with fake values (Do not flag)

covers: do-not-flag:test-file

```typescript path=src/pay.spec.ts
const apiKey = 'sk_live_51H8abcDEF123456789';
```

## Case: a JSON file the rule also applies to

covers: detect:json

```json path=config/keys.json
{ "token": "a1b2c3d4e5f6g7h8" }
```
