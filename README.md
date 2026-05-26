Unifies frontend and backend events under a single user session span.
Adds an allowlist in settings for IJ Platform subsystem metrics.

Useful to provide observability for IDEs 

## Installation

This plugin works only in Remote Development IDE-s while being installed on **both frontend and backend** sides.

## Configuration

### Backend OTLP Headers (required)

Set your OTLP headers **on the backend only** using either:

**Environment Variable:**
```bash
export OTEL_EXPORTER_OTLP_HEADERS="authorization=Bearer your_token"
```

**OR System Property:**
```
-Dotel.exporter.otlp.headers=authorization=Bearer your_token
```

Headers follow the standard [OTEL_EXPORTER_OTLP_HEADERS](https://opentelemetry.io/docs/specs/otel/protocol/exporter/) format: comma-separated `key=value` pairs. This works with any OTLP-compatible backend (Jaeger, Grafana, OpenTelemetry Collector, etc.).

The plugin automatically propagates the headers securely from backend to frontend via encrypted RPC.

Signal-specific headers are also supported and override common headers with the same key:

```bash
export OTEL_EXPORTER_OTLP_TRACES_HEADERS="x-honeycomb-dataset=your_trace_dataset"
export OTEL_EXPORTER_OTLP_METRICS_HEADERS="x-honeycomb-dataset=your_metrics_dataset"
```

Equivalent system properties:

```bash
-Dotel.exporter.otlp.traces.headers=x-honeycomb-dataset=your_trace_dataset
-Dotel.exporter.otlp.metrics.headers=x-honeycomb-dataset=your_metrics_dataset
```

For Honeycomb, set `OTEL_EXPORTER_OTLP_METRICS_HEADERS` (or `otel.exporter.otlp.metrics.headers`) when metrics should go to a dataset other than Honeycomb's fallback `unknown_metrics`.

### Backend OTLP Endpoint (optional)

**Environment Variable:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost
```
**OR System Property:**
```bash
-Dotel.exporter.otlp.endpoint=http://localhost
```
Default: `http://localhost`

### OTLP Protocol (optional)

Supported values:
- `http/protobuf` (default)
- `grpc`

Set using either:

**Environment Variable:**
```bash
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
```

**OR System Property:**
```bash
-Dotel.exporter.otlp.protocol=grpc
```

In Remote Development mode, the backend OTLP protocol is propagated to the frontend together with the rest of the OTLP connection config. Setting it on the frontend is only needed as a fallback when backend config is unavailable.

### Global Span Attributes (optional)

Set this on the backend using either:

Environment variable:

```bash
export RDCT_COMMON_SPAN_ATTRIBUTES="option1=value1,option2=value2"
```

Or system property:

```bash
-Drdct.common.span.attributes=option1=value1,option2=value2
```

The value uses comma-separated `key=value` pairs, so:

- `-Drdct.common.span.attributes=deployment.environment=staging` adds `deployment.environment=staging`
- `-Drdct.common.span.attributes=deployment.environment=staging,service.instance.id=rd-backend-1` adds both attributes

In Remote Development mode, these backend-defined attributes are propagated to the frontend together with the rest of the OTLP connection config and are added to every exported span and metric on both sides.

The plugin also automatically adds:
- `plugin.version`: installed plugin version
- `idea.version`: IDE product version, for example `2026.1.1`
- `idea.build`: IDE product build number, for example `IU-261.23567.138`

### Frontend Xmx Sync (optional)

Set this on the backend as a system property:

```bash
-Drdct.diagnostic.frontend.xmx.mb=2048
```

The value is interpreted as a positive integer number of MiB. On frontend startup, the plugin fetches this backend property and writes the corresponding `-Xmx` value into the JetBrains Client VM options.

This updates the client configuration for the next JetBrains Client start. The current JVM heap size cannot be changed in-place at runtime.

### Plugin Span Filter Source

`isPluginSpanFilterEnabled` is resolved in this order:
1. System property/env override (authoritative):
   - `rdct.diagnostic.otlp.plugin.span.filter.enabled`
   - `RDCT_DIAGNOSTIC_OTLP_PLUGIN_SPAN_FILTER_ENABLED`
2. Saved plugin setting (`OpenTelemetry Diagnostic` settings UI)
3. Default: `true`

- `true`: keep only spans from this diagnostic plugin tracer (`com.jetbrains.otp.diagnostic`)
- `false`: do not apply plugin-tracer-only filtering at this stage

### Metrics Export Source

`isMetricsExportEnabled` is resolved in this order:
1. System property/env override (authoritative):
   - `rdct.diagnostic.otlp.metrics.enabled`
   - `RDCT_DIAGNOSTIC_OTLP_METRICS_ENABLED`
2. Saved plugin setting (`OpenTelemetry Diagnostic` settings UI)
3. Default: `true`

### Frequent Performance Metrics Reporting Source

The feature is intended for diagnostic analysis of transient performance incidents (for example, UI thread freezes or remote connection drops).  
When triggered, it emits high-frequency CPU and memory metrics for the preceding 5-minute window and continues emitting for the subsequent 5 minutes to capture post-incident recovery behavior.

`frequentPerformanceMetricsReportingEnabled` is resolved in this order:
1. System property/env override (authoritative):
   - `rdct.diagnostic.otlp.frequent.performance.metrics.reporting.enabled`
   - `RDCT_DIAGNOSTIC_OTLP_FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED`
2. Saved plugin setting (`OpenTelemetry Diagnostic` settings UI)
3. Default: `false`


### Filtering Settings UI

In `Settings | Tools | OpenTelemetry Diagnostic`, you can configure:
- `Show only a short list of diagnostic spans` (plugin span filter)
- `Enable on-demand performance report (last 5 minutes)` (frequent performance metrics reporting)
- `Enable frequent spans`
- hierarchical span categories (parent/child checkboxes)

Category and toggle settings are synced frontend -> backend.

## Spans

The plugin creates the following OpenTelemetry spans to track various aspects of your remote development session:

### 1. `session-metadata`

A short-lived metadata span created at session initialization to store session information.

**Created when:** Session is initialized (backend notifies frontend of session start)


**Attributes:**
- `session.trace_id` (string): The trace ID of the session
- `session.span_id` (string): The span ID of the session
- `host.name` (string): Name of the backend host
- `session.id` (string): Unique identifier for the session

**Parent:** Root span

**Purpose:** This span is useful for finding and identifying sessions in your telemetry backend even when the main `remote-dev-session` span hasn't closed yet (see note about long-running spans below).

---

### 2. `remote-dev-session`

Tracks an active remote development session from start to finish.

**Created when:** Client connects to remote backend
**Ended when:** Session is terminated or connection ends

**Attributes:**
- `session.id` (string): Remote Development session identifier. In direct RD links it is derived from the backend certificate fingerprint. If the fingerprint is unavailable, the plugin falls back to the remote connection address, for example `tcp://127.0.0.1:5985`.

**Parent:** Root span

**Note:** This span can be long-running (hours or days) and may not close until the session ends. Use the `session-metadata` span to identify active sessions.

---

### 3. `application-idle`

Lazy-created default span that captures telemetry events before the first remote development session starts.

**Created when:** First telemetry event occurs before any session starts
**Ended when:** First remote session begins or application shuts down
**Note:** This span is only created if events occur before a session starts. It prevents telemetry data loss during application initialization.

---

### 4. `connection-dropped-reconnecting`

Tracks the duration of connection drops and reconnection attempts.

**Created when:** Connection to remote backend is lost
**Ended when:** Connection is successfully re-established

**Attributes:**
- `project` (string): Name of the affected project
- `reconnection.throttled.drop_count` (long): Number of reconnect-drop transitions suppressed before this span

**Parent:** Current session span (`remote-dev-session` or `application-idle`)

**Use case:** Measure reconnection latency and identify connection stability issues.

**Note:** Reconnection spans use a process-wide adaptive cooldown. After an emitted span closes, another span is allowed after 6 minutes; every suppressed reconnect attempt during that cooldown extends the next cooldown exponentially, capped at 1 hour. Suppressed reconnect flaps do not force on-demand performance metrics or UI-thread status checks.

---

### 5. `ui-thread-freeze`

Captures UI freeze events with accurate timing based on freeze duration.

**Created when:** UI freeze is detected and reported by the platform
**Timing:** Uses calculated start time (end time - duration) for accurate span placement

**Parent:** Current session span (`remote-dev-session` or `application-idle`)

**Note:** This span is created retroactively with explicit start/end timestamps calculated from the freeze duration.

---

### 6. `ide-exception.<ExceptionClass>`

Captures uncaught IDE exceptions as short-lived error spans.

**Created when:** The IntelliJ Platform logs an error with a throwable
**Name:** `ide-exception.<ExceptionClass>` (for example `ide-exception.IllegalStateException`)

**Attributes:**
- `exception.type` (string): Exception class name
- `exception.message` (string): Exception message when available
- `exception.stacktrace` (string): Exception stack trace, truncated to 64 KiB
- `exception.source` (string): Reporting path (`platform-log`)
- `plugin` (string): Plugin ID blamed by the platform when available
- `log.logger` (string): Logger name from the platform log record
- `log.level` (string): Log level from the platform log record
- `log.message` (string): Message from the platform log record

**Parent:** Current session span (`remote-dev-session` or `application-idle`)

**Note:** Coroutine exceptions are reported through IntelliJ's platform exception handler, which logs them once via `Logger.error`; this plugin converts that platform log record into a span instead of installing a second coroutine reporter.

---

## Long-Running Spans

Some spans, particularly `remote-dev-session`, are designed to be long-running and may remain open for extended periods (hours or even days) until the session ends. This is normal behavior for session-tracking spans.

When querying your telemetry backend:
- Use the `session-metadata` span to identify and locate sessions, as it closes quickly after session initialization
- The main `remote-dev-session` span will only close when the actual session terminates
- All other spans are short-lived and close immediately after their tracked operations complete

---

## Building the Plugin

To build the plugin distribution:

```bash
./gradlew buildPlugin
```

The built plugin will be available in the `build/distributions/` directory as a `.zip` file that can be installed in IntelliJ IDEA.
