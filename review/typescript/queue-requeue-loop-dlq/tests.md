# Tests: typescript/queue-requeue-loop-dlq

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: nack with requeue in a catch (Examples: Flag)

covers: example:flag,required:requeue

```typescript path=src/consumer.ts
ch.consume('jobs', async (msg) => {
  try { await work(msg); ch.ack(msg); } catch (e) {
    ch.nack(msg, false, true);  // CANDIDATE
  }
});
```

## Case: a retry counter, and a dead-letter exchange (Examples: OK)

covers: example:ok,do-not-flag:bounded-requeue,do-not-flag:dlx-declared,required:counter

```typescript path=src/bounded.ts
ch.consume('jobs', async (msg) => {
  try { await work(msg); ch.ack(msg); } catch (e) {
    const attempts = (msg.properties.headers['x-death'] || []).length;
    if (attempts < 3) ch.nack(msg, false, true); else ch.nack(msg, false, false);
  }
});
```

```typescript path=src/queues.ts
await ch.assertQueue('jobs', { durable: true, deadLetterExchange: 'dlx' });
ch.nack(msg, false, false);
```

## Case: reject and requeue: true

covers: detect:forms

```typescript path=src/consumer.ts
ch.consume('jobs', (msg) => {
  try { work(msg); } catch (e) {
    ch.reject(msg, true);  // CANDIDATE
  }
});
```

## Case: a queue with no dead-letter exchange in a file that nacks

covers: detect:assert-queue

```typescript path=src/setup.ts
await ch.assertQueue('jobs', { durable: true });  // CANDIDATE
ch.nack(msg, false, false);
```

## Case: requeue outside a catch

covers: detect:not-in-catch

```typescript path=src/consumer.ts
ch.consume('jobs', (msg) => {
  if (!ready) ch.nack(msg, false, true);
});
```
