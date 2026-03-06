# Metrics Filtering Configuration

## Overview

Prevents rate limiting at scale by implementing two-layer filtering:

1. **Denylist** - Blocks 10 noisy metric patterns
2. **Throttling** - Exports every 5 minutes (default) instead of every minute

**Impact:** ~96% reduction in metric traffic (1M+ → 24K events/min at 5000 developers)

## Configuration

### Denylist (Default: Enabled)

```bash
# Disable denylist
-Drdct.diagnostic.otlp.metrics.denylist.enabled=false
```

**Blocked patterns:**
- `StreamlinedBlobStorage.*`, `FileNameCache.*`, `FilePageCache.*`
- `DiskQueryRelay.*`, `FileChannelInterruptsRetryer.*`
- `VFS.contentStorage.recordsDeduplicated`, `VFS.contentStorage.recordsDecompressionTimeUs`
- `cacheStateStorage.*`
- `DirectByteBufferAllocator.disposed`, `DirectByteBufferAllocator.reclaimed`

**Preserved metrics:** `JVM.*`, `AWTEventQueue.*`, `VFS.*` (most), `workspaceModel.*`, `Indexes.*`, `rdct.*`

### Throttling (Default: 5 minutes)

```bash
# Change export interval (valid: 1-60 minutes)
-Drdct.diagnostic.otlp.metrics.export.interval.minutes=10
```

## Customizing

Edit patterns in:
```
shared/src/main/kotlin/com/jetbrains/otp/exporter/HardcodedListDeniedMetricsProvider.kt
```
