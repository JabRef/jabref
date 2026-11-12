---
nav_order: 81
parent: Decision Records
status: accepted
date: 2026-11-04
---
# Open long Windows paths with the URL protocol handler
`adr~open-long-windows-paths-with-the-url-protocol-handler~1`

Needs: impl

## Context and Problem Statement

On Windows, JabRef opens a linked file by starting `explorer.exe` with the file as its argument, and reveals a file with `explorer.exe /select,`.
Explorer truncates that argument at `MAX_PATH` (260 characters, the terminating NUL included), so from 260 characters on the argument names nothing and Explorer silently shows the user's Documents folder instead.
Nothing fails and nothing is logged: the user clicks the PDF icon and gets the wrong window.

JabRef reaches that length on its own.
The default file name pattern builds names from author, year and title, and a library stored a few directories deep passes 260 characters without the user doing anything unusual — in the library that uncovered this, 50 of 269 linked PDFs are affected.

[Microsoft's documentation on the path length limitation](https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation) describes the opt-in out of that limit: the `LongPathsEnabled` registry value plus a `longPathAware` application manifest.
The question is whether JabRef can use it here.

## Decision Drivers

* The user must reach the file, with no silent wrong result.
* JabRef must not depend on a machine-wide setting the user has to change.
* The fix must stay inside JabRef's own process and its own invocations.

## Considered Options

* Hand arguments of `MAX_PATH` characters or more to `rundll32.exe url.dll,FileProtocolHandler`
* Declare `longPathAware` in the application manifest and require `LongPathsEnabled`
* Prefix the path with `\?\`
* Pass the 8.3 short name of the path to Explorer
* Open the file through `java.awt.Desktop#open`

## Decision Outcome

Chosen option: "Hand arguments of `MAX_PATH` characters or more to `rundll32.exe url.dll,FileProtocolHandler`", because it is the only option that works without changing anything outside JabRef.
The protocol handler takes its argument unquoted and without the limit, and it resolves the file association the same way Explorer does, so the user's PDF viewer opens as before.
Revealing a file has no such substitute — the protocol handler cannot select a file in its folder — so beyond the limit JabRef opens the containing folder without the selection, which at least arrives where the file is.

The manifest option does not apply.
`longPathAware` governs the process that declares it, and the process that fails here is `explorer.exe`, which JabRef starts and whose manifest is Microsoft's.
The behaviour was measured on a machine with `LongPathsEnabled` set to 1, where Explorer still truncated at exactly 260 characters.
JabRef's own file access is not affected either way, because the JDK prefixes long absolute paths with `\?\` in its Win32 calls.

### Consequences

* Good, because linked files open regardless of their path length, without the user configuring Windows.
* Good, because it changes one method: everything else keeps calling Explorer.
* Bad, because a revealed file beyond the limit is no longer selected in its folder.
* Bad, because the limit is now encoded in JabRef and has to be kept in step should Explorer ever lift it.

### Confirmation

`WindowsTest` pins both command builders around the limit: 259 characters still go to Explorer, 260 go to the protocol handler, and a path beyond the limit reveals its parent folder instead of selecting the file.

## Pros and Cons of the Options

### Declare `longPathAware` in the application manifest and require `LongPathsEnabled`

* Good, because it is the mechanism Microsoft documents for the limit.
* Bad, because it applies to the declaring process, and the truncation happens in Explorer.
* Bad, because `LongPathsEnabled` is machine-wide and off by default, so the fix would not reach most users.
* Bad, because the launcher's manifest comes from `jpackage` and is not JabRef's to write.

### Prefix the path with `\?\`

* Good, because it lifts the limit for the Win32 file functions.
* Bad, because the prefix addresses the file system directly and the shell does not accept it, so Explorer is no better off.

### Pass the 8.3 short name of the path to Explorer

* Good, because the short name usually stays below the limit and Explorer accepts it.
* Bad, because 8.3 name creation can be switched off per volume, and then no short name exists.
* Bad, because the shortened path is unreadable in error messages and in the folder Explorer opens.

### Open the file through `java.awt.Desktop#open`

* Good, because it is one call and already used as the fallback of the other platforms.
* Bad, because it is unverified at the limit — it goes through the same shell API as Explorer.
* Bad, because the GUI avoids AWT on Windows, where it is only the fallback path.

<!-- markdownlint-disable-file MD022 -->
