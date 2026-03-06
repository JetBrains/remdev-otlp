# OpenTelemetry Diagnostic (Remote Development)

Exports telemetry from JetBrains Remote Development sessions to any OTLP-compatible backend. It keeps frontend and backend activity under one session and can export selected IntelliJ Platform metrics.

## Installation

- Supported only in Remote Development IDEs.
- Install the plugin on both the backend IDE and the frontend client.

## Configuration

Set OTLP options on the backend.

### Required: OTLP headers

```bash
export OTEL_EXPORTER_OTLP_HEADERS="authorization=Bearer your_token"
```

```text
-Dotel.exporter.otlp.headers=authorization=Bearer your_token
```

Headers use the standard [OTEL_EXPORTER_OTLP_HEADERS](https://opentelemetry.io/docs/specs/otel/protocol/exporter/) format. They are propagated from backend to frontend.

### Optional: OTLP endpoint

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost
```

```text
-Dotel.exporter.otlp.endpoint=http://localhost
```

### Optional: feature flags

Environment variables and system properties override saved values from `Settings | Tools | OpenTelemetry Diagnostic`.

| Feature | System property | Environment variable | Default |
| --- | --- | --- | --- |
| Plugin span filter | `rdct.diagnostic.otlp.plugin.span.filter.enabled` | `RDCT_DIAGNOSTIC_OTLP_PLUGIN_SPAN_FILTER_ENABLED` | `true` |
| IJ Platform metrics export | `rdct.diagnostic.otlp.metrics.enabled` | `RDCT_DIAGNOSTIC_OTLP_METRICS_ENABLED` | `true` |
| On-demand performance report (last 5 minutes) | `rdct.diagnostic.otlp.frequent.performance.metrics.reporting.enabled` | `RDCT_DIAGNOSTIC_OTLP_FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED` | `false` |

The settings UI also controls span filtering, performance reporting, frequent spans, span categories, and the IJ Platform subsystem metric allowlist.

## Exported spans

| Span | When it appears | Why it matters |
| --- | --- | --- |
| `session-metadata` | Session initialization | Short-lived lookup span |
| `remote-dev-session` | From client connection until the session ends | Main session span |
| `application-idle` | Telemetry arrives before any remote session starts | Keeps pre-session events |
| `connection-dropped-reconnecting` | Connection to the backend drops and later recovers | Measures reconnect time |
| `ui-thread-freeze` | The platform reports a UI freeze | Records freeze duration |

## Building

```bash
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/`.
