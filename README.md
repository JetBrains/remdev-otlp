Filters IDE OpenTelemetry events and adds Remote Development–specific events.
Useful for debugging issues with Remote Development setups, especially when the backend runs on the host machine.

## Installation

This plugin  must be installed on **both frontend and backend** sides.

## Configuration

### Required Configuration

Set your Honeycomb API key **on the backend only** using either:

**Environment Variable:**
```bash
export HONEYCOMB_API_KEY=your_api_key_here
```

**OR System Property:**
```
-Dhoneycomb.api.key=your_api_key_here
```

The plugin automatically propagates the API key securely from backend to frontend via encrypted RPC

### Optional Configuration

**Honeycomb Dataset:**
```bash
export HONEYCOMB_DATASET=your_dataset_name
```
Default: `intellij-plugin`

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
