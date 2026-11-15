---
parent: Code Howtos
---
# Profiling Gradle test memory with JFR

Use this workflow to investigate test-worker heap exhaustion, excessive allocation, or objects retained after tests finish.
The examples use a POSIX shell, a HotSpot-based JDK 25 or later, and JabRef's Gradle wrapper.
Replace the task and fully qualified test class to use it in another Gradle project.

## Choose the measurement

| Question | Evidence |
| --- | --- |
| Which code allocates heavily? | JFR allocation samples and their call stacks |
| How much heap survives collection? | GC observations and a live heap dump at a defined point |
| What keeps those objects alive? | Heap dominators and paths to GC roots in VisualVM |
| Why does the whole suite fail when one class passes? | The original worker batch, test order, parallelism, and process reuse |

The allocation site in an `OutOfMemoryError` identifies the allocation that failed.
Use retention evidence to identify what consumed the available heap.
High allocation volume alone does not establish a leak, and one reachable-object snapshot does not establish unbounded growth.

## Prepare an isolated recording directory

Run from the repository root, using the same shell for the following commands:

```shell
TEST_PROFILE_DIR=$(mktemp -d)
TEST_PROFILE_TASK=:jablib:test
TEST_PROFILE_CLASS=org.jabref.logic.journals.AbbreviationsTest

jfr configure --input profile.jfc \
  allocation-profiling=maximum memory-leaks=gc-roots gc=detailed \
  --output "$TEST_PROFILE_DIR/heap.jfc"
```

Use a fresh directory for each variant.
Choose a path without spaces, commas, or colons for these shell examples and JVM logging options.
Keep the failing worker's heap limit; increasing it changes the reproduction.
The detailed recording settings are for diagnosis and affect execution time.

## Configure the test JVM

Gradle runs tests in separate JVMs.
Flags in `org.gradle.jvmargs` configure the build daemon, while `Test.jvmArgumentProviders` supplies flags to test workers.
Keep existing module flags and agents, including Mockito's agent.
See Gradle's [test JVM configuration](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html).

Save the following as `$TEST_PROFILE_DIR/profile-tests.init.gradle`:

```groovy
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider

gradle.taskGraph.whenReady { graph ->
    def options = gradle.startParameter.projectProperties
    graph.allTasks.findAll {
        it instanceof Test && it.path == options['testProfileTask']
    }.each { testTask ->
        def directory = new File(options['testProfileDir']).absoluteFile
        def settings = new File(directory, 'heap.jfc')
        def heapAgent = new File(directory, 'heap-on-exit.jar')
        testTask.maxHeapSize = options.getOrDefault('testProfileHeap', '512m')
        testTask.maxParallelForks = options.getOrDefault('testProfileForks', '1').toInteger()
        testTask.systemProperty('test.profile.class', options['testProfileClass'])
        testTask.jvmArgumentProviders.add({ ->
            def arguments = [
                "-XX:StartFlightRecording=name=tests,settings=${settings},filename=${directory}/tests-%p.jfr,dumponexit=true,path-to-gc-roots=true",
                '-XX:FlightRecorderOptions=stackdepth=128',
                '-XX:+HeapDumpOnOutOfMemoryError',
                "-XX:HeapDumpPath=${directory}",
                "-Xlog:gc*,gc+heap=debug:file=${directory}/gc-%p.log:time,uptimemillis,level,tags"
            ]
            if (heapAgent.isFile()) {
                arguments.add("-javaagent:${heapAgent}=${directory}")
            }
            arguments
        } as CommandLineArgumentProvider)
        testTask.doFirst {
            new File(directory, 'configuration.txt').text = [
                "task=${testTask.path}",
                "heap=${testTask.maxHeapSize}",
                "forks=${testTask.maxParallelForks}",
                "forkEvery=${testTask.forkEvery}",
                "java=${testTask.javaLauncher.get().executablePath.asFile}",
                "testClass=${options['testProfileClass']}"
            ].join('\n') + '\n'
        }
    }
}
```

This temporary script applies settings to the selected task after project configuration, including in builds with included builds.
It records the effective settings and preserves `forkEvery`.
The task-graph callback requires disabling the configuration cache for this run.
The optional heap agent is described below; the basic recording does not require it.

`%p` gives each worker its own recording and GC log.
A directory for `HeapDumpPath` lets HotSpot choose a process-specific OOM dump filename.
These options are documented in the [JDK launcher reference](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html).

## Run the selected tests

```shell
./gradlew --no-configuration-cache \
  --init-script "$TEST_PROFILE_DIR/profile-tests.init.gradle" \
  "-PtestProfileDir=$TEST_PROFILE_DIR" \
  "-PtestProfileTask=$TEST_PROFILE_TASK" \
  "-PtestProfileClass=$TEST_PROFILE_CLASS" \
  -PtestProfileHeap=512m -PtestProfileForks=1 \
  "$TEST_PROFILE_TASK" --tests "$TEST_PROFILE_CLASS" --rerun
```

Coding agents must pass this underlying Gradle command through their configured managed Gradle execution wrapper, when required by their environment.

