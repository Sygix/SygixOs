# Up Next CPU profile

Run explicitly, outside the normal test suite:

```sh
./gradlew --init-script tools/benchmarks/up-next.init.gradle testDebugUnitTest --tests '*UpNextFeedProfileTest' --no-daemon
```

The harness merges 200, 1,000 and 2,000 synthetic movie rows from two packages. Each size has two warm-up calls followed by five timed calls. Every sample checks the selected row IDs and source-package order. Timings are written to the JUnit XML standard output; there is no wall-clock assertion or CI performance gate.

Compare the same harness on both revisions, with the same JVM, resources and compilation settings and no other heavy workload. These are pure JVM CPU measurements, not Android TV frame-time or end-to-end responsiveness measurements. Keep individual samples and compare their median and range.
