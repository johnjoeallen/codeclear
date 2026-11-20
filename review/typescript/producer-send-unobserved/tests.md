# Tests: typescript/producer-send-unobserved

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a send that is not awaited (Examples: Flag)

covers: example:flag,required:not-awaited

```typescript path=src/events.ts
export async function publish(event) {
  producer.send({ topic: 'orders', messages: [{ value: JSON.stringify(event) }] });  // CANDIDATE
}
```

## Case: an awaited send in try/catch (Examples: OK)

covers: example:ok,do-not-flag:awaited,required:awaited

```typescript path=src/events.ts
export async function publish(event) {
  try {
    await producer.send({ topic: 'orders', messages: [{ value: JSON.stringify(event) }] });
  } catch (e) {
    logger.error(e);
    throw e;
  }
}
```

## Case: the other sends

covers: detect:senders

```typescript path=src/events.ts
export async function a(m) {
  channel.sendToQueue('q', Buffer.from(m));  // CANDIDATE
  channel.publish('ex', 'key', Buffer.from(m));  // CANDIDATE
  sqs.send(new SendMessageCommand(m));  // CANDIDATE
  sns.publish(m);  // CANDIDATE
}
```

## Case: explicit best-effort telemetry (Do not flag)

covers: do-not-flag:telemetry

```typescript path=src/metrics.ts
// best-effort metrics: losing one is fine
export function record(m) {
  producer.send({ topic: 'metrics', messages: [m] });
}
```

## Case: process.exit right after a send

covers: detect:exit

```typescript path=src/cli.ts
export function done(m) {
  producer.send({ topic: 'done', messages: [m] });  // CANDIDATE
  process.exit(0);  // CANDIDATE
}
```

## Case: other receivers

covers: detect:other-receivers

```typescript path=src/other.ts
export function a(socket, m) {
  socket.send(m);
  emitter.publish(m);
}
```
