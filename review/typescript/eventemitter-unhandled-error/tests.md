# Tests: typescript/eventemitter-unhandled-error

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a stream with a data listener and no error listener (Examples: Flag)

covers: example:flag,required:no-error-listener

```typescript path=src/read.ts
import fs from 'fs';
const stream = fs.createReadStream('big.log');
stream.on('data', (c) => use(c));  // CANDIDATE
```

## Case: an error listener, and pipeline (Examples: OK)

covers: example:ok,do-not-flag:error-listener-or-pipeline,required:error-listener

```typescript path=src/read.ts
import fs from 'fs';
const a = fs.createReadStream('a.log');
a.on('data', (c) => use(c));
a.on('error', (e) => log(e));
```

```typescript path=src/piped.ts
import fs from 'fs';
import { pipeline } from 'stream';
const b = fs.createReadStream('b.log');
b.on('data', (c) => use(c));
pipeline(b, out, (err) => log(err));
```

## Case: sockets, requests and emitters

covers: detect:emitters

```typescript path=src/net.ts
import net from 'net';
import http from 'http';
import { EventEmitter } from 'events';
const sock = net.connect(80, 'h');
sock.on('connect', () => go());  // CANDIDATE
const req = http.request(opts);
req.on('response', (r) => use(r));  // CANDIDATE
const bus = new EventEmitter();
bus.on('message', (m) => use(m));  // CANDIDATE
```

## Case: an object that is not an emitter, and events that are not data events

covers: detect:other-objects

```typescript path=src/other.ts
const el = document.querySelector('x');
el.on('data', () => go());
const s = fs.createReadStream('x');
s.on('custom', () => go());
```

## Case: libraries that register their own error handler are left to the AI (Do not flag)

covers: do-not-flag:library-handler

```typescript path=src/lib.ts
const bus = new EventEmitter();
bus.on('message', (m) => use(m));  // CANDIDATE
```
