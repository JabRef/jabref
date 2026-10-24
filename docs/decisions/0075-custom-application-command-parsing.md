---
nav_order: 75
parent: Decision Records
---

# Parse custom external application commands
`adr~custom-application-command-parsing~1`

Needs: impl

## Context and Problem Statement

Custom terminal and file browser preferences store a command in one text field. `ProcessBuilder` needs the executable and arguments as separate strings, it does not interpret quotes. Splitting at every space breaks quoted paths, while replacing `%DIR` before splitting also breaks directory names containing spaces. How should these commands become argument lists across Linux, macOS, and Windows?

## Decision Drivers

* Keep existing `%DIR` and `%DIR%` placeholders working.
* Support quoted paths, escaped whitespace, and literal Windows drive and UNC paths.
* Keep the one-line preference and consistent parsing across operating systems.
* Launch the configured application without implicitly enabling shell features.

## Considered Options

* Split with `split(" ")`, then replace placeholders.
* Parse a limited command grammar, preserving UNC paths.
* Delegate parsing to the operating system shell.
* Require one argument per line.

## Decision Outcome

Chosen option: "Parse a limited command grammar, preserving UNC paths", because it supports quoted arguments and existing placeholders without migrating preferences or introducing OS-specific shell syntax.

Concretely:

* Unquoted whitespace separates arguments. Single and double quotes group text and are removed, empty quoted arguments are retained.
* Outside single quotes, a backslash escapes a quote or whitespace. Other backslashes remain literal, including the pair starting a UNC path.
* After tokenization, `%DIR` and `%DIR%` are replaced in one pass. Spaces and placeholder-like text in the directory cannot change argument boundaries or trigger another replacement.
* The resulting list goes directly to `ProcessBuilder`. Pipes, redirects, and shell expansion are not interpreted.

Examples, using a selected directory containing a space:

| OS | Configured command | Resulting arguments |
| --- | --- | --- |
| Linux | `gnome-terminal --working-directory='%DIR'` | `gnome-terminal`, `--working-directory=/home/user/My Library` |
| macOS | `open -a Terminal "%DIR"` | `open`, `-a`, `Terminal`, `/Users/user/My Library` |
| Windows | `"C:\Program Files\ConEmu\ConEmu64.exe" /single /dir "%DIR"` | `C:\Program Files\ConEmu\ConEmu64.exe`, `/single`, `/dir`, `C:\My Library` |
| Windows UNC | `explorer.exe "\\server\share"` | `explorer.exe`, `\\server\share` |

### Consequences

* Good, because quoted paths and both placeholder forms work with the existing one-line preference.
* Good, because parsing is consistent across operating systems and needs no extra shell process.
* Bad, because the limited grammar needs maintenance and differs from POSIX shells and `cmd.exe`.

### Confirmation

`CommandLineParserTest` checks the OS examples, literal UNC paths, escaped whitespace, empty arguments, both placeholder forms, and directories containing `%DIR`. `NativeDesktop` passes the prepared arguments to `ProcessBuilder`.

## Pros and Cons of the Options

### Split with `split(" ")`, then replace placeholders

Replacing after splitting keeps spaces introduced by the directory in one argument. It does not interpret quotes already in the command: the Linux example retains literal single quotes, the macOS example retains literal double quotes, and the Windows example splits the executable at `Program Files`.

* Good, because it is simple and handles unquoted commands without embedded spaces.
* Bad, because quotes neither group arguments nor disappear before launching the application.

### Parse a limited command grammar, preserving UNC paths

See the decision outcome and its OS examples.

* Good, because quoted paths and literal UNC paths work without changing the preference UI.
* Good, because replacement happens after argument boundaries are known.
* Bad, because users must follow this grammar rather than their shell's full syntax.

### Delegate parsing to the operating system shell

Pass the directory as an environment variable and run `/bin/sh -c` on Linux/macOS or `cmd.exe /c` on Windows.

| OS | Example | Compatibility cost |
| --- | --- | --- |
| Linux | `gnome-terminal --working-directory="$DIR"` | Existing `'%DIR'` needs new quoting too: `'$DIR'` would remain literal. |
| macOS | `open -a Terminal "$DIR"` | Existing `%DIR` must become `$DIR`. |
| Windows | `"C:\Program Files\ConEmu\ConEmu64.exe" /single /dir "%DIR%"` | Existing `%DIR` needs a trailing `%`, `cmd /c` also needs careful outer quoting. |

* Good, because the OS provides the parser and full shell syntax.
* Bad, because existing commands need migration and OS-specific quoting.
* Bad, because it enables shell features beyond launching an application. Directly inserting directory text instead of using an environment variable would require shell-specific escaping.

### Require one argument per line

Each line is one argument, with no quote delimiters. For the examples above:

* Linux: `gnome-terminal`, then `--working-directory=%DIR`.
* macOS: `open`, `-a`, `Terminal`, then `%DIR`, each on its own line.
* Windows: `C:\Program Files\ConEmu\ConEmu64.exe`, `/single`, `/dir`, then `%DIR`, each on its own line.

* Good, because argument boundaries are explicit and paths with spaces need no quoting.
* Bad, because existing preferences need migration and short commands become cumbersome to enter.
* Bad, because arguments containing newlines need another escaping rule.

<!-- markdownlint-disable-file MD022 -->
