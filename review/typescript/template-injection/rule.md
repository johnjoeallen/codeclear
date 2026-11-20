# typescript/template-injection: Untrusted input compiled as a template or into a code string

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
Compiling user-controlled text with a server-side template engine (`ejs.render`, `pug.compile`, `handlebars.compile`, `lodash.template`) or `vm`/`new Function` executes attacker expressions. (Browser `eval`: `unsafe-html-sink`.)

## Detect
`ejs.render(x)`, `pug.render/compile(x)`, `Handlebars.compile(x)`, `_.template(x)`, `nunjucks.renderString(x)`, `vm.runInNewContext(x)`, `vm.Script(x)` where the template/code argument (not the data) is non-literal; also `res.render(name)` where `name` comes from the request.

## Do not flag
- Literal templates with user data passed as context, template names from an allow-list.

## Examples
Flag:
```ts
ejs.render(req.body.template, data);
```
OK:
```ts
ejs.render(fs.readFileSync('t.ejs', 'utf8'), { name: req.body.name });
```

## Implementation notes
Anchor to the compile/render call.

Detected on `ejs.render`, `pug.render/compile`, `Handlebars.compile`, `_.template`, `nunjucks.renderString`, `mustache.render`, `vm.runIn*Context` and `new vm.Script` whose first argument is not a literal, and on `res.render(name)` where the name is derived from `req` or `request` and the function mentions no allow-list.

## Required tests
Request-derived template string (finding), literal template with data (no finding), `res.render(req.query.view)` (finding), allow-listed view (no finding).

## AI prompt
"Is the template or code string attacker-controlled, and is the engine sandboxed in a way that actually prevents code execution?" Evidence: call, origin of the argument.

## Why hybrid
Sink matching is local; attacker control of the string needs data flow.
