# Up Next CPU profile

Run explicitly, outside the normal test suite:

```sh
./gradlew --init-script tools/benchmarks/up-next.init.gradle testDebugUnitTest --tests '*UpNextFeedProfileTest' --no-daemon
```

The harness merges 200, 1,000 and 2,000 synthetic movie rows from two packages. Each size has two warm-up calls followed by five timed calls. Every sample checks the selected row IDs and source-package order. Timings are written to the JUnit XML standard output; there is no wall-clock assertion or CI performance gate.

Compare the same harness on both revisions, with the same JVM, resources and compilation settings and no other heavy workload. These are pure JVM CPU measurements, not Android TV frame-time or end-to-end responsiveness measurements. Keep individual samples and compare their median and range.

## Audit comparison

The same fixture was run before and after indexed grouping, on Java 21 under the same exclusive resource lock. Two warmups preceded five measured samples at each size. The pre-change reference was `9f6afae`; the indexed implementation was later committed as `a55f323`.

| Rows | Before median (ms) | After median (ms) |
|---|---:|---:|
| 200 | 22.014 | 2.544 |
| 1,000 | 397.844 | 7.952 |
| 2,000 | 1,579.799 | 8.848 |

At 2,000 rows, before samples were 1579.799, 1575.868, 1583.285, 1581.125 and 1579.758 ms; after samples were 9.116, 8.848, 8.701, 8.612 and 10.950 ms. All samples checked the selected IDs and source ordering. The same source fixture was used before moving it out of the default test source set into this opt-in harness.

This comparison demonstrates the synthetic merge CPU improvement. It does not measure Android frame presentation, total startup time, or real-TV performance, and introduces no CI timing threshold.
