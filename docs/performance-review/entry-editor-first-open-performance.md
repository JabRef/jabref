# Entry editor first-open performance

This report records the baseline Java Flight Recorder (JFR) evidence for the reported UI freeze when the entry editor is opened for the first time. It accompanies the raw recording so the evidence can be inspected again and compared with later implementations. The investigation is tracked in [JabRef pull request #16464](https://github.com/JabRef/jabref/pull/16464).

## Result

The recording supports eager field-editor construction followed by JavaFX CSS and layout work as the main source of the first-open stall.

- The sampled JavaFX application thread enters `FieldsEditorTab.createLabelAndEditor(...)` while opening the editor.
- Samples then remain in JavaFX CSS selector matching, style-map sorting, CSS reapplication, and recursive bounds calculation.
- The confirmed sample window from field-editor creation to the last CSS-pass sample spans 2.033 seconds.
- A broader window of JavaFX scene, CSS, and layout samples around the interaction spans 3.436 seconds.
- Garbage collection does not explain a multi-second freeze. The longest stop-the-world pause during the relevant window was 8.41 milliseconds.
- No `jdk.JavaMonitorEnter` events were recorded, so the capture does not indicate contended Java-monitor blocking.
- A supplemental run with a library containing linked PDF files reproduced the same JavaFX CSS, control-construction, and layout profile. It recorded no file or socket reads on the JavaFX application thread during the relevant window, so linked-file I/O is not supported as the cause of this occurrence.
- The supplemental run also recorded a burst of JavaFX CSS conversion exceptions. The looked-up-color failures are consistent with [OpenJFX pull request #2225](https://github.com/openjdk/jfx/pull/2225), and the entry-editor construction path initializes ControlsFX validation decoration for many field controls.
- A restored-editor startup run after preinstalling ControlsFX's `DecorationPane` constructs the editor during startup without the prior looked-up-color `ClassCastException` burst. It still performs substantial JavaFX CSS and layout work, so it is evidence that the root replacement moved off the later open-editor path, not a comparable click-to-render improvement measurement.

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
| Library | `../../jablib/src/test/resources/org/jabref/bibtexFiles/test.bib` |
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

### Supplemental linked-file run

A second cold-start run used a repository fixture whose six entries each link a local PDF. The first entry, `minimal-sentence-case`, was opened by double-clicking its row in the main table.

| Property | Value |
| --- | --- |
| Library | `../../jablib/src/test/resources/org/jabref/logic/search/test-library-with-attached-files.bib` |
| Source revision | `72a34bfaa1` |
| JavaFX | 26.0.2, macOS AArch64 |
| JFR settings | `profile`, stack depth 256, maximum size 250 MB |
| Recording duration | 59 seconds |
| Recording size | approximately 2.8 MB |

The application was launched with:

```bash
JDK_JAVA_OPTIONS='-XX:StartFlightRecording=name=entry,settings=profile,disk=true,maxsize=250m -XX:FlightRecorderOptions=stackdepth=256' \
  ./gradlew :jabgui:run \
  --args="/absolute/path/to/jabref/jablib/src/test/resources/org/jabref/logic/search/test-library-with-attached-files.bib"
```

The recording was dumped while JabRef was still running:

```bash
jcmd <pid> JFR.dump name=entry filename=/tmp/jabref-entry-editor-linked-cold.jfr
```

The supplemental recording was analyzed locally and is not part of the committed snapshots. The original recording above remains the reproducible artifact attached to this report.

### AppleScript interaction probe

The following accessibility script was used while attempting to automate the double-click and detect the editor toolbar:

```applescript
tell application "System Events"
    tell process "java"
        set frontmost to true
        click at {600, 300}
        delay 0.05
        click at {600, 300}
        repeat 400 times
            if (count of (UI elements of front window whose role is "AXToolbar")) > 1 then
                return "editor-visible"
            end if
            delay 0.025
        end repeat
        return "timeout"
    end tell
end tell
```

It was invoked as follows:

```bash
osascript <<'APPLESCRIPT'
tell application "System Events"
    tell process "java"
        set frontmost to true
        click at {600, 300}
        delay 0.05
        click at {600, 300}
        repeat 400 times
            if (count of (UI elements of front window whose role is "AXToolbar")) > 1 then
                return "editor-visible"
            end if
            delay 0.025
        end repeat
        return "timeout"
    end tell
end tell
APPLESCRIPT
```

This script did **not** produce a reliable JavaFX double-click. It selected the entry cell in the main table, and the entry editor was subsequently opened by a manual double-click while JFR continued recording. The script's approximately 10.8-second wall-clock result therefore includes the wait for that manual action and is invalid as click-to-render latency. A future automation must emit a verified double-click on the main-table row and record a custom input marker before its timing can be used.

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

### Supplemental interaction evidence

The manual double-click in the linked-file run was followed by JavaFX application-thread work from approximately 18:02:08.515 until layout settled around 18:02:10.280. This approximately 1.765-second interval is another statistical evidence window, not exact input-to-render latency, because the recording has no custom input marker.

During that interval, the JavaFX application thread recorded:

| Evidence | Result |
| --- | ---: |
| Java exceptions | 160 |
| `ClassCastException`, predominantly CSS `String` to `Paint`, `Color`, or `ParsedValue` conversions | 90 |
| `MalformedURLException`, predominantly JavaFX CSS parsing of `HAND`, `TEXT`, and `DEFAULT` | 58 |
| `NoSuchMethodError` from method-handle linkage | 6 |
| `IllegalArgumentException` for invalid color specifications | 5 |
| `FileNotFoundException` for a JAR entry | 1 |
| Weighted sampled allocation | approximately 130 MB |
| Garbage-collection pause | 10.593 ms |
| `jdk.FileRead` on the JavaFX application thread | 0 |
| `jdk.SocketRead` on the JavaFX application thread | 0 |

The allocation samples were dominated by JavaFX CSS state and matching structures, including `PseudoClassState` arrays and objects, byte and long arrays, and immutable-set iterators. Execution samples were dominated by `SelectorPartitioning.match(...)`, `StyleManager.findMatchingStyles(...)`, `CssStyleHelper.createStyleHelper(...)`, CSS state transitions, `Node.reapplyCss()`, and subsequent `VirtualFlow` and `TableView` layout.

The absence of recorded UI-thread file reads does not prove that no filesystem metadata check occurred, but the profile contains no evidence that resolving or reading an attached PDF caused this stall. The dominant sampled work remains field-control creation, CSS, and layout.

### ControlsFX-decoration startup experiment

ControlsFX silently installs its internal `DecorationPane` as the scene root when the first validation decoration is added. `JabRefGUI` now performs this installation with a temporary `GraphicDecoration` immediately after creating the scene, while the `PowerPane` is the root. The temporary decoration is then removed; the `DecorationPane` remains around the `PowerPane` and later validations do not need to replace the scene root.

A JFR run then launched the linked-file fixture after the entry editor had been open in the preceding session. The editor was constructed during startup without an input command: `EntryEditor.<init>` was sampled at 20:51:04.050 and `EntryEditorViewModel.rebuildTabs()` at 20:51:04.670 (Europe/Berlin). The JavaFX application thread subsequently sampled CSS selector matching, style-map creation, CSS transitions, and layout until approximately 20:51:11.140.

No `ClassCastException` events were recorded on the JavaFX application thread between 20:51:03 and 20:51:12, unlike the 90 looked-up-color conversion exceptions in the comparable-sized linked-file interaction window above. This supports the hypothesis that eager `DecorationPane` installation avoids that exception path.

This is deliberately not reported as a speedup: the recording overlaps application and library startup, uses editor restoration rather than a verified table interaction, and has no click-to-render marker. The remaining startup CSS and layout activity also shows that eager field-editor construction remains material work. A controlled post-change capture with the original library, entry selection, and an input marker is still required for a direct before/after latency comparison.

## Relevant code path

The direct application frame in the execution sample is:

```text
FieldsEditorTab.createLabelAndEditor(...)
FieldsEditorTab.setupPanel(...)
FieldsEditorTab.bindToEntry(...)
```

`FieldsEditorTab.setupPanel(...)` determines every field, then maps every field through `createLabelAndEditor(...)` before layout. Each call creates a `FieldEditorFX`, binds it to the entry, stores it, and creates a `FieldNameLabel`. Adding the resulting control tree triggers the CSS, bounds, and rendering work visible in subsequent samples.

The entry editor also constructs all configured tabs in `EntryEditorViewModel.rebuildTabs()`. That can contribute to initial work, but the strongest direct evidence in this recording is the selected fields tab creating and laying out its editors.

The current main tab also eagerly creates controls that are not immediately used:

- `AllFieldsTab.createSectionPane(...)` builds add-field chip buttons even for sections that start collapsed.
- `AllFieldsTab.createFreeFormAddRow(...)` fills the editable field-name combo box with every known non-internal field during panel construction.

The release tag `v6.0-alpha.5` predates both the June 2026 MVVM entry-editor refactor and the July 2026 new entry editor. Commit `6c67e6f99b` (`New entry editor (#16166)`) is therefore a useful regression boundary to test, particularly because it introduced the current all-fields main tab. This source-history observation narrows the A/B candidates; it does not by itself identify the cause.

## CSS looked-up-color failures

The supplemental profile and terminal output show repeated JavaFX CSS conversion failures during entry-editor construction. [OpenJFX pull request #2225](https://github.com/openjdk/jfx/pull/2225) fixes looked-up colors that fail when used for `-fx-background-color` in a deeply nested, optionally unstyled scene graph. That description and its affected CSS code match the observed `CssStyleHelper`, selector-matching, and invalid-color activity.

There is also a direct entry-editor path to ControlsFX decoration:

```text
field editor construction
EditorValidator.configureValidation(...)
MVVMFX ControlsFxVisualizer
JabRef IconValidationDecorator
ControlsFX GraphicValidationDecoration
JavaFX CSS processing
```

This makes ControlsFX validation decoration a plausible trigger for the looked-up-color issue when many editors are created together. OpenJFX #2225 is the likely JavaFX-side fix, but an A/B run with that patch, or with validation decoration disabled, is still needed to prove how much of the first-open latency and exception burst it removes.

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
7. Build the contents and add-field chips of initially collapsed sections only when they are first expanded.
8. Populate the free-form field-name combo box when it is first opened instead of during panel construction.

Lazy tab creation is a secondary opportunity, especially for tabs that are configured but not selected. It does not replace incremental creation in the initially selected fields tab.

Separately, test a JavaFX build containing OpenJFX #2225 and compare the CSS exception count and JavaFX application-thread work window. Prewarming the editor may hide the cold-path delay, but reducing the initial scene graph and removing the CSS failure path address the recorded work directly.

## Validation plan

Retain this recording as the before snapshot and capture an equivalent after snapshot with the same library, citation key, JVM, and JFR settings.

The target outcomes are:

- no single multi-second JavaFX application-thread work window after opening the editor;
- a shorter field-creation-to-final-CSS sample window;
- no regression in focus restoration, jump-to-field behavior, or field visibility;
- GC pauses remaining insignificant relative to UI work.
- no burst of looked-up-color conversion exceptions after the JavaFX fix;
- unchanged results when the linked-file fixture is compared with an equivalent entry without attachments.

A JMH benchmark can measure isolated field-editor creation and binding, including cold and warm cases. It cannot fully measure JavaFX pulse, CSS, and rendering latency by itself. An automated JavaFX integration measurement should therefore complement JMH for the end-to-end open-editor latency.

<!-- markdownlint-disable-file MD033 MD041 -->
