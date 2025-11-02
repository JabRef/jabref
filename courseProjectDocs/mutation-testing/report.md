# Mutation Overview
## 1. Mutation Testing Setup

**Tool Used:** [PIT (Pitest)](https://pitest.org) – a mutation testing framework for Java integrated via Gradle.  
**Configuration:** Added the PIT plugin to `build.gradle.kts` within the `jablib` module.  
Configured target classes, test sources, and report output directory. Example configuration snippet:

```kotlin
plugins {
    id("info.solidsoft.pitest") version "1.19.0"
}

pitest {
    pitestVersion.set("1.19.0")
    targetClasses.set(listOf("org.jabref.logic.util.*")) // mutate only util package
    targetTests.set(listOf("org.jabref.logic.util.*Test"))
}
```
**Report Location:** `jablib/build/reports/pitest/index.html`  

To generate a mutation report, run:
```bash
./gradlew :jablib:pitest
```
| Member   | Task / Contribution                                                                                                                                   | Notes                                                          |
|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| Geoffrey | Fixed failing unit tests in `PdfImporterMockTest`, configured PIT mutation testing, and added new mutation-killing tests for `QuotedStringTokenizer`. | Increased test coverage by 2% and killed 24 surviving mutants. |
| Lucille  | X                                                                                                                                                     | X                                                              |
| Vanessa  | X                                                                                                                                                     | X                                                              |

# 2. Mutations Before

## org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator
### Generated 42 Killed 21 (50%)  
KILLED 21 SURVIVED 11 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 10  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator
### Generated 20 Killed 13 (65%)  
KILLED 13 SURVIVED 0 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 7  
---
## org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator
### Generated 2 Killed 1 (50%)
KILLED 1 SURVIVED 0 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 1  
---
## org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator
### Generated 284 Killed 8 (3%)
KILLED 8 SURVIVED 4 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 272  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanTrueReturnValsMutator
### Generated 55 Killed 24 (44%)
KILLED 24 SURVIVED 5 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 26  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator
### Generated 109 Killed 30 (28%)
KILLED 30 SURVIVED 2 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 77  
---
## org.pitest.mutationtest.engine.gregor.mutators.MathMutator
### Generated 48 Killed 32 (67%)
KILLED 32 SURVIVED 2 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 14  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator
### Generated 99 Killed 42 (42%)
KILLED 42 SURVIVED 5 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 52  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanFalseReturnValsMutator
### Generated 35 Killed 17 (49%)
KILLED 17 SURVIVED 3 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 15  
---
## org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator
### Generated 224 Killed 149 (67%)
KILLED 148 SURVIVED 11 TIMED_OUT 1 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 64  
---
## Timing
pre-scan for mutations : `2 seconds`  
scan classpath : `< 1 second`  
coverage and dependency analysis : `21 seconds`  
build mutation tests : `1 seconds`  
run mutation analysis : `2 minutes and 3 seconds`  
Total : `2 minutes and 28 seconds`
---
## Statistics
Line Coverage (for mutated classes only): 660/1568 (42%)  
16 tests examined  
Generated 918 mutations Killed 337 (37%)  
Mutations with no coverage 538. Test strength 89%  
Ran 700 tests (0.76 tests per mutation)

# 3. Mutations After QuotedStringTokenizerTest.java

## org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator
### Generated 42 Killed 26 (62%)
KILLED 26 SURVIVED 11 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 5  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator
### Generated 20 Killed 13 (65%)
KILLED 13 SURVIVED 0 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 7  
---
## org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator
### Generated 2 Killed 1 (50%)
KILLED 1 SURVIVED 0 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 1  
---
## org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator
### Generated 284 Killed 8 (3%)
KILLED 8 SURVIVED 4 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 272  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanTrueReturnValsMutator
### Generated 55 Killed 26 (47%)
KILLED 26 SURVIVED 5 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 24  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator
### Generated 109 Killed 30 (28%)
KILLED 30 SURVIVED 2 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 77  
---
## org.pitest.mutationtest.engine.gregor.mutators.MathMutator
### Generated 48 Killed 36 (75%)
KILLED 35 SURVIVED 2 TIMED_OUT 1 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 10  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator
### Generated 99 Killed 44 (44%)
KILLED 44 SURVIVED 5 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 50  
---
## org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanFalseReturnValsMutator
### Generated 35 Killed 17 (49%)
KILLED 17 SURVIVED 3 TIMED_OUT 0 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 15  
---
## org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator
### Generated 227 Killed 160 (70%)
KILLED 159 SURVIVED 11 TIMED_OUT 1 NON_VIABLE 0  
MEMORY_ERROR 0 NOT_STARTED 0 STARTED 0 RUN_ERROR 0  
NO_COVERAGE 56  
---
## Timings
pre-scan for mutations : `< 1 second`  
scan classpath : < `1 second`  
coverage and dependency analysis : `7 seconds`  
build mutation tests : `< 1 second`  
run mutation analysis : `56 seconds`  
Total : `1 minutes and 4 seconds`  
---
## Statistics
Line Coverage (for mutated classes only): 687/1571 (44%)  
17 tests examined  
Generated 921 mutations Killed 361 (39%)  
Mutations with no coverage 517. Test strength 89%  
Ran 750 tests (0.81 tests per mutation)  
