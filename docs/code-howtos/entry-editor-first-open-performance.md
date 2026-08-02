> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

# Entry editor first-open performance

This report records the baseline Java Flight Recorder (JFR) evidence for the reported UI freeze when the entry editor is opened for the first time. It accompanies the raw recording so the evidence can be inspected again and compared with later implementations.

## Result

The recording supports eager field-editor construction followed by JavaFX CSS and layout work as the main source of the first-open stall.

- The sampled JavaFX application thread enters `FieldsEditorTab.createLabelAndEditor(...)` while opening the editor.
- Samples then remain in JavaFX CSS selector matching, style-map sorting, CSS reapplication, and recursive bounds calculation.
- The confirmed sample window from field-editor creation to the last CSS-pass sample spans 2.033 seconds.
- A broader window of JavaFX scene, CSS, and layout samples around the interaction spans 3.436 seconds.
- Garbage collection does not explain a multi-second freeze. The longest stop-the-world pause during the relevant window was 8.41 milliseconds.
- No `jdk.JavaMonitorEnter` events were recorded, so the capture does not indicate contended Java-monitor blocking.

These spans are evidence windows between statistical samples, not exact method durations. The recording did not include a custom event marking the menu action or JavaFX pulse-duration events, so it cannot provide an exact click-to-first-render latency.

## Recording

The complete recording is stored at:

- [entry-editor-first-open-before-lazy-loading.jfr](snapshots/entry-editor-first-open-before-lazy-loading.jfr)

Artifact details:

| Property | Value |
| --- | --- |
| Format | JFR 2.1 |
| Size | 2,592,250 bytes |
| SHA-256 | `519c927a9ac515003cfefcb531b7f98ff8e189c1cfed7f3eafe929e5736d4c2e` |
| Start | 2026-08-02 15:12:32 UTC / 17:12:32 Europe/Berlin |
| Duration | 34 seconds |
| Chunks | 1 |
| Settings | JFR `profile` |
| Maximum recording size | 256 MB |
| Source revision | `e868904e29aee6871242c9135e2e8c7264675a66` |
| JabRef version | `100.0.0` |

### Loading the recording

Open the `.jfr` file in one of the following tools:

- JDK Mission Control: **File → Open File**
- IntelliJ IDEA: open the `.jfr` file as a profiler snapshot
- JDK command line: `jfr summary <file>` or `jfr print <file>`

Verify the artifact before comparing results:

```bash
shasum -a 256 docs/code-howtos/snapshots/entry-editor-first-open-before-lazy-loading.jfr
```

## Reproduction scenario

The final capture used a known repository test library and selected a known citation key through the CLI before opening the editor.

| Input | Value |
| --- | --- |
| Library | `jablib/src/test/resources/org/jabref/bibtexFiles/test.bib` |
| Selected citation key | `1102917` |
| Editor command | **View → Open entry editor** (`Ansicht → Eintragseditor öffnen`) |
| Preference state | Reset to defaults before the final capture |

The application was launched with:

```bash
./gradlew --no-daemon :jabgui:run \
  --args="/absolute/path/to/jabref/jablib/src/test/resources/org/jabref/bibtexFiles/test.bib --jumpToKey=1102917" \
  --console=plain
```

The `:jabgui:run` task received this additional JVM option through a temporary Gradle init script:

```text
-XX:StartFlightRecording=filename=/tmp/entry-editor.jfr,settings=profile,dumponexit=true,maxsize=256m
```

The recording was dumped after the entry editor had opened:

```bash
jcmd <pid> JFR.dump name=1 filename=entry-editor-first-open-before-lazy-loading.jfr
```

## Environment

| Property | Value |
| --- | --- |
| Operating system | Darwin 24.6.0, ARM64 |
| CPU | AArch64, 10 cores, 10 hardware threads |
| JVM | OpenJDK 64-Bit Server VM 25.0.2+10-LTS |
| JavaFX | 26.0.1, macOS AArch64 |
| Garbage collector | G1 |
| Initial heap | 256 MB |
| Maximum heap | 4 GB |
| Compact object headers | Enabled |
| String deduplication | Enabled |

## Recording overview

The most relevant recorded event counts are:

| Event | Count |
| --- | ---: |
| `jdk.ExecutionSample` | 235 |
| `jdk.NativeMethodSample` | 1,542 |
| `jdk.ObjectAllocationSample` | 1,149 |
| `jdk.GarbageCollection` | 17 |
| `jdk.ThreadPark` | 212 |
| `jdk.JavaMonitorEnter` | 0 |
| `javafx.Input` | 0 |
| `javafx.PulsePhase` | 0 |

The absence of JavaFX input and pulse-phase events is an important limitation: the snapshot is suitable for locating CPU and allocation work, but not for computing an exact input-to-render latency.

## Interaction timeline

The timestamps below are shown in Europe/Berlin local time. Thread ID 48 is named `Thread-5` in the execution samples and appears as `JavaFX Application Thread` with the same Java thread ID in the final thread dump.

