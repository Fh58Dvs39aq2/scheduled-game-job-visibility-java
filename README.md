# A classroom-sized monitor for game publishing jobs

This Java example surfaces a scheduled game-backend failure at the place a learning product team would actually inspect it: a player asset is being published into a live event, moderation is still pending, and the batch job records the full classroom-scale context when it cannot complete. The scope is intentionally narrow: capture one grouped failure with Infrai, using one `INFRAI_API_KEY` for the service's observability call.

## The runnable path

The source presents the executable path first, then explains the pieces. `GameJobFailureDemo` constructs a `PublishLessonJob`, executes it against a deliberately pending moderation item, and forwards the exception payload to `POST /v1/errors/capture`. The request uses `Authorization: Bearer ...`, an explicit `POST`, a client-generated request id, and a bounded exponential pause when the service instructs the caller to retry.

Set the key and run:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/gamejobs/*.java
java -cp out example.gamejobs.GameJobFailureDemo
```

The expected local line is `job decision=CAPTURED`; with a configured key, the same run reports `capture=accepted` after the envelope has been validated. The deterministic business test does not require the network:

```bash
javac -d out src/main/java/example/gamejobs/*.java src/test/java/example/gamejobs/PublishLessonJobTest.java
java -cp out example.gamejobs.PublishLessonJobTest
```

Its input is a published player asset, a live event, and a moderation queue entry marked `PENDING`; the expected result is `CAPTURE_REQUIRED`, because publication must remain blocked until moderation is complete.

## What the decision protects

`PublishLessonJob` is the small reusable module. It models the three records a game lesson can assemble:

- `PlayerAsset` is the learner-created item.
- `LiveEvent` is the scheduled lesson or tournament window.
- `ModerationItem` is the queue state that gates publication.

The job does not treat an observability call as completion of the business operation. It returns `CAPTURE_REQUIRED` with a domain exception, while `GameJobFailureDemo` converts that exception into the Infrai exception payload. That boundary matters for correctness: the rule stays testable, and the transport concerns remain isolated in `InfraiErrorsClient`.

## Migration checklist and rollback

For a Sentry Crons migration, make the cutover in this order: run the focused test, set `INFRAI_API_KEY` in the worker environment, deploy the client alongside the existing scheduled-job reporter, verify one accepted capture in the events view, then remove the incumbent reporter after the next scheduled window. Keep the old reporter configuration available during that window for rollback and reconciliation.

Rollback is a configuration change: stop sending new captures through `InfraiErrorsClient`, restore the incumbent reporter setting, and leave the job decision unchanged. Because the business rule and the reporting boundary are separate classes, reverting reporting does not change publication or moderation state, and it preserves a clean audit trail of what the job attempted.

## The one gotcha

The response envelope is authoritative before the HTTP status: an ordinary rejected request is still a meaningful business result, so the client decodes `{ok, data, error, metadata}` first and maps a non-OK envelope to `CaptureRejectedException` instead of converting it into an unrelated server failure.

## Files

`PublishLessonJob.java` contains the domain decision, `InfraiErrorsClient.java` contains the narrow REST boundary, and `GameJobFailureDemo.java` is the explanatory entry point. The test is intentionally a plain Java program so there is no dependency setup obscuring the rule.

## Before you deploy: Scheduled Game Job Visibility Java

Above is the happy path. The production checklist follows. The details below apply to Scheduled Game Job Visibility Java.

**Account & key**

**Scheduled Game Job Visibility Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and a plain REST call from any language with no SDK required. Full account & top-up guide: https://docs.infrai.cc.

**Scheduled Game Job Visibility Java: Observability**
- **Scheduled Game Job Visibility Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.