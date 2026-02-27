Unifies frontend and backend events under a single user session span.
Adds an allowlist in settings for IJ Platform subsystem metrics.

Useful to provide observability for IDEs 

## Installation

This plugin must be installed on **both frontend and backend** sides.

## Configuration

### Backend OTLP Headers (required)

Set your OTLP headers **on the backend only** using either:

**Environment Variable:**
```bash
export OTEL_EXPORTER_OTLP_HEADERS="x-honeycomb-team=your_api_key,x-honeycomb-dataset=your_dataset"
```

**OR System Property:**
```
-Dotel.exporter.otlp.headers=x-honeycomb-team=your_api_key,x-honeycomb-dataset=your_dataset
```

Headers follow the standard [OTEL_EXPORTER_OTLP_HEADERS](https://opentelemetry.io/docs/specs/otel/protocol/exporter/) format: comma-separated `key=value` pairs. This works with any OTLP-compatible backend (Honeycomb, Jaeger, Grafana, etc.).

The plugin automatically propagates the headers securely from backend to frontend via encrypted RPC.

### Backend OTLP Endpoint (optional)

**Environment Variable:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=https://api.honeycomb.io
```
**OR System Property:**
```bash
-Dotel.exporter.otlp.endpoint=https://api.honeycomb.io
```
Default: `https://api.honeycomb.io`

### Plugin Span Filter Source

`isPluginSpanFilterEnabled` is resolved in this order:
1. System property/env override (authoritative):
   - `rdct.diagnostic.otlp.plugin.span.filter.enabled`
   - `RDCT_DIAGNOSTIC_OTLP_PLUGIN_SPAN_FILTER_ENABLED`
2. Saved plugin setting (`OpenTelemetry Diagnostic` settings UI)
3. Default: `true`

- `true`: keep only spans from this diagnostic plugin tracer (`com.jetbrains.otp.diagnostic`)
- `false`: do not apply plugin-tracer-only filtering at this stage

### Filtering Settings UI

In `Settings | Tools | OpenTelemetry Diagnostic`, you can configure:
- `Show only a short list of diagnostic spans` (plugin span filter)
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
- `host-name` (string): Name of the backend host
- `session.id` (string): Unique identifier for the session

**Parent:** Root span

**Purpose:** This span is useful for finding and identifying sessions in your telemetry backend even when the main `remote-dev-session` span hasn't closed yet (see note about long-running spans below).

---

### 2. `remote-dev-session`

Tracks an active remote development session from start to finish.

**Created when:** Client connects to remote backend
**Ended when:** Session is terminated or connection ends

**Attributes:**
- `session.id` (string): Unique identifier for the session. Remains the same for frontend and client sessions.

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

**Parent:** Current session span (`remote-dev-session` or `application-idle`)

**Use case:** Measure reconnection latency and identify connection stability issues.

---

### 5. `ui-thread-freeze`

Captures UI freeze events with accurate timing based on freeze duration.

**Created when:** UI freeze is detected and reported by the platform
**Timing:** Uses calculated start time (end time - duration) for accurate span placement

**Parent:** Current session span (`remote-dev-session` or `application-idle`)

**Note:** This span is created retroactively with explicit start/end timestamps calculated from the freeze duration.

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