| Time | Thread | Sampled work |
| --- | --- | --- |
| 17:13:02.724 | JavaFX application thread | `Scene.preferredSize()` and scene-size invalidation |
| 17:13:02.754 | JavaFX application thread | CSS style-cache lookup |
| 17:13:03.329 | JavaFX application thread | `StyleManager.stylesheetsChanged(...)` |
| 17:13:04.115 | JavaFX application thread | Text-field behavior and scene-listener changes |
| 17:13:04.127 | JavaFX application thread | `FieldsEditorTab.createLabelAndEditor(...)` |
| 17:13:04.148 | JavaFX application thread | CSS transition and cached-font lookup |
| 17:13:04.195 | JavaFX application thread | `Node.doProcessCSS()` |
| 17:13:04.207 | Quantum renderer | Rendering the newly created scene graph |
| 17:13:04.714 | JavaFX application thread | CSS selector matching |
| 17:13:04.736 | JavaFX application thread | Sorting JavaFX style data |
| 17:13:05.065 | JavaFX application thread | Recursive `Parent.updateBounds()` |
| 17:13:05.144 | JavaFX application thread | `StyleMap` sorting and merging |
| 17:13:05.168 | JavaFX application thread | Recursive node and parent bounds updates |
| 17:13:05.558 | JavaFX application thread | Popup and node bounds calculation |
| 17:13:05.583 | JavaFX application thread | Recursive `Parent.updateBounds()` |
| 17:13:05.593 | JavaFX application thread | Recursive `Parent.updateBounds()` |
| 17:13:05.629 | JavaFX application thread | `Node.reapplyCss()` |
| 17:13:06.125 | JavaFX application thread | CSS selector matching and style-map lookup |
| 17:13:06.160 | JavaFX application thread | `Scene.doCSSPass()` during a pulse |

The 2.033-second confirmed evidence window is the difference between the first direct `FieldsEditorTab` sample at 17:13:04.127 and the final CSS-pass sample at 17:13:06.160. The 3.436-second broader window begins with scene-size work at 17:13:02.724. Sampling gaps mean neither number should be interpreted as continuous CPU time.

## Relevant code path

The direct application frame in the execution sample is:

```text
FieldsEditorTab.createLabelAndEditor(...)
FieldsEditorTab.setupPanel(...)
FieldsEditorTab.bindToEntry(...)
```

`FieldsEditorTab.setupPanel(...)` determines every field, then maps every field through `createLabelAndEditor(...)` before layout. Each call creates a `FieldEditorFX`, binds it to the entry, stores it, and creates a `FieldNameLabel`. Adding the resulting control tree triggers the CSS, bounds, and rendering work visible in subsequent samples.

The entry editor also constructs all configured tabs in `EntryEditorViewModel.rebuildTabs()`. That can contribute to initial work, but the strongest direct evidence in this recording is the selected fields tab creating and laying out its editors.

## Garbage collection

The GC events overlapping the end of the interaction were:

| Time | Event | Total duration | Sum of pauses | Longest pause |
| --- | --- | ---: | ---: | ---: |
| 17:13:05.610 | G1 young collection | 7.05 ms | 7.05 ms | 7.05 ms |
| 17:13:06.149 | G1 young collection | 5.32 ms | 5.32 ms | 5.32 ms |
| 17:13:06.154 | G1 old collection | 61.9 ms | 8.43 ms | 8.41 ms |

The 61.9-millisecond old-collection duration includes concurrent work. Its longest application pause was 8.41 milliseconds. GC therefore cannot account for the observed multi-second JavaFX work window.

## Other concurrent work

The recording contains network, TLS, cryptography, and JSON samples on `pool-3-thread-1` during the editor interaction. These samples are on a background thread and are not the direct UI-thread stall. They may compete for CPU, but the machine had ten hardware threads and the JavaFX application thread independently shows sustained scene-graph work.

## Best-supported improvement

Create field editors incrementally instead of constructing the complete field-control tree during the first bind.

A safe implementation should:

1. Create enough rows to render an immediately usable first editor.
2. Create additional rows in bounded batches between JavaFX pulses.
3. Cancel or invalidate queued batches when the selected entry changes.
4. Immediately materialize a requested field for jump-to-field and focus restoration.
5. Avoid repeatedly recreating controls that were already bound.
6. Keep all JavaFX node construction on the JavaFX application thread.

Lazy tab creation is a secondary opportunity, especially for tabs that are configured but not selected. It does not replace incremental creation in the initially selected fields tab.

## Validation plan

Retain this recording as the before snapshot and capture an equivalent after snapshot with the same library, citation key, JVM, and JFR settings.

The target outcomes are:

- no single multi-second JavaFX application-thread work window after opening the editor;
- a shorter field-creation-to-final-CSS sample window;
- no regression in focus restoration, jump-to-field behavior, or field visibility;
- GC pauses remaining insignificant relative to UI work.

A JMH benchmark can measure isolated field-editor creation and binding, including cold and warm cases. It cannot fully measure JavaFX pulse, CSS, and rendering latency by itself. An automated JavaFX integration measurement should therefore complement JMH for the end-to-end open-editor latency.

<!-- markdownlint-disable-file MD033 MD041 -->
