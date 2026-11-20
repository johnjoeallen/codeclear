# Tests: typescript/token-in-web-storage

A `// FLAG` marker ends each line that must produce a finding.

## Case: a token in localStorage (Examples: Flag)

covers: example:flag,required:token

```typescript path=src/auth.ts
localStorage.setItem('accessToken', token);  // FLAG
```

## Case: a harmless key (Examples: OK)

covers: example:ok,do-not-flag:non-sensitive-key,required:theme

```typescript path=src/auth.ts
localStorage.setItem('theme', 'dark');
localStorage.setItem('locale', 'en');
localStorage.setItem('authorName', name);
localStorage.setItem('tokenizer', 'bpe');
```

## Case: the other storages, spellings and assignments

covers: detect:forms

```typescript path=src/auth.ts
sessionStorage.setItem('jwt', t);  // FLAG
window.localStorage.setItem('x-auth-token', t);  // FLAG
localStorage.setItem('API_KEY', k);  // FLAG
localStorage.setItem('user_password', p);  // FLAG
localStorage.token = t;  // FLAG
localStorage['sessionId'] = s;  // FLAG
sessionStorage.credentials = c;  // FLAG
cache.setItem('token', t);
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```typescript path=src/auth.test.ts
localStorage.setItem('token', 't');
```
