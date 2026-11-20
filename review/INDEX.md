# Rule index

Generated from each rule file's metadata. "AI call" is whether the tool calls the model for the rule at review time.

## Summary

| Folder | Deterministic | Hybrid | AI only | Total |
|---|---|---|---|---|
| common | 4 | 7 | 8 | 19 |
| config | 3 | 2 | 1 | 6 |
| java | 17 | 39 | 29 | 85 |
| python | 12 | 26 | 56 | 94 |
| sql | 3 | 3 | 3 | 9 |
| typescript | 20 | 37 | 50 | 107 |
| **All** | 59 | 114 | 147 | 320 |

## common

| Rule | Title | Type | AI call |
|---|---|---|---|
| [comment-restates-code](common/comment-restates-code/rule.md) | Comments that only repeat the code | hybrid | Candidates only |
| [commented-out-code](common/commented-out-code/rule.md) | Block of disabled code | hybrid | Candidates only |
| [duplicate-logging](common/duplicate-logging/rule.md) | Same failure logged at several layers | hybrid | Candidates only |
| [expensive-log-construction](common/expensive-log-construction/rule.md) | Costly log argument built when the level may be disabled | hybrid | Candidates only |
| [failure-path-without-diagnosis](common/failure-path-without-diagnosis/rule.md) | Error or fallback path logs nothing useful | ai | Always |
| [hardcoded-credential](common/hardcoded-credential/rule.md) | Credential or key in source code | regex | No |
| [leftover-debug-output](common/leftover-debug-output/rule.md) | Debug print left in production code | regex | No |
| [log-injection](common/log-injection/rule.md) | Untrusted input written verbatim to a log line | hybrid | Candidates only |
| [log-placeholder-mismatch](common/log-placeholder-mismatch/rule.md) | Log placeholders and arguments do not match | ast | No |
| [lost-error-detail](common/lost-error-detail/rule.md) | Unexpected failure logged without error object or stack trace | hybrid | Candidates only |
| [misleading-api-documentation](common/misleading-api-documentation/rule.md) | Public API doc omits or misstates a caller-relevant condition | ai | Always |
| [misleading-log-message](common/misleading-log-message/rule.md) | Log text contradicts the code | ai | Always |
| [missing-why-for-surprising-code](common/missing-why-for-surprising-code/rule.md) | Non-obvious code with no explanation | ai | Always |
| [sensitive-data-in-log](common/sensitive-data-in-log/rule.md) | Secret or personal data written to a log | hybrid | Candidates only |
| [stale-comment](common/stale-comment/rule.md) | Comment contradicts the code it describes | ai | Always |
| [unclear-or-contradictory-comment](common/unclear-or-contradictory-comment/rule.md) | Comment wording suggests the opposite of the code | ai | Always |
| [unowned-todo](common/unowned-todo/rule.md) | TODO, FIXME or HACK without a reference | regex | No |
| [unstructured-log-context](common/unstructured-log-context/rule.md) | Key facts in free text where structured fields are used | ai | Always |
| [wrong-log-level](common/wrong-log-level/rule.md) | Log level does not match the severity of the event | ai | Always |

## config

| Rule | Title | Type | AI call |
|---|---|---|---|
| [api-contract-breaking-change](config/api-contract-breaking-change/rule.md) | OpenAPI change that breaks existing clients | ast | No |
| [environment-specific-behaviour-change](config/environment-specific-behaviour-change/rule.md) | Config value changes behaviour between environments | ai | Always |
| [insecure-default](config/insecure-default/rule.md) | Setting that disables security or exposes internals | regex | No |
| [missing-required-setting](config/missing-required-setting/rule.md) | Config key with no matching consumer or value | hybrid | Candidates only |
| [secret-in-config](config/secret-in-config/rule.md) | Credential committed in a configuration file | regex | No |
| [secret-logged-by-config](config/secret-logged-by-config/rule.md) | Configuration that makes secrets appear in logs | hybrid | Candidates only |

## java

