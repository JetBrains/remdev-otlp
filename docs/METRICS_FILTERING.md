# Metrics Filtering

## Overview

Exports only allowlisted metrics to reduce OTLP backend load while preserving valuable performance data.

**Approach:** Allowlist filtering exports ~40-50 high-value metrics every minute:
- JVM metrics (heap, GC, threads, CPU)
- Performance metrics (AWTEventQueue, Indexes, VFS, workspaceModel)
- Plugin's own metrics (rdct.*)

**Impact:** ~75-80% reduction in metrics (200+ → 40-50 per export)

## Configuration

### Allowlist (Default: Enabled)

```bash
# Disable allowlist (export all metrics)
-Drdct.diagnostic.otlp.metrics.allowlist.enabled=false

# Or via environment variable
export RDCT_DIAGNOSTIC_OTLP_METRICS_ALLOWLIST_ENABLED=false
```

## Allowed Metrics

- `rdct.*` - Plugin's own metrics
- `JVM.*` - Heap, GC, threads, CPU
- `AWTEventQueue.*` - UI responsiveness
- `Indexes.*`, `Indexing.*` - Indexing performance
- `workspaceModel.*` - Workspace state
- `VFS.cache.*`, `VFS.fileByIdCache.*` - File system operations
- `ReadAction.*`, `WriteAction.*` - IDE actions
- `FlushQueue.*`, `LowMemory.*`, `MEM.*`, `OS.loadAverage`

## Customizing

To modify the allowlist patterns, edit:
```
shared/src/main/kotlin/com/jetbrains/otp/exporter/HardcodedListAllowedMetricsProvider.kt
```

Supports wildcards: `JVM.*` matches all JVM metrics.
