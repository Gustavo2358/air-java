# AIR codec latency — DONE / MERGED

AIR PRs [#23](https://github.com/Gustavo2358/air-java/pull/23) and
[#24](https://github.com/Gustavo2358/air-java/pull/24) were approved and merged on
2026-10-01, preserving the implementation commits. The local-control transport
merge is `36826c55fb140f4ad81998161fe7f40acccf2b40`; the codec merge is
`e82211774019bc7d788860cfea05af76316414e8`.

## Implemented behavior

The decoder validates the complete physical JSON document into segmented UTF-8
offsets, shares field names per decode and binds AIR directly from byte slices.
Valid input no longer requires a complete UTF-16 document or a generic JSON
object tree. Physical failures use the reference parser to preserve diagnostics,
UTF-16 positions and precedence. Full physical checking still precedes binding.

`DecodeOptions.bindingParallelism` controls ordered block conversion. The default
is `min(4, Runtime.availableProcessors())`; 1 selects sequential binding.
Inventories smaller than 1,024 elements remain sequential. Each decode owns its
workers; nested mapping remains on its worker and results/failures are observed
in input order. This is scheduling policy, not an AIR capacity limit.

`AirValidator.check` creates an immutable `CheckedPublication` with a private
constructor and retains the exact publication, validation options and full
result. It can retain invalid or incomplete results and is not a validity
certificate. `decodeChecked` and `decodeCheckedForPartialAnalysis` apply the
existing strict or partial admission rules. Legacy methods remain available.
Consumers may reuse that run only for the same snapshot and validation options;
consumer-specific admission and capability checks still apply.

The architecture gate confines the CPU query to `DecodeOptions` and the reviewed
concurrency classes to `OrderedBlocks`. Process, filesystem and network APIs
remain outside the codec boundary. Model/validation keep their previous rules.

## Measured result and qualification

Java 21, Ryzen 5 5600GT, G1, `-Xmx2g`, populated filesystem cache; medians of three
fresh JVMs per variant/case, alternating runs without another benchmark/profiler.
For COACCT01 (264.2 MB), decode decreased **4.733 → 2.867 s (39.4%)**.
The paired complete dependency bundle decreased **16.559 → 6.553 s (60.4%)**;
that combined result also includes the consumer changes in
[CFG #58](https://github.com/Gustavo2358/analysis-cfg/pull/58). It is not a codec-only
speedup or a universal hardware guarantee. No compression or persistent cache was added.

Qualified AIR HEAD: `13518a709908eb3824ccd83d5c52d26cf83c1aac`.
[Final Fast CI](https://github.com/Gustavo2358/air-java/actions/runs/36874215719)
and local Fast passed: 189 model checks, 136 transport checks and harness tests.
The codec checks include 18,410 physical-parser comparisons; a separate 23,001-case
baseline/candidate comparison preserves canonical bytes or exact diagnostics.
The paired 560-case replay preserves 2,240 complete AIR, validation, CFG and
dependency products byte for byte. Correctness replay used `-Xmx3g`; timings used
`-Xmx2g`. The merge and documentation do not change product/test sources, so this
is reused qualification evidence, not a new corpus or benchmark execution.

The decoder still retains input, offsets and the AIR model. Encoding still uses
its intermediate binding tree. Memory is finite; operational limits and all
PARTIAL/INCOMPLETE statuses remain explicit. Raw logs, products and hashes remain
in workspace `.air-codec-latency-v2/evidence`; the integration record is in
`artefatos-e2e/analyzer-integration-20261001/REPORT.md`.