| Rule | Title | Type | AI call |
|---|---|---|---|
| [api-response-contract-break](java/api-response-contract-break/rule.md) | Changed API response breaks an existing consumer | ai | Always |
| [async-failure-unobserved](java/async-failure-unobserved/rule.md) | Async work whose failure nobody observes | hybrid | Candidates only |
| [aws-async-future-blocked](java/aws-async-future-blocked/rule.md) | Async AWS SDK future immediately blocked on | hybrid | Candidates only |
| [aws-client-not-reused](java/aws-client-not-reused/rule.md) | AWS client created per call or never closed | ast | No |
| [aws-hardcoded-credentials-or-region](java/aws-hardcoded-credentials-or-region/rule.md) | Hard-coded AWS credentials, region or endpoint | hybrid | Candidates only |
| [aws-missing-pagination](java/aws-missing-pagination/rule.md) | Single page used where all results are expected | hybrid | Candidates only |
| [aws-overbroad-iam-policy](java/aws-overbroad-iam-policy/rule.md) | IAM policy broader than the operations used | hybrid | Candidates only |
| [aws-presigned-url-and-secret-lookup](java/aws-presigned-url-and-secret-lookup/rule.md) | Presigned URL lifetime or per-call secret/config lookup | ai | Always |
| [bidirectional-relationship-inconsistent](java/bidirectional-relationship-inconsistent/rule.md) | One side of a bidirectional association updated | hybrid | Candidates only |
| [bigdecimal-misuse](java/bigdecimal-misuse/rule.md) | BigDecimal from double or compared with equals | ast | No |
| [blocking-in-synchronized-or-event-loop](java/blocking-in-synchronized-or-event-loop/rule.md) | Blocking call under a lock or on a non-blocking thread | hybrid | Candidates only |
| [broad-catch-swallowed](java/broad-catch-swallowed/rule.md) | Broad catch that continues as if successful | hybrid | Candidates only |
| [catch-log-and-continue](java/catch-log-and-continue/rule.md) | Catch that only logs when the caller needs the failure | hybrid | Candidates only |
| [check-then-act-on-shared-state](java/check-then-act-on-shared-state/rule.md) | Non-atomic check-then-act on shared map or lazy field | hybrid | Candidates only |
| [collection-modified-during-iteration](java/collection-modified-during-iteration/rule.md) | Collection mutated while iterating | ast | No |
| [completablefuture-blocking-or-unbounded-wait](java/completablefuture-blocking-or-unbounded-wait/rule.md) | CompletableFuture misuse (indefinite or blocking waits) | hybrid | Candidates only |
| [config-properties-without-validation](java/config-properties-without-validation/rule.md) | Configuration binding without validation | hybrid | Candidates only |
| [double-checked-locking-without-volatile](java/double-checked-locking-without-volatile/rule.md) | Double-checked locking on a non-volatile field | ast | No |
| [dto-mapping-drops-or-exposes-fields](java/dto-mapping-drops-or-exposes-fields/rule.md) | DTO/entity mapping loses or leaks fields | ai | Always |
| [dual-write-without-outbox](java/dual-write-without-outbox/rule.md) | Database write and broker/search publish not atomic | ai | Always |
| [empty-catch-block](java/empty-catch-block/rule.md) | Empty catch block | ast | No |
| [exception-cause-dropped](java/exception-cause-dropped/rule.md) | Exception wrapped without its cause | ast | No |
| [external-client-failure-handling](java/external-client-failure-handling/rule.md) | External client misreports failures | ai | Always |
| [external-payload-compatibility](java/external-payload-compatibility/rule.md) | External payload fields, enums and version skew | ai | Always |
| [floating-point-money](java/floating-point-money/rule.md) | double or float used for money | hybrid | Candidates only |
| [generic-exception-in-public-api](java/generic-exception-in-public-api/rule.md) | Generic exception thrown from a public API | hybrid | Candidates only |
| [hardcoded-secret-or-environment-url](java/hardcoded-secret-or-environment-url/rule.md) | Secret or environment-specific URL in code | hybrid | Candidates only |
| [inconsistent-business-invariant](java/inconsistent-business-invariant/rule.md) | Business rule implemented differently across layers | ai | Always |
| [inconsistent-error-mapping](java/inconsistent-error-mapping/rule.md) | Entry point bypasses consistent error mapping | ai | Always |
| [integer-overflow-risk](java/integer-overflow-risk/rule.md) | int arithmetic that can overflow | hybrid | Candidates only |
| [inter-service-call-resilience](java/inter-service-call-resilience/rule.md) | Missing resilience controls on inter-service calls | ai | Always |
| [interrupt-flag-not-restored](java/interrupt-flag-not-restored/rule.md) | InterruptedException swallowed | ast | No |
| [jpa-entity-equals-hashcode](java/jpa-entity-equals-hashcode/rule.md) | Entity equals/hashCode that breaks collections or persistence | hybrid | Candidates only |
| [kafka-ack-before-processing-complete](java/kafka-ack-before-processing-complete/rule.md) | Offset commit/ack timing vs processing and DB commit | ai | Always |
| [kafka-consumer-failure-handling](java/kafka-consumer-failure-handling/rule.md) | Swallowed errors, poison messages and batch failures | ai | Always |
| [kafka-deserialization-error-routing](java/kafka-deserialization-error-routing/rule.md) | Deserialization errors and schema skew | ai | Always |
| [kafka-non-idempotent-consumer](java/kafka-non-idempotent-consumer/rule.md) | Redelivery duplicates a side effect | ai | Always |
| [kafka-partition-key-ordering](java/kafka-partition-key-ordering/rule.md) | Per-entity ordering without a stable partition key | ai | Always |
| [kafka-producer-send-unobserved](java/kafka-producer-send-unobserved/rule.md) | Producer send result ignored when success matters | hybrid | Candidates only |
| [kafka-rebalance-and-poll-interval](java/kafka-rebalance-and-poll-interval/rule.md) | Processing time or local state breaks on rebalance | ai | Always |
| [lazy-association-outside-session](java/lazy-association-outside-session/rule.md) | Lazy association accessed after the persistence context closes | hybrid | Candidates only |
| [lock-not-released-in-finally](java/lock-not-released-in-finally/rule.md) | Lock acquired without unlock in finally | ast | No |
| [lock-ordering-deadlock](java/lock-ordering-deadlock/rule.md) | Inconsistent lock acquisition order | ai | Always |
| [log-and-rethrow](java/log-and-rethrow/rule.md) | Exception logged and rethrown (duplicate noise) | hybrid | Candidates only |
| [log-message-without-stacktrace](java/log-message-without-stacktrace/rule.md) | Unexpected failure logged without stack trace | ast | No |
| [lost-update-read-modify-write](java/lost-update-read-modify-write/rule.md) | Unprotected read-modify-write on a shared row | ai | Always |
| [missing-endpoint-authorization](java/missing-endpoint-authorization/rule.md) | New endpoint without authorization | hybrid | Candidates only |
| [missing-network-timeout](java/missing-network-timeout/rule.md) | Network or database call without bounded timeout | hybrid | Candidates only |
| [missing-request-validation](java/missing-request-validation/rule.md) | Request body not validated | hybrid | Candidates only |
| [mock-verifies-implementation-details](java/mock-verifies-implementation-details/rule.md) | Mock verification instead of observable behaviour | ai | Always |
| [mocked-test-cannot-prove-transaction-guarantee](java/mocked-test-cannot-prove-transaction-guarantee/rule.md) | Mocks used to claim transactional delivery | ai | Always |
| [multi-step-write-partial-success](java/multi-step-write-partial-success/rule.md) | Multi-step write can partially succeed | ai | Always |
| [mutable-shared-state](java/mutable-shared-state/rule.md) | Mutable static field or singleton bean state | hybrid | Candidates only |
| [nondeterministic-test](java/nondeterministic-test/rule.md) | Test depends on time, randomness, order or the network | hybrid | Candidates only |
| [optional-field-or-parameter](java/optional-field-or-parameter/rule.md) | Optional used as a field or parameter | hybrid | Candidates only |
| [optional-get-without-check](java/optional-get-without-check/rule.md) | Optional.get() without presence check | ast | No |
| [pagination-batching-boundary-error](java/pagination-batching-boundary-error/rule.md) | Off-by-one or lost items in pagination and batching | ai | Always |
| [rabbit-ack-nack-handling](java/rabbit-ack-nack-handling/rule.md) | Ack/nack/reject behaviour wrong on success or failure | ai | Always |
| [rabbit-connection-per-message](java/rabbit-connection-per-message/rule.md) | Connection or channel created per message | hybrid | Candidates only |
| [rabbit-durability-and-ttl-contract](java/rabbit-durability-and-ttl-contract/rule.md) | Queue/message durability, TTL and DLX mismatch | ai | Always |
| [rabbit-endless-requeue](java/rabbit-endless-requeue/rule.md) | Permanent failure requeued forever | ai | Always |
| [rabbit-publisher-confirms-and-routing](java/rabbit-publisher-confirms-and-routing/rule.md) | Publishes without confirms or unroutable handling | ai | Always |
| [readonly-transaction-with-writes](java/readonly-transaction-with-writes/rule.md) | Write inside a readOnly transaction | hybrid | Candidates only |
| [reference-equality-on-values](java/reference-equality-on-values/rule.md) | == on strings or boxed numbers | ast | No |
| [remote-call-in-transaction](java/remote-call-in-transaction/rule.md) | Remote call or sleep inside an open database transaction | hybrid | Candidates only |
| [requires-new-pressure](java/requires-new-pressure/rule.md) | REQUIRES_NEW in repeated work | hybrid | Candidates only |
| [retry-without-bound-or-idempotency](java/retry-without-bound-or-idempotency/rule.md) | Retry loop unbounded, unclassified or on non-idempotent work | hybrid | Candidates only |
| [return-or-throw-in-finally](java/return-or-throw-in-finally/rule.md) | Return or throw inside finally | ast | No |
| [risky-change-without-tests](java/risky-change-without-tests/rule.md) | Risky changed branch with no test in the MR | ai | Always |
| [rolling-deploy-incompatibility](java/rolling-deploy-incompatibility/rule.md) | Contract change unsafe while old and new versions coexist | ai | Always |
| [scheduled-task-overlap](java/scheduled-task-overlap/rule.md) | Scheduled tasks that block or overlap | hybrid | Candidates only |
| [shared-simpledateformat](java/shared-simpledateformat/rule.md) | SimpleDateFormat held in a shared field | ast | No |
| [shared-unsafe-collection](java/shared-unsafe-collection/rule.md) | HashMap or ArrayList shared across threads | hybrid | Candidates only |
| [sqs-visibility-timeout-and-delete-timing](java/sqs-visibility-timeout-and-delete-timing/rule.md) | SQS delete timing vs processing duration | ai | Always |
| [stale-entity-after-rollback](java/stale-entity-after-rollback/rule.md) | In-memory entity used as if changes persisted after rollback | ai | Always |
| [test-without-meaningful-assertion](java/test-without-meaningful-assertion/rule.md) | Test with no assertion or only assertNotNull | ast | No |
| [threadlocal-not-cleaned](java/threadlocal-not-cleaned/rule.md) | ThreadLocal not removed on pooled threads | ast | No |
| [timezone-ambiguity](java/timezone-ambiguity/rule.md) | Timestamps crossing boundaries without zone information | hybrid | Candidates only |
| [transactional-on-private-method](java/transactional-on-private-method/rule.md) | @Transactional that Spring cannot intercept | ast | No |
| [transactional-rollback-mismatch](java/transactional-rollback-mismatch/rule.md) | Transaction rollback does not match the failure handling | hybrid | Candidates only |
| [unbounded-executor-queue](java/unbounded-executor-queue/rule.md) | Executor configuration allowing unbounded backlog or starvation | hybrid | Candidates only |
| [unbounded-findall](java/unbounded-findall/rule.md) | Unbounded repository query or whole-file read | hybrid | Candidates only |
| [unchecked-null-dereference](java/unchecked-null-dereference/rule.md) | Possibly null value dereferenced without a check | hybrid | Candidates only |
| [unclosed-resource](java/unclosed-resource/rule.md) | Closeable not closed on every exit path | hybrid | Candidates only |
| [unconsumed-lazy-stream](java/unconsumed-lazy-stream/rule.md) | Stream pipeline never consumed or with lazy side effects | ast | No |

