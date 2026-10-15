# Project Guidance

- Use Java 21 and Spring Boot; keep the two deployable services independent.
- Do not add Docker or cloud-managed infrastructure. RabbitMQ and Redis must run locally as separate processes.
- Keep each service's database separate. The Checkout service must not query the Processor database directly.
- Preserve the required queues `order-created` and `order-compensation`; `order-status-updated` carries final status changes to Checkout.
- Propagate `X-Correlation-Id` through HTTP, AMQP message headers, and structured logs/MDC.
- Keep the MVP small, explain material design decisions in Portuguese, and include a runnable local test/demo path in README.md.