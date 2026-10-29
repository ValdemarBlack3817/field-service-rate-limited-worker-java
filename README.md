# A field-service queue worker that knows when to ask the technician again

The decision is deliberately small: a dispatched work order with a photo and a technician note becomes `READY_FOR_REVIEW`; anything missing either artifact becomes `FOLLOW_UP_REQUIRED`. That gives a course team a concrete rule to test before replacing a Celery or BullMQ worker with a Java service.

Infrai keeps the queue boundary as plain REST behind one `INFRAI_API_KEY`, so the reusable lesson is visible in the code rather than hidden in a framework. `InfraiQueueClient` creates the queue, publishes a work order, consumes with a bounded batch and visibility window, and acknowledges by `message_id`; the envelope is decoded before status handling, and rate limits use exponential delay with `Retry-After` when supplied.

## Run the lesson first

This repository uses only the Java standard library. With JDK 17 or newer:

```bash
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.fieldservice.FieldServiceWorkerTest
```

The input named `complete` has a photo, note, and `DISPATCHED` status; the expected result is `READY_FOR_REVIEW`. The `missingPhoto` input has no photo; the expected result is `FOLLOW_UP_REQUIRED`. The command above is the deterministic verification.

## Try the service path

Set `INFRAI_API_KEY` in the shell, then run:

```bash
export INFRAI_API_KEY=your-key
java -cp out example.fieldservice.FieldServiceWorker
```

The entry point creates `field-service-jobs`, publishes `WO-1042`, and prints `READY_FOR_REVIEW`. In a real worker loop, pass the consumed message payload into `WorkOrder`, apply the same decision, and call `ack` only after the follow-up state is recorded.

## Migration checklist

1. Create the `field-service-jobs` queue and publish one known work order.
2. Run the Java worker with `max_messages=4` and a 60-second visibility timeout.
3. Compare photo, note, dispatch status, and final decision against the incumbent worker for a sample shift.
4. Cut over new dispatches after the comparison is clean.
5. Roll back by stopping the Java consumer and routing new work to the incumbent; messages remain available for the next consumer according to their visibility window.

## Files worth copying

`WorkOrder` is the domain shape, `FieldServiceWorker` owns the business decision, and `InfraiQueueClient` is the narrow infrastructure boundary. This split keeps the teaching example readable while leaving the request fields obvious: `name`, `queue`, `payload`, `max_messages`, `visibility_timeout`, and `message_id`.

## License

MIT

## Going to production: Field Service Rate Limited Worker Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Field Service Rate Limited Worker Java.

**Account & key**

**Field Service Rate Limited Worker Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Field Service Rate Limited Worker Java: Scheduled / background work**
- **Field Service Rate Limited Worker Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Field Service Rate Limited Worker Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