## python

| Rule | Title | Type | AI call |
|---|---|---|---|
| [async-resource-exceptional-exit](python/async-resource-exceptional-exit/rule.md) | Async generators, clients and locks not cleaned up on cancel or error | ai | Always |
| [aws-credentials-region-iam](python/aws-credentials-region-iam/rule.md) | Credential/region lookup, rotation and IAM scope | ai | Always |
| [background-task-ownership-shutdown](python/background-task-ownership-shutdown/rule.md) | Detached task not owned, awaited or shut down | ai | Always |
| [bare-except](python/bare-except/rule.md) | Bare except or BaseException catch | ast | No |
| [blocking-call-in-async](python/blocking-call-in-async/rule.md) | Blocking call inside async def | ast | No |
| [boto3-pagination-timeouts-lifecycle](python/boto3-pagination-timeouts-lifecycle/rule.md) | Unpaginated listings, unobserved failures, missing timeouts and client lifecycle | hybrid | Candidates only |
| [cache-key-isolation-and-ttl](python/cache-key-isolation-and-ttl/rule.md) | Cache key isolation, TTL units, invalidation timing and staleness | ai | Always |
| [cache-nonatomic-read-modify-write](python/cache-nonatomic-read-modify-write/rule.md) | Non-atomic cache updates and separate value/expiry commands | hybrid | Candidates only |
| [cache-stampede-and-distributed-lock](python/cache-stampede-and-distributed-lock/rule.md) | Cache stampedes, negative caching, lock ownership, lease expiry and fencing | ai | Always |
| [cancellation-deadline-propagation](python/cancellation-deadline-propagation/rule.md) | Timeouts and cancellation do not stop work or undo writes | ai | Always |
| [celery-delivery-and-failure-semantics](python/celery-delivery-and-failure-semantics/rule.md) | Celery delivery, execution, retry, result and worker-loss semantics | ai | Always |
| [celery-dispatch-before-commit](python/celery-dispatch-before-commit/rule.md) | Task dispatched inside a transaction before it commits | hybrid | Candidates only |
| [celery-subtask-wait-starvation](python/celery-subtask-wait-starvation/rule.md) | Task blocks waiting for subtasks, chords or groups | hybrid | Candidates only |
| [config-defaults-and-import-side-effects](python/config-defaults-and-import-side-effects/rule.md) | Invalid environment defaults, import-time side effects and per-worker startup | hybrid | Candidates only |
| [context-manager-exit-suppression](python/context-manager-exit-suppression/rule.md) | __exit__/__aexit__ suppresses or replaces the original failure | hybrid | Candidates only |
| [contextvar-and-thread-local-leak](python/contextvar-and-thread-local-leak/rule.md) | Request or tenant context leaks or disappears across tasks and threads | ai | Always |
| [datetime-timezone-and-units](python/datetime-timezone-and-units/rule.md) | Timezone, DST, timestamp-unit and serialization errors | ai | Always |
| [deadlock-and-executor-starvation](python/deadlock-and-executor-starvation/rule.md) | Opposing lock order, unreleased locks or pool self-wait | ai | Always |
| [decimal-from-float](python/decimal-from-float/rule.md) | Decimal built from a float | ast | No |
| [django-orm-evaluation-and-oncommit](python/django-orm-evaluation-and-oncommit/rule.md) | Lazy QuerySets, on_commit callbacks and sync ORM in async contexts | ai | Always |
| [dto-mapping-field-loss](python/dto-mapping-field-loss/rule.md) | DTO/model mapping drops fields, exposes sensitive data or skews versions | ai | Always |
| [dual-write-outbox-partial-failure](python/dual-write-outbox-partial-failure/rule.md) | Database plus broker/cache/search write without a safe failure window | ai | Always |
| [dynamodb-conditional-and-batch-handling](python/dynamodb-conditional-and-batch-handling/rule.md) | Conditional writes, unprocessed items, decimals and read consistency | hybrid | Candidates only |
| [event-loop-ownership](python/event-loop-ownership/rule.md) | Nested asyncio.run or cross-thread access to loop-bound objects | hybrid | Candidates only |
| [gather-taskgroup-error-semantics](python/gather-taskgroup-error-semantics/rule.md) | gather/TaskGroup failure leaves siblings running or hides errors | hybrid | Candidates only |
| [graceful-shutdown-draining](python/graceful-shutdown-draining/rule.md) | Shutdown does not stop intake, drain work or close resources within the termination budget | ai | Always |
| [handler-execution-and-lifespan](python/handler-execution-and-lifespan/rule.md) | Sync/async handler semantics, lifespan clients and dependency cleanup | ai | Always |
| [hardcoded-credentials](python/hardcoded-credentials/rule.md) | Secrets and credentials embedded in source | hybrid | Candidates only |
| [http-call-without-timeout](python/http-call-without-timeout/rule.md) | HTTP client call with no timeout | ast | No |
| [http-response-status-unchecked](python/http-response-status-unchecked/rule.md) | HTTP/gRPC response used without status or error classification | hybrid | Candidates only |
| [idempotency-key-handling](python/idempotency-key-handling/rule.md) | Idempotency reservation, payload conflict and replay defects | ai | Always |
| [in-process-lock-multi-replica](python/in-process-lock-multi-replica/rule.md) | In-memory lock or deduplication used across processes or replicas | ai | Always |
| [is-literal-comparison](python/is-literal-comparison/rule.md) | Identity comparison with a literal | ast | No |
| [iteration-and-pagination-boundaries](python/iteration-and-pagination-boundaries/rule.md) | Exhausted generator, mutation during iteration, pagination or batch boundary errors | ai | Always |
| [kafka-offset-commit-ordering](python/kafka-offset-commit-ordering/rule.md) | Offset commit not ordered after processing and durable commit | ai | Always |
| [kafka-ordering-and-schema](python/kafka-ordering-and-schema/rule.md) | Partition keys, per-partition concurrency, batch partial failure and schema compatibility | ai | Always |
| [kafka-poison-and-skipped-records](python/kafka-poison-and-skipped-records/rule.md) | Swallowed failures, poison-message loops and offsets advancing past failed records | ai | Always |
| [kafka-poll-interval-and-rebalance](python/kafka-poll-interval-and-rebalance/rule.md) | Processing time versus poll/heartbeat limits and rebalance cleanup | ai | Always |
| [kafka-producer-delivery-unobserved](python/kafka-producer-delivery-unobserved/rule.md) | Producer send without observing delivery or flushing | hybrid | Candidates only |
| [kafka-redelivery-idempotency](python/kafka-redelivery-idempotency/rule.md) | Non-idempotent effects under Kafka redelivery | ai | Always |
| [lambda-lifecycle-and-state](python/lambda-lifecycle-and-state/rule.md) | Unfinished work at return, warm-instance state and connection pressure | ai | Always |
| [late-binding-loop-closure](python/late-binding-loop-closure/rule.md) | Late-bound loop variable captured by a callback | hybrid | Candidates only |
| [lost-update-and-concurrent-upsert](python/lost-update-and-concurrent-upsert/rule.md) | Check-then-insert and read-modify-write races on persisted data | ai | Always |
| [mock-only-guarantee-tests](python/mock-only-guarantee-tests/rule.md) | Mock-only tests offered as proof of ordering or delivery guarantees | ai | Always |
| [money-numeric-precision](python/money-numeric-precision/rule.md) | Money rounding, mixed numeric types and lossy JSON conversion | ai | Always |
| [multiprocessing-fork-and-pickle](python/multiprocessing-fork-and-pickle/rule.md) | Inherited connections, unpicklable arguments and worker failures | ai | Always |
| [mutable-default-argument](python/mutable-default-argument/rule.md) | Mutable default argument | ast | No |
| [naive-datetime](python/naive-datetime/rule.md) | Naive datetime creation | ast | No |
| [nonatomic-shared-state](python/nonatomic-shared-state/rule.md) | Shared read-modify-write, check-then-act or lazy init without synchronization | ai | Always |
| [none-vs-falsy-confusion](python/none-vs-falsy-confusion/rule.md) | None conflated with zero, false or empty; unchecked missing values | ai | Always |
| [nosql-operator-injection](python/nosql-operator-injection/rule.md) | User-supplied objects used as NoSQL filters | hybrid | Candidates only |
| [orm-lazy-load-and-unbounded-results](python/orm-lazy-load-and-unbounded-results/rule.md) | Detached lazy loads, N+1 queries and unbounded materialization | ai | Always |
| [partial-batch-retry-mapping](python/partial-batch-retry-mapping/rule.md) | Batch retry repeats already-completed non-idempotent work | ai | Always |
| [periodic-job-overlap](python/periodic-job-overlap/rule.md) | Scheduled jobs overlapping, duplicated across schedulers or interrupted mid-work | ai | Always |
| [rabbitmq-ack-paths](python/rabbitmq-ack-paths/rule.md) | Manual ack/nack/reject not matched to processing and durable commit | ai | Always |
| [rabbitmq-channel-safety-and-durability](python/rabbitmq-channel-safety-and-durability/rule.md) | Channel/connection thread safety, durability and lifecycle | ai | Always |
| [rabbitmq-poison-dlq-and-confirms](python/rabbitmq-poison-dlq-and-confirms/rule.md) | Poison requeue loops, retry bounds, dead-lettering and publisher confirms | ai | Always |
| [raise-without-cause](python/raise-without-cause/rule.md) | New exception raised in handler without chaining | ast | No |
| [remote-call-inside-transaction](python/remote-call-inside-transaction/rule.md) | Network call, message send or sleep while a transaction is open | hybrid | Candidates only |
| [replica-read-and-rollback-state](python/replica-read-and-rollback-state/rule.md) | Read-after-write on replicas and in-memory state after rollback | ai | Always |
| [resilience-and-context-propagation](python/resilience-and-context-propagation/rule.md) | Circuit breaker, bulkhead, deadline budget and trace/auth propagation misuse | ai | Always |
| [resource-leak-on-exit](python/resource-leak-on-exit/rule.md) | Files, sockets, responses, subprocesses and locks not released on every path | hybrid | Candidates only |
| [response-exposure-and-mass-assignment](python/response-exposure-and-mass-assignment/rule.md) | Serializer exposure, mass assignment and partial-update semantics | ai | Always |
| [retry-bounds-and-idempotency](python/retry-bounds-and-idempotency/rule.md) | Unbounded or non-idempotent retries after uncertain outcomes | ai | Always |
| [return-in-finally](python/return-in-finally/rule.md) | return, break or continue inside finally | ast | No |
| [rolling-deploy-api-event-compat](python/rolling-deploy-api-event-compat/rule.md) | API, event and stored-payload compatibility across versions | ai | Always |
| [s3-stream-multipart-and-presign](python/s3-stream-multipart-and-presign/rule.md) | S3 stream closure, multipart cleanup, key validation and presigned expiry | ai | Always |
| [saga-compensation-and-fallbacks](python/saga-compensation-and-fallbacks/rule.md) | Compensation ordering/retry defects and fallbacks that mask failure | ai | Always |
| [schema-model-migration-compat](python/schema-model-migration-compat/rule.md) | Model changes inconsistent with constraints, migrations or old/new versions | ai | Always |
| [sensitive-data-in-logs-and-errors](python/sensitive-data-in-logs-and-errors/rule.md) | Secrets or personal data written to logs, errors or responses | hybrid | Candidates only |
| [shared-mutable-class-or-cached-state](python/shared-mutable-class-or-cached-state/rule.md) | Shared mutable class attribute, cached mutable result or aliased collection | hybrid | Candidates only |
| [shell-injection-and-tls](python/shell-injection-and-tls/rule.md) | shell=True and disabled TLS verification | ast | No |
| [side-effect-failure-windows](python/side-effect-failure-windows/rule.md) | Success boundary and per-side-effect failure, duplicate and concurrency windows | ai | Always |
| [sql-string-formatting](python/sql-string-formatting/rule.md) | SQL built with string formatting | ast | No |
| [sqlalchemy-session-misuse](python/sqlalchemy-session-misuse/rule.md) | Shared, reused-after-failure or escaping SQLAlchemy sessions | ai | Always |
| [sqs-delete-visibility-partial-batch](python/sqs-delete-visibility-partial-batch/rule.md) | SQS delete timing, visibility timeout, partial batch responses and poison routing | ai | Always |
| [streaming-upload-and-webhook-body](python/streaming-upload-and-webhook-body/rule.md) | Streaming failures after headers, body/upload limits and raw-body signatures | ai | Always |
| [swallowed-db-error-and-savepoints](python/swallowed-db-error-and-savepoints/rule.md) | Caught database error leaves partial commit or unusable transaction | ai | Always |
| [swallowed-exception-false-success](python/swallowed-exception-false-success/rule.md) | Caught exception reported as success | ai | Always |
| [template-and-path-injection](python/template-and-path-injection/rule.md) | Untrusted input in templates, file paths or dynamic code strings | hybrid | Candidates only |
| [tenant-identifier-propagation](python/tenant-identifier-propagation/rule.md) | Tenant/user id not carried from authentication to queries, caches and downstream calls | ai | Always |
| [test-defects](python/test-defects/rule.md) | Unawaited async tests, leaking fixtures, assertions that cannot fail and nondeterminism | hybrid | Candidates only |
| [test-risk-coverage](python/test-risk-coverage/rule.md) | Tests omit the material risks of the change | ai | Always |
| [token-verification-and-session-config](python/token-verification-and-session-config/rule.md) | JWT verification, cookies/session, CSRF, CORS and proxy trust | hybrid | Candidates only |
| [transaction-scope-and-release](python/transaction-scope-and-release/rule.md) | Transaction boundary, commit/rollback and connection release | ai | Always |
| [unawaited-coroutine](python/unawaited-coroutine/rule.md) | Coroutine called but not awaited or scheduled | hybrid | Candidates only |
| [unbounded-buffering](python/unbounded-buffering/rule.md) | Unbounded result lists, subprocess output and queues | ai | Always |
| [unbounded-concurrency](python/unbounded-concurrency/rule.md) | Unbounded gather, task creation, queue or executor submission | hybrid | Candidates only |
| [unsafe-deserialization-or-eval](python/unsafe-deserialization-or-eval/rule.md) | pickle, yaml.load, eval and exec | ast | No |
| [untrusted-archive-and-ssrf](python/untrusted-archive-and-ssrf/rule.md) | Unsafe archive extraction, symlink traversal and server-side request forgery | hybrid | Candidates only |
| [untrusted-payload-runtime-validation](python/untrusted-payload-runtime-validation/rule.md) | Type hints treated as validation of untrusted payloads | ai | Always |
| [validate-after-normalization](python/validate-after-normalization/rule.md) | Untrusted data validated before decoding or normalization | hybrid | Candidates only |
| [web-authn-authz-and-validation](python/web-authn-authz-and-validation/rule.md) | Missing authentication, resource/tenant authorization or nested validation on an endpoint | ai | Always |
| [webhook-signature-and-secret-comparison](python/webhook-signature-and-secret-comparison/rule.md) | Missing webhook signature/replay checks and timing-unsafe secret comparison | hybrid | Candidates only |

