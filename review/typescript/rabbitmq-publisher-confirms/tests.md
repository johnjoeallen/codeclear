# Tests: typescript/rabbitmq-publisher-confirms

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a plain channel used to publish (Examples: Flag)

covers: example:flag,required:plain-channel

```typescript path=src/publish.ts
const conn = await amqp.connect(URL);
const ch = await conn.createChannel();  // CANDIDATE
ch.publish('ex', 'key', Buffer.from('x'));
```

## Case: a confirm channel (Examples: OK)

covers: example:ok,do-not-flag:shared-channel,required:confirm-channel

```typescript path=src/publish.ts
const conn = await amqp.connect(URL);
const ch = await conn.createConfirmChannel();
ch.publish('ex', 'key', Buffer.from('x'), {}, (err) => { if (err) throw err; });
```

## Case: publish without persistent to a durable queue

covers: detect:persistent

```typescript path=src/publish.ts
await ch.assertQueue('jobs', { durable: true });
ch.sendToQueue('jobs', Buffer.from('x'));  // CANDIDATE
ch.sendToQueue('jobs', Buffer.from('x'), { persistent: true });
```

## Case: a connection per call that is never closed

covers: detect:per-call

```typescript path=src/send.ts
export async function send(m) {
  const conn = await amqp.connect(URL);  // CANDIDATE
  const ch = await conn.createChannel();  // CANDIDATE
  ch.sendToQueue('q', Buffer.from(m), { persistent: true });
}
export async function sendClosed(m) {
  const conn = await amqp.connect(URL);
  const ch = await conn.createConfirmChannel();
  ch.sendToQueue('q', Buffer.from(m), { persistent: true });
  await ch.close();
  await conn.close();
}
```

## Case: telemetry and long-lived setup (Do not flag)

covers: do-not-flag:telemetry-and-setup

```typescript path=src/telemetry.ts
// transient telemetry: losing a message is fine
const conn = await amqp.connect(URL);
const ch = await conn.createChannel();
ch.publish('metrics', 'k', Buffer.from('x'));
```
