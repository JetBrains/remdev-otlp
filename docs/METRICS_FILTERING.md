# Metrics Filtering & Throttling Configuration

## Overview

To address rate limiting at scale (5000+ developers), the plugin implements **two-layer filtering**:

1. **Denylist Filtering** - Blocks noisy metrics by name pattern
2. **Export Throttling** - Controls how often metrics are sent to backend

## Configuration

### 1. Metrics Denylist (Default: Enabled)

Blocks low-value metrics to reduce cardinality.

**Setting:** `metricsDenylistEnabled: Boolean = true`

**System Property:** `-Drdct.diagnostic.otlp.metrics.denylist.enabled=false`
**Environment Variable:** `RDCT_DIAGNOSTIC_OTLP_METRICS_DENYLIST_ENABLED=false`

**Default Denylist Patterns:**
```kotlin
StreamlinedBlobStorage.*      // Always 0, not useful
FileNameCache.*               // High frequency, low value
FilePageCache.*               // Mostly 0s
DiskQueryRelay.*             // Mostly 0s unless indexing
FileChannelInterruptsRetryer.* // Rarely non-zero
VFS.contentStorage.recordsDeduplicated
VFS.contentStorage.recordsDecompressionTimeUs
cacheStateStorage.*
DirectByteBufferAllocator.disposed
DirectByteBufferAllocator.reclaimed
```

**Metrics KEPT (valuable for performance analysis):**
- `JVM.*` - Heap, GC, threads, CPU
- `AWTEventQueue.*` - UI responsiveness
- `VFS.*` (most) - File system operations
- `workspaceModel.*` - IDE workspace state
- `Indexes.*` - Indexing performance
- `rdct.*` - Plugin's own metrics

### 2. Export Throttling (Default: 5 minutes)

Controls export frequency to avoid rate limits.

**Setting:** `metricsExportIntervalMinutes: Int = 5`

**System Property:** `-Drdct.diagnostic.otlp.metrics.export.interval.minutes=10`
**Environment Variable:** `RDCT_DIAGNOSTIC_OTLP_METRICS_EXPORT_INTERVAL_MINUTES=10`

**Valid Range:** 1-60 minutes

### How It Works

```
Platform collects metrics every ~1 minute
↓
Export Pipeline:
├─ Global on/off check (metricsExportEnabled)
├─ Throttling (only export every N minutes)
├─ Denylist filtering (remove noisy metrics)
└─ OTLP Export to backend
```

**Without throttling:** 5000 devs × 2 exports/min × 100 metrics = **1M metrics/minute**
**With 5min throttle + denylist:** 5000 devs × 2 exports/5min × 60 metrics = **120K metrics/5min** (24K/min) = **~96% reduction**

## Example Configurations

### Production (Minimize Backend Load)
```bash
# Export every 10 minutes, keep denylist
export RDCT_DIAGNOSTIC_OTLP_METRICS_EXPORT_INTERVAL_MINUTES=10
export RDCT_DIAGNOSTIC_OTLP_METRICS_DENYLIST_ENABLED=true
```

### Development (More frequent, keep all metrics)
```bash
# Export every 2 minutes, disable denylist
export RDCT_DIAGNOSTIC_OTLP_METRICS_EXPORT_INTERVAL_MINUTES=2
export RDCT_DIAGNOSTIC_OTLP_METRICS_DENYLIST_ENABLED=false
```

### Debugging (Maximum detail)
```bash
# Export every minute, no filtering
export RDCT_DIAGNOSTIC_OTLP_METRICS_EXPORT_INTERVAL_MINUTES=1
export RDCT_DIAGNOSTIC_OTLP_METRICS_DENYLIST_ENABLED=false
```

## Customizing the Denylist

To add custom patterns, edit:
`/shared/src/main/kotlin/com/jetbrains/otp/exporter/HardcodedListDeniedMetricsProvider.kt`

```kotlin
private val DENIED_METRICS = listOf(
    "StreamlinedBlobStorage.*",
    "YourCustomMetric.*",  // Add your patterns here
    // Supports wildcards (*)
)
```

## Monitoring

The `ThrottledMetricExporter` logs debug messages:
- `"Exporting N metrics (Xms since last export)"` - When actually exporting
- `"Throttling metric export (Xms < Yms)"` - When skipping due to throttle

Enable debug logging:
```bash
-Didea.log.debug.categories=#com.jetbrains.otp.exporter.ThrottledMetricExporter
```