## sql

| Rule | Title | Type | AI call |
|---|---|---|---|
| [add-not-null-column-without-default](sql/add-not-null-column-without-default/rule.md) | NOT NULL column added with no default | regex | No |
| [blocking-backfill-or-constraint](sql/blocking-backfill-or-constraint/rule.md) | Backfill or constraint change that locks or scans a large table | hybrid | Candidates only |
| [blocking-index-creation](sql/blocking-index-creation/rule.md) | Index created without a non-blocking option | hybrid | Candidates only |
| [destructive-schema-change](sql/destructive-schema-change/rule.md) | Drop, rename or truncate | hybrid | Candidates only |
| [dynamic-sql-concatenation](sql/dynamic-sql-concatenation/rule.md) | Dynamic SQL built from concatenated values | regex | No |
| [non-idempotent-partial-migration](sql/non-idempotent-partial-migration/rule.md) | Data change that can leave partially migrated data | ai | Always |
| [query-returns-wrong-rows](sql/query-returns-wrong-rows/rule.md) | Changed query returns wrong rows for a given input | ai | Always |
| [rolling-deploy-incompatibility](sql/rolling-deploy-incompatibility/rule.md) | Migration breaks the previous application version | ai | Always |
| [update-or-delete-without-where](sql/update-or-delete-without-where/rule.md) | UPDATE or DELETE affecting every row | regex | No |

