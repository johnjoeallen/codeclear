# Tests: typescript/template-injection

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a template from a variable (Examples: Flag)

covers: example:flag,required:template-source

```typescript path=src/views.ts
export function page(source: string) {
  return ejs.render(source);  // CANDIDATE
}
```

## Case: a literal template with data (Examples: OK)

covers: example:ok,do-not-flag:literal-template,required:literal

```typescript path=src/views.ts
export function page(name: string) {
  return ejs.render('<h1><%= name %></h1>', { name });
}
```

## Case: the other engines and vm

covers: detect:engines

```typescript path=src/views.ts
export function a(src: string) {
  pug.compile(src);  // CANDIDATE
  Handlebars.compile(src);  // CANDIDATE
  _.template(src);  // CANDIDATE
  nunjucks.renderString(src, {});  // CANDIDATE
  vm.runInNewContext(src);  // CANDIDATE
  new vm.Script(src);  // CANDIDATE
}
```

## Case: a template name from the request

covers: detect:render-name

```typescript path=src/views.ts
export function a(req, res) {
  res.render(req.query.view);  // CANDIDATE
}
export function b(req, res) {
  const allowed = ['home', 'about'];
  const view = req.query.view;
  if (!allowed.includes(view)) return res.sendStatus(404);
  res.render(view);
}
export function c(res) {
  res.render('home');
}
```
