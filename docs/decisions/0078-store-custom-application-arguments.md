---
nav_order: 78
parent: Decision Records
---

# Store custom application arguments separately
`adr~store-custom-application-arguments~1`

Needs: impl

## Context and Problem Statement

The custom terminal and file browser preferences used to store an entire command in one string. `ProcessBuilder` requires a list containing the executable and each argument. Splitting a command on spaces breaks quoted paths; implementing shell-like quoting also creates a grammar that JabRef must maintain. How should these preferences represent commands?

## Considered Options

* Keep one command field and parse it when launching.
* Run the command through an operating system shell.
* Store the executable and arguments as a list.

## Decision Outcome

Chosen option: "Store the executable and arguments as a list", because explicit argument boundaries let `ProcessBuilder` launch commands without parsing quotes at runtime.

In preferences, the first line is the executable, and each following line is one argument. Quotes are not needed to keep spaces within a line. `%DIR` and `%DIR%` are replaced within each argument after the list is assembled. The resulting list is passed directly to `ProcessBuilder`.

For the command reported in [issue 12531](https://github.com/JabRef/jabref/issues/12531), the preference is:

```text
/usr/bin/gnome-terminal
--working-directory=%DIR%
```

The second line becomes one argument even when the library directory contains spaces.

Existing one-line preferences are converted once to the new argument list. The previous parser remains only for this migration. The exact legacy Windows ConEmu default is migrated directly because its executable path was unquoted. The original preference is retained so a command that cannot be converted is not lost.

### Consequences

* Commands with spaces in executable paths or arguments no longer depend on quote parsing when launched.
* Users enter one argument per line rather than one shell-style command.
* Saved commands need a one-time conversion; malformed legacy commands may need to be corrected in preferences.

## Pros and Cons of the Options

### Keep one command field and parse it when launching

* Good, because the existing preference interface stays the same.
* Bad, because quoting and escaping differ across operating systems and have edge cases such as apostrophes in paths.

### Run the command through an operating system shell

* Good, because the shell supplies its own grammar.
* Bad, because shell syntax differs between platforms and shell expansion would interpret the library path as command text unless escaped carefully.

### Store the executable and arguments as a list

* Good, because each argument boundary is explicit and `ProcessBuilder` can use it directly.
* Good, because replacing the directory placeholder cannot create another argument.
* Bad, because the preference interface changes and legacy commands require conversion.

<!-- markdownlint-disable-file MD022 -->