## typescript

| Rule | Title | Type | AI call |
|---|---|---|---|
| [allsettled-ignored-rejections](typescript/allsettled-ignored-rejections/rule.md) | Promise.allSettled results not classified | ast | No |
| [async-callback-ignored-by-api](typescript/async-callback-ignored-by-api/rule.md) | async callback passed to an API that does not await it | hybrid | Candidates only |
| [async-callback-in-foreach](typescript/async-callback-in-foreach/rule.md) | async callback passed to forEach | ast | No |
| [await-interleaving-race](typescript/await-interleaving-race/rule.md) | Check-then-act or lazy init spanning an await | ai | Always |
| [aws-credentials-region-iam](typescript/aws-credentials-region-iam/rule.md) | Credential, region configuration, rotation and IAM scope | ai | Always |
| [aws-sdk-client-misuse](typescript/aws-sdk-client-misuse/rule.md) | SDK send failures, retries, pagination and client lifecycle | ai | Always |
| [batch-write-partial-retry](typescript/batch-write-partial-retry/rule.md) | Retrying a whole batch after partial success | ai | Always |
| [cache-key-isolation-ttl](typescript/cache-key-isolation-ttl/rule.md) | Cache keys, TTL units, invalidation and stale values | ai | Always |
| [cache-nonatomic-read-modify-write](typescript/cache-nonatomic-read-modify-write/rule.md) | Non-atomic cache updates or separate value and expiry | ai | Always |
| [cache-stampede-negative-caching](typescript/cache-stampede-negative-caching/rule.md) | Cache stampede and negative caching | ai | Always |
| [changed-code-without-test](typescript/changed-code-without-test/rule.md) | Changed component or logic has no matching test | ai | Always |
| [command-injection](typescript/command-injection/rule.md) | Shell command built from untrusted input | hybrid | Candidates only |
| [cookie-cors-csrf-misconfig](typescript/cookie-cors-csrf-misconfig/rule.md) | Insecure cookie, session, CORS or CSRF configuration | hybrid | Candidates only |
| [dto-mapping-data-loss](typescript/dto-mapping-data-loss/rule.md) | DTO mapping loses fields, leaks data or changes defaults | ai | Always |
| [dual-write-partial-failure](typescript/dual-write-partial-failure/rule.md) | Database plus broker/cache/search write without a recovery path | ai | Always |
| [duplicate-insert-and-affected-rows](typescript/duplicate-insert-and-affected-rows/rule.md) | App-only uniqueness check or ignored affected-row count | ai | Always |
| [dynamodb-conditional-and-batch](typescript/dynamodb-conditional-and-batch/rule.md) | Conditional updates, unprocessed items and read consistency | ai | Always |
| [effect-without-cleanup](typescript/effect-without-cleanup/rule.md) | useEffect starts a timer, subscription or listener without cleanup | ast | No |
| [error-cause-lost](typescript/error-cause-lost/rule.md) | Wrapped error drops the original cause | ast | No |
| [event-loop-blocking](typescript/event-loop-blocking/rule.md) | Synchronous or CPU-heavy work on the Node event loop | hybrid | Candidates only |
| [eventemitter-unhandled-error](typescript/eventemitter-unhandled-error/rule.md) | EventEmitter error events and async listener rejections | hybrid | Candidates only |
| [express-async-handler-rejection](typescript/express-async-handler-rejection/rule.md) | Rejected async route handler not forwarded to error handling | hybrid | Candidates only |
| [fetch-without-error-handling](typescript/fetch-without-error-handling/rule.md) | Network call with no error handling | ast | No |
| [finally-overrides-result](typescript/finally-overrides-result/rule.md) | return or throw in finally replaces the original outcome | linter | No |
| [float-money-arithmetic](typescript/float-money-arithmetic/rule.md) | Money computed with binary floating point | hybrid | Candidates only |
| [floating-promise](typescript/floating-promise/rule.md) | Promise neither awaited nor handled | linter | No |
| [graceful-shutdown-handling](typescript/graceful-shutdown-handling/rule.md) | SIGTERM path does not drain work or close clients | ai | Always |
| [hardcoded-secret](typescript/hardcoded-secret/rule.md) | Secret committed in source or config | regex | No |
| [http-client-no-timeout](typescript/http-client-no-timeout/rule.md) | Outbound HTTP/gRPC call without a bounded timeout | ast | No |
| [idempotency-key-defects](typescript/idempotency-key-defects/rule.md) | Idempotency key scope, reservation or replay handled incorrectly | ai | Always |
| [in-memory-coordination-multi-instance](typescript/in-memory-coordination-multi-instance/rule.md) | In-process lock, dedupe or counter used across instances | ai | Always |
| [index-as-key](typescript/index-as-key/rule.md) | Array index used as React list key | ast | No |
| [invariant-bypass-across-entry-points](typescript/invariant-bypass-across-entry-points/rule.md) | Business invariant enforced on one entry point only | ai | Always |
| [job-queue-retry-and-locks](typescript/job-queue-retry-and-locks/rule.md) | BullMQ job retry idempotency, lock expiry and failure observation | ai | Always |
| [jwt-verification-weak](typescript/jwt-verification-weak/rule.md) | Token verification without algorithm, issuer, audience or expiry enforcement | ast | No |
| [kafka-consumer-timing-rebalance](typescript/kafka-consumer-timing-rebalance/rule.md) | Processing time versus heartbeat, session and rebalance | ai | Always |
| [kafka-deserialization-schema-state](typescript/kafka-deserialization-schema-state/rule.md) | Deserialization, schema compatibility and partition state cleanup | ai | Always |
| [kafka-offset-commit-ordering](typescript/kafka-offset-commit-ordering/rule.md) | Offset commit not aligned with completed processing | ai | Always |
| [kafka-partition-key-ordering](typescript/kafka-partition-key-ordering/rule.md) | Per-entity ordering not preserved | ai | Always |
| [kafka-poison-message-handling](typescript/kafka-poison-message-handling/rule.md) | Swallowed failures, poison loops and skipped records | ai | Always |
| [lambda-lifecycle-hazards](typescript/lambda-lifecycle-hazards/rule.md) | Unfinished work, warm-instance state and connection pressure in Lambda | ai | Always |
| [loading-state-never-cleared](typescript/loading-state-never-cleared/rule.md) | Loading flag not reset when the request fails | ast | No |
| [lock-ordering-and-ownership](typescript/lock-ordering-and-ownership/rule.md) | Lock order, release and distributed-lock ownership | ai | Always |
| [loose-equality](typescript/loose-equality/rule.md) | == or != instead of === or !== | ast | No |
| [message-redelivery-idempotency](typescript/message-redelivery-idempotency/rule.md) | Non-idempotent side effects under at-least-once delivery | ai | Always |
| [missing-authorization-or-tenant-scope](typescript/missing-authorization-or-tenant-scope/rule.md) | Authorization or tenant ownership not enforced on the path | ai | Always |
| [missing-return-after-response](typescript/missing-return-after-response/rule.md) | Response or next(err) sent without returning | ast | No |
| [mock-only-guarantee-claim](typescript/mock-only-guarantee-claim/rule.md) | Mock-only tests cited as proof of database or broker guarantees | ai | Always |
| [mutating-props-or-state](typescript/mutating-props-or-state/rule.md) | Props, state or shared objects mutated in place | hybrid | Candidates only |
| [non-constant-time-secret-compare](typescript/non-constant-time-secret-compare/rule.md) | Secrets or signatures compared with ordinary equality | ast | No |
| [nosql-operator-injection](typescript/nosql-operator-injection/rule.md) | Request object passed into a document query filter | hybrid | Candidates only |
| [null-vs-missing-patch-semantics](typescript/null-vs-missing-patch-semantics/rule.md) | Missing field and explicit null treated alike | ai | Always |
| [number-precision-bigint](typescript/number-precision-bigint/rule.md) | Large identifiers or counters in Number, BigInt in JSON | hybrid | Candidates only |
| [orm-n-plus-one-unbounded-read](typescript/orm-n-plus-one-unbounded-read/rule.md) | N+1 queries, unbounded reads and connection lifecycle | hybrid | Candidates only |
| [overlapping-async-interval](typescript/overlapping-async-interval/rule.md) | setInterval with an async callback can overlap | ast | No |
| [pagination-iteration-defects](typescript/pagination-iteration-defects/rule.md) | Pagination, batching or iteration skips, duplicates or truncates | ai | Always |
| [path-traversal](typescript/path-traversal/rule.md) | Filesystem path built from untrusted input | hybrid | Candidates only |
| [poisoned-memoized-promise](typescript/poisoned-memoized-promise/rule.md) | Rejected initialization promise cached permanently | hybrid | Candidates only |
| [post-rollback-state-use](typescript/post-rollback-state-use/rule.md) | Uncommitted entities, ids, events or cache used after rollback | ai | Always |
| [producer-send-unobserved](typescript/producer-send-unobserved/rule.md) | Broker publish failures not observed or flushed | hybrid | Candidates only |
| [promise-all-sibling-continues](typescript/promise-all-sibling-continues/rule.md) | Promise.all rejection leaves siblings running | ai | Always |
| [prototype-pollution](typescript/prototype-pollution/rule.md) | Untrusted object merged into another object | hybrid | Candidates only |
| [queue-requeue-loop-dlq](typescript/queue-requeue-loop-dlq/rule.md) | Permanent failures requeued forever or without dead-letter routing | hybrid | Candidates only |
| [rabbitmq-ack-paths](typescript/rabbitmq-ack-paths/rule.md) | Ack/nack/reject not aligned with processing on every exit | hybrid | Candidates only |
| [rabbitmq-publisher-confirms](typescript/rabbitmq-publisher-confirms/rule.md) | Publisher confirms, unroutable messages and durability | hybrid | Candidates only |
| [react-hooks-rules](typescript/react-hooks-rules/rule.md) | Conditional hooks and wrong dependency arrays | linter | No |
| [read-modify-write-lost-update](typescript/read-modify-write-lost-update/rule.md) | Read-modify-write not protected against concurrent updates | ai | Always |
| [remote-call-in-transaction](typescript/remote-call-in-transaction/rule.md) | Remote call, sleep or broker send inside a DB transaction | hybrid | Candidates only |
| [request-body-limits](typescript/request-body-limits/rule.md) | Missing body, upload or parser limits | hybrid | Candidates only |
| [resilience-fallback-false-success](typescript/resilience-fallback-false-success/rule.md) | Fallbacks, breakers and propagation hide failure | ai | Always |
| [retry-non-idempotent-or-amplifying](typescript/retry-non-idempotent-or-amplifying/rule.md) | Retries that duplicate writes or amplify load | ai | Always |
| [rolling-deploy-compatibility](typescript/rolling-deploy-compatibility/rule.md) | Route, payload, event or cache-format change breaks old/new coexistence | ai | Always |
| [s3-stream-pagination-multipart](typescript/s3-stream-pagination-multipart/rule.md) | S3 streaming, pagination, multipart cleanup, keys and presigned URLs | ai | Always |
| [saga-compensation-defects](typescript/saga-compensation-defects/rule.md) | Saga compensation order, failure and repeated execution | ai | Always |
| [scheduled-task-overlap-replicas](typescript/scheduled-task-overlap-replicas/rule.md) | Scheduled or detached work duplicated, overlapping or not durable | ai | Always |
| [schema-change-compatibility](typescript/schema-change-compatibility/rule.md) | Model or schema change incompatible with constraints or old code | ai | Always |
| [sensitive-data-in-logs-responses](typescript/sensitive-data-in-logs-responses/rule.md) | Credentials or personal data in logs, errors or responses | hybrid | Candidates only |
| [shared-state-across-requests](typescript/shared-state-across-requests/rule.md) | Module-level mutable state leaks across requests or tenants | ai | Always |
| [sql-injection-dynamic-query](typescript/sql-injection-dynamic-query/rule.md) | SQL built from strings, including identifiers and ORDER BY | hybrid | Candidates only |
| [sqs-ack-and-partial-batch](typescript/sqs-ack-and-partial-batch/rule.md) | SQS delete timing, partial batch response and visibility | ai | Always |
| [ssrf-and-open-redirect](typescript/ssrf-and-open-redirect/rule.md) | Server-side request or redirect to an untrusted URL | hybrid | Candidates only |
| [stale-replica-read](typescript/stale-replica-read/rule.md) | Read-after-write served from a replica | ai | Always |
| [stream-backpressure-and-errors](typescript/stream-backpressure-and-errors/rule.md) | Stream piping without backpressure, error propagation or cleanup | hybrid | Candidates only |
| [streaming-error-after-headers-sent](typescript/streaming-error-after-headers-sent/rule.md) | Error handling after streaming has begun | ai | Always |
| [success-before-work-complete](typescript/success-before-work-complete/rule.md) | Response, commit or ack issued before required work finishes | ai | Always |
| [swallowed-error-or-false-success](typescript/swallowed-error-or-false-success/rule.md) | Catch or promise chain converts failure into success | hybrid | Candidates only |
| [template-injection](typescript/template-injection/rule.md) | Untrusted input compiled as a template or into a code string | hybrid | Candidates only |
| [test-unreliable-assertions](typescript/test-unreliable-assertions/rule.md) | Async tests that cannot fail or depend on time, randomness, network or order | hybrid | Candidates only |
| [tests-cover-risk-paths](typescript/tests-cover-risk-paths/rule.md) | Tests miss the changed failure, concurrency or contract risk | ai | Always |
| [timeout-without-cancellation](typescript/timeout-without-cancellation/rule.md) | Promise.race timeout that does not cancel the work | hybrid | Candidates only |
| [timer-listener-leak](typescript/timer-listener-leak/rule.md) | Timers or listeners not released on every exit path (Node) | hybrid | Candidates only |
| [timestamp-unit-timezone-mismatch](typescript/timestamp-unit-timezone-mismatch/rule.md) | Timestamp units, invalid dates or timezone conversion | ai | Always |
| [tls-verification-disabled](typescript/tls-verification-disabled/rule.md) | TLS certificate verification turned off | regex | No |
| [token-in-web-storage](typescript/token-in-web-storage/rule.md) | Credential stored in localStorage or sessionStorage | regex | No |
| [transaction-client-not-used](typescript/transaction-client-not-used/rule.md) | Query escapes the transaction-bound client | hybrid | Candidates only |
| [transaction-commit-rollback-paths](typescript/transaction-commit-rollback-paths/rule.md) | Transaction commit, rollback or release missed on some path | hybrid | Candidates only |
| [truthiness-drops-valid-value](typescript/truthiness-drops-valid-value/rule.md) | Truthiness check or default discards 0, "" or false | hybrid | Candidates only |
| [unbounded-concurrency](typescript/unbounded-concurrency/rule.md) | Unbounded Promise.all, queues, workers or recursion | hybrid | Candidates only |
| [unchecked-network-data](typescript/unchecked-network-data/rule.md) | Type assertion or non-null assertion on network data | hybrid | Candidates only |
| [unencoded-url-input](typescript/unencoded-url-input/rule.md) | URL built from user input without encoding | hybrid | Candidates only |
| [unsafe-deserialization](typescript/unsafe-deserialization/rule.md) | Unsafe deserialization of untrusted data | hybrid | Candidates only |
| [unsafe-html-sink](typescript/unsafe-html-sink/rule.md) | innerHTML, dangerouslySetInnerHTML or eval with dynamic data | hybrid | Candidates only |
| [unstable-pagination-order](typescript/unstable-pagination-order/rule.md) | Paginated query without a deterministic ordering | ast | No |
| [unvalidated-runtime-payload](typescript/unvalidated-runtime-payload/rule.md) | Untrusted JSON used without runtime validation | ai | Always |
| [validation-not-enforced](typescript/validation-not-enforced/rule.md) | Validation declared but not actually enabled or nested | ai | Always |
| [webhook-signature-verification](typescript/webhook-signature-verification/rule.md) | Webhook signature, raw body and replay protection | ai | Always |
| [worker-thread-lifecycle](typescript/worker-thread-lifecycle/rule.md) | Worker thread messages, failures and shared buffers | ai | Always |
