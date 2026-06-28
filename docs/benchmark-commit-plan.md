# v3.1.0 Benchmark Commit Plan

Implementation checklist for the `-benchmark` flag. Push after each commit;
CI must pass before the next commit.

## Commits

| # | Message | Deliverable |
| - | ------- | ----------- |
| 1 | `feat(benchmark): add LatencyStats and BenchmarkConfig` | Percentile stats + CLI config |
| 2 | `feat(benchmark): add HardwareInfo and BenchmarkReport` | Environment capture + one-liner formatter |
| 3 | `feat(benchmark): add BenchmarkRunner and BenchmarkMain` | In-process SearchEngine timing loop |
| 4 | `feat(cli): wire -benchmark flag and IndexOpener.openOrBuild` | Driver integration + index build for benchmark |
| 5 | `test(benchmark): add LatencyStats and BenchmarkRunner tests` | Unit tests |
| 6 | `docs: document -benchmark flag in README and runbook` | User-facing docs |
| 7 | `chore(release): bump version to 3.1.0` | `pom.xml` 3.1.0; tag `v3.1.0` |

## Exit criteria

- [ ] `-benchmark` prints one-liner + detail line
- [ ] Uses `SearchEngine` (not HTTP) for latency
- [ ] Reports parent doc count (`listLocations`), chunk count, index bytes
- [ ] Optional `-corpus` for corpus size
- [ ] `mvn test` green
- [ ] Tag `v3.1.0` pushed

## Example run

```bash
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-text input/ -index-dir data/index -benchmark \
    -benchmark-queries queries.txt -corpus input/ -iterations 100"
```