Use the fully qualified class name to avoid matching unrelated classes with the same suffix.
`--rerun` forces the selected task to execute, so an up-to-date result or build-cache hit cannot substitute for a recording.
For a suite-only failure, run the relevant batch or full task without `--tests`, matching the original fork count and `forkEvery`.
Changing process reuse changes how long retained fixtures can accumulate; see Gradle's [test execution controls](https://docs.gradle.org/current/userguide/java_testing.html).

Check `configuration.txt` and the selected module's XML test reports for the expected JVM, heap, fork settings, test count, failures, and skips.
Copy the reports into the recording directory before another run overwrites them.
For the example above:

```shell
cp -R jablib/build/test-results/test "$TEST_PROFILE_DIR/test-results"
```

There can be several recordings, including short-lived discovery workers.
Identify the worker that ran the selected tests using its JVM information and recorded execution stacks.
A worker that merely loaded the test class during discovery is not enough to establish that it executed the workload.

## Analyze allocations with JFR

Set `TEST_PROFILE_RECORDING` to the relevant worker recording:

```shell
TEST_PROFILE_RECORDING="$TEST_PROFILE_DIR/tests-12345.jfr"

jfr summary "$TEST_PROFILE_RECORDING"
jfr view jvm-information "$TEST_PROFILE_RECORDING"
jfr view --width 160 allocation-by-class "$TEST_PROFILE_RECORDING"
jfr view --width 160 allocation-by-site "$TEST_PROFILE_RECORDING"
jfr view gc "$TEST_PROFILE_RECORDING"
jfr view memory-leaks-by-class "$TEST_PROFILE_RECORDING"
```

The PID above is an example; select a file from your recording directory.
The [JFR command reference](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jfr.html) describes these analysis commands.
Open the recording in JDK Mission Control when an interactive call-tree view is useful.

Allocation by site often points to general-purpose code such as deserialization or collection growth.
Follow its caller stack before attributing that cost to a subsystem.
For custom analysis, stream `jdk.ObjectAllocationSample` events with `jdk.jfr.consumer.RecordingFile`, sum each event's `weight`, and count an event once when any caller frame matches the chosen subsystem.
Do not count matching frames as separate allocations or treat the number of samples as a byte total.
Report weighted totals as estimates and record the caller/method filter used for attribution.

To inspect selected events with deeper stacks:

```shell
jfr print --stack-depth 128 \
  --events jdk.ObjectAllocationSample,jdk.OldObjectSample \
  "$TEST_PROFILE_RECORDING" > "$TEST_PROFILE_DIR/allocation-and-retention.txt"
```

An absent old-object sample does not rule out retained objects.
Use a heap dump to establish ownership.

### Calculate sampled bytes for a caller

For a repeatable caller filter, save this JDK-only example as `$TEST_PROFILE_DIR/AllocationTotals.java`:

```java
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import jdk.jfr.consumer.RecordingFile;

public class AllocationTotals {
    public static void main(String[] args) throws IOException {
        long total = 0;
        long attributed = 0;
        try (var recording = new RecordingFile(Path.of(args[0]))) {
            while (recording.hasMoreEvents()) {
                var event = recording.readEvent();
                if (!event.getEventType().getName().equals("jdk.ObjectAllocationSample")) {
                    continue;
                }
                long weight = event.getLong("weight");
                total += weight;
                boolean matches = Optional.ofNullable(event.getStackTrace()).stream()
                                          .flatMap(stack -> stack.getFrames().stream())
                                          .anyMatch(frame -> frame.getMethod().getType().getName().equals(args[1])
                                                  && frame.getMethod().getName().equals(args[2]));
                if (matches) {
                    attributed += weight;
                }
            }
        }
        System.out.printf("Sampled bytes: total=%d, attributed=%d, fraction=%.2f%%%n",
                total, attributed, total == 0 ? 0 : 100.0 * attributed / total);
    }
}
```

For example, attribute samples whose stacks contain the LTWA repository constructor:

```shell
java "$TEST_PROFILE_DIR/AllocationTotals.java" "$TEST_PROFILE_RECORDING" \
  org.jabref.logic.journals.ltwa.LtwaRepository '<init>'
```

This counts each sampled allocation once even if the selected method appears repeatedly in its stack.
Constructor attribution excludes separate file-copy work outside that constructor and can miss callers beyond the recorded stack depth.
Keep the exact same filter when comparing variants.

## Capture a live heap

For a sufficiently long-running worker, use its PID from `jcmd -l`:

```shell
jcmd -l
TEST_PROFILE_PID=12345
jcmd "$TEST_PROFILE_PID" GC.class_histogram > "$TEST_PROFILE_DIR/histogram.txt"
jcmd "$TEST_PROFILE_PID" GC.heap_dump "$TEST_PROFILE_DIR/live-$TEST_PROFILE_PID.hprof"
```

