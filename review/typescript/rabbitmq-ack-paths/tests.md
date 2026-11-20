# Tests: typescript/rabbitmq-ack-paths

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a consumer that never acknowledges (Examples: Flag)

covers: example:flag,required:no-ack-call

```typescript path=src/consumer.ts
ch.consume('jobs', (msg) => {  // CANDIDATE
  process(msg);
});
```

## Case: try/catch/finally that settles the delivery (Examples: OK)

covers: example:ok,do-not-flag:always-settled,required:settled

```typescript path=src/consumer.ts
ch.consume('jobs', async (msg) => {
  try {
    await work(msg);
    ch.ack(msg);
  } catch (e) {
    ch.nack(msg, false, false);
  }
});
```

## Case: await with no catch that settles

covers: detect:throw-path

```typescript path=src/consumer.ts
ch.consume('jobs', async (msg) => {  // CANDIDATE
  await work(msg);
  ch.ack(msg);
});
```

## Case: ack before the work is done

covers: detect:early-ack

```typescript path=src/consumer.ts
ch.consume('jobs', async (msg) => {  // CANDIDATE
  ch.ack(msg);
  await work(msg);
});
```

## Case: noAck (Do not flag)

covers: do-not-flag:no-ack

```typescript path=src/consumer.ts
ch.consume('metrics', (msg) => {
  record(msg);
}, { noAck: true });
```
