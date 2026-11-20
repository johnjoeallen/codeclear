# Tests: typescript/nosql-operator-injection

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: the request body as the filter (Examples: Flag)

covers: example:flag,required:body-filter

```typescript path=src/users.ts
import mongoose from 'mongoose';
export async function login(req, res) {
  const user = await User.findOne(req.body);  // CANDIDATE
  res.json(user);
}
```

## Case: values cast to strings (Examples: OK)

covers: example:ok,do-not-flag:cast,required:cast

```typescript path=src/users.ts
import mongoose from 'mongoose';
export async function login(req, res) {
  const user = await User.findOne({ name: String(req.body.name), password: String(req.body.password) });
  res.json(user);
}
```

## Case: properties, spreads and variables

covers: detect:forms

```typescript path=src/users.ts
import mongoose from 'mongoose';
export async function a(req) {
  await User.find({ name: req.query.name });  // CANDIDATE
  await User.updateOne({ ...req.body }, { $set: { x: 1 } });  // CANDIDATE
  const filter = req.body.filter;
  await User.deleteMany(filter);  // CANDIDATE
  await User.countDocuments(req.query);  // CANDIDATE
}
```

## Case: a schema or sanitizer (Do not flag)

covers: do-not-flag:schema-or-sanitizer

```typescript path=src/users.ts
import mongoose from 'mongoose';
export async function a(req) {
  const body = schema.parse(req.body);
  return User.findOne(body);
}
```

```typescript path=src/app.ts
import mongoSanitize from 'express-mongo-sanitize';
import mongoose from 'mongoose';
app.use(mongoSanitize());
export async function b(req) {
  return User.findOne(req.body);
}
```

## Case: a filter that does not come from the request, and a file without Mongo

covers: detect:other

```typescript path=src/users.ts
import mongoose from 'mongoose';
export async function a(id: string) {
  return User.findOne({ _id: id });
}
```

```typescript path=src/cache.ts
export async function b(req) {
  return cache.find(req.body);
}
```