Choose the Gradle test worker rather than the daemon.
`GC.heap_dump` requests a full GC by default; `-all` also includes unreachable objects and changes the comparison.
The [diagnostic command reference](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jcmd.html) documents both commands.
Record whether the capture happened during a test, after a batch, or at worker shutdown.
An OOM dump and a successful worker's post-GC dump capture different states.

### Capture after short-lived tests finish

For repeatable worker-exit snapshots, save this diagnostic agent as `$TEST_PROFILE_DIR/TestHeapOnExit.java`.
It uses only JDK classes and captures workers that loaded the selected class; still verify which workers executed the tests.

```java
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Arrays;

import com.sun.management.HotSpotDiagnosticMXBean;

public class TestHeapOnExit {
    public static void premain(String directory, Instrumentation instrumentation) {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("test-heap-dump").unstarted(() -> {
            String selectedClass = System.getProperty("test.profile.class");
            boolean selectedWorker = Arrays.stream(instrumentation.getAllLoadedClasses())
                                           .anyMatch(type -> type.getName().equals(selectedClass));
            if (!selectedWorker) {
                return;
            }
            Path output = Path.of(directory).resolve("live-" + ProcessHandle.current().pid() + ".hprof");
            try {
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                                 .dumpHeap(output.toString(), true);
            } catch (IOException exception) {
                System.getLogger(TestHeapOnExit.class.getName())
                      .log(System.Logger.Level.ERROR, "Could not capture the test heap", exception);
            }
        }));
    }
}
```

Compile the agent before running Gradle; the init script automatically adds the resulting JAR:

```shell
mkdir -p "$TEST_PROFILE_DIR/agent-classes"
javac -d "$TEST_PROFILE_DIR/agent-classes" "$TEST_PROFILE_DIR/TestHeapOnExit.java"
printf 'Premain-Class: TestHeapOnExit\n' > "$TEST_PROFILE_DIR/agent-manifest.mf"
jar --create --file "$TEST_PROFILE_DIR/heap-on-exit.jar" \
  --manifest "$TEST_PROFILE_DIR/agent-manifest.mf" \
  -C "$TEST_PROFILE_DIR/agent-classes" .
```

The [`dumpHeap` live option](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.management/com/sun/management/HotSpotDiagnosticMXBean.html#dumpHeap(java.lang.String,boolean)) includes reachable objects.
The dump path must not already exist.
A successful test run does not guarantee capture succeeded: verify the expected `.hprof` exists and can be opened.
Shutdown snapshots include shutdown-time roots and do not replace an in-test snapshot when object lifetime during a test is the question.

Let the JVM's `dumponexit=true` save JFR.
Calling `Recording.dump()` from your own shutdown hook can race JFR's shutdown and fail with `No chunks`.
If the worker may be forcibly killed, dump JFR while it is still running:

```shell
jcmd "$TEST_PROFILE_PID" JFR.dump name=tests \
  "filename=$TEST_PROFILE_DIR/running-$TEST_PROFILE_PID.jfr" path-to-gc-roots=true
```

## Find retaining references in VisualVM

Open the `.hprof` in VisualVM and inspect the heap summary, class instance counts, and dominators by retained size.
Follow paths to GC roots from a large retained graph.
A dominator identifies ownership of reachable memory; an allocation hotspot identifies where memory was created.
Shallow size counts an object itself; retained size also counts the objects that would become collectible with it.
Do not add the retained sizes of nested owners, because their graphs overlap.

In the journal tests, the retaining path was:

```text
Mockito advice → mock map → invocation/stubbing history
  → background task → journal repository → LTWA trees
```

Concrete queued executors removed this retention.
Separately, sharing read-only LTWA data removed repeated construction.
The intentional cache retains one dataset, so it uses more live heap than the fixture-only fix while allocating far less during the tests.

Use VisualVM's reported live and retained byte counts, not the `.hprof` file size.
If using VisualVM's heap-analysis library in another process, use a separate copy or hard link of the dump: concurrent analysis of the same path can collide in its `.hwcache` directory.

## Compare and report results

Keep the test set, inputs, JDK, GC, heap limit, fork settings, recording settings, and capture point consistent.
Change one implementation variable at a time.
Run added regression tests separately when preserving an existing workload for profiling.
Verify the fix against the full worker batch or suite that originally failed.
Profiling timings are diagnostic observations, not benchmark results.

Record these fields in the PR or investigation notes:

| Field | What to record |
| --- | --- |
| Reproduction | Task, exact test filter, commit/variant, test count, failures and skips |
| Runtime | Actual test JDK/vendor, heap limit, GC, forks and `forkEvery` |
| Recording | JFC settings, stack depth, worker PID, capture point and whether GC was requested |
| Allocations | Weighted estimated bytes, caller filter and major allocation sites |
| Retention | Live bytes, important instance counts, retained bytes and the GC-root path |
| Result | Which observation changed after the fix and which costs remain |

State the units explicitly: allocation estimates in GB and live heap in MiB need different byte conversions.
Keep raw recordings, heap dumps, GC logs, and temporary agents outside the repository.
Put the measurements and explanation in the PR; review captures before sharing them because they contain runtime data and local paths.
