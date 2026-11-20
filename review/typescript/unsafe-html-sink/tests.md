# Tests: typescript/unsafe-html-sink

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: innerHTML from a variable (Examples: Flag)

covers: example:flag,required:inner-html

```typescript path=src/ui.ts
export function show(el: HTMLElement, text: string) {
  el.innerHTML = text;  // CANDIDATE
}
```

## Case: literals, empty strings and sanitized values (Examples: OK)

covers: example:ok,do-not-flag:literal-and-sanitized,do-not-flag:empty-string,required:sanitized

```typescript path=src/ui.ts
export function a(el: HTMLElement, text: string) {
  el.innerHTML = '<b>bold</b>';
  el.innerHTML = '';
  el.innerHTML = DOMPurify.sanitize(text);
  el.textContent = text;
}
```

## Case: the other sinks

covers: detect:sinks

```typescript path=src/ui.ts
export function a(el: HTMLElement, html: string, code: string) {
  el.outerHTML = html;  // CANDIDATE
  el.insertAdjacentHTML('beforeend', html);  // CANDIDATE
  document.write(html);  // CANDIDATE
  eval(code);  // CANDIDATE
  new Function(code);  // CANDIDATE
  setTimeout('doIt(' + code + ')', 10);  // CANDIDATE
  setTimeout(() => doIt(), 10);
}
```

## Case: dangerouslySetInnerHTML

covers: detect:jsx

```tsx path=src/Card.tsx
export const A = ({ html }) => <div dangerouslySetInnerHTML={{ __html: html }} />;  // CANDIDATE
export const B = () => <div dangerouslySetInnerHTML={{ __html: '<b>x</b>' }} />;
export const C = ({ html }) => <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }} />;
```

## Case: a test file

covers: detect:test-file

```typescript path=src/ui.test.ts
export function show(el: HTMLElement, text: string) {
  el.innerHTML = text;
}
```
