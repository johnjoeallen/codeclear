# Tests: typescript/ssrf-and-open-redirect

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a URL from the request (Examples: Flag)

covers: example:flag,required:fetch-url

```typescript path=src/proxy.ts
export async function proxy(req, res) {
  const r = await fetch(req.query.url);  // CANDIDATE
  res.send(await r.text());
}
```

## Case: a constant URL and an allow-listed host (Examples: OK)

covers: example:ok,do-not-flag:allow-list,required:allow-list

```typescript path=src/proxy.ts
export async function a(req, res) {
  const u = new URL(req.query.url);
  if (!ALLOWED_HOSTS.includes(u.hostname)) return res.sendStatus(400);
  return fetch(u);
}
export async function b() {
  return fetch('https://api.acme.io/v1');
}
```

## Case: axios, got and through a variable

covers: detect:clients

```typescript path=src/proxy.ts
export async function a(req) {
  const target = req.body.target;
  await axios.get(target);  // CANDIDATE
  await got(req.query.u);  // CANDIDATE
  await http.request(req.query.host);  // CANDIDATE
}
```

## Case: an open redirect

covers: detect:redirect,required:redirect

```typescript path=src/login.ts
export function a(req, res) {
  res.redirect(req.query.next);  // CANDIDATE
}
```

## Case: relative-only redirects and user values only in encoded path segments (Do not flag)

covers: do-not-flag:relative-redirect

```typescript path=src/login.ts
export function a(req, res) {
  const next = req.query.next;
  if (!next.startsWith('/')) return res.redirect('/');
  res.redirect(next);
}
export async function b(req) {
  return fetch(`https://api.acme.io/users/${encodeURIComponent(req.params.id)}`);
}
```
