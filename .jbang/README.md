# JBang scripts for JabRef

This directory contains JBang scripts for JabRef.
[JBang](https://www.jbang.dev/) allows for running Java applications without having a JDK installed (before).

Three use cases:

- Try out any pull request with minimal installation. See [our blog entry](https://blog.jabref.org/2025/05/31/run-pr/) for details.
- Run JabKit - JabRef's CLI tool.
- Run JabSrv - JabRef's HTTP server.

## Running JabKit without installation

By using [gg.cmd](https://github.com/eirikb/gg#ggcmd) you can "just run" JabKit with minimal setup:

1. Download `gg.cmd` from: <https://github.com/eirikb/gg#ggcmd>. `gg.cmd` is a "binary" running on macOS, Linux, and Windows. No need for different binaries on different operating systems.
2. Run `gg.cmd`. This will download and use JBang as wrapper around running JabKit:

    - Linux/macOS: Run `sh ./gg.cmd jbang jabkit@jabref --help`.
    - Windows: Run `gg.cmd jbang jabkit@jabref --help`.

You can also put `gg.cmd` on your `PATH` and make it executable.
Then you enable `alias jabkit='gg.cmd jbang jabkit@jabref`.

## Running JabKit with JBang

If you have JBang installed, just run

```terminal
jbang jabkit@jabref --help
```

You can also install `jabkit` permanently in your `PATH`:

1. [Install JBang](https://www.jbang.dev/download/). E.g., by `brew install jbangdev/tap/jbang` or `choco install jbang`
2. Make `jabkit` available on the command line: `jbang app install jabkit@jabref`
3. Run `jabkit --help`

[JBang takes care about updating JabKit automatically](https://github.com/orgs/jbangdev/discussions/1636#discussioncomment-6150992).

## Running JabSrv

If you have JBang installed, just run following command

```terminal
jbang jabsrv@jabref --help
```

With `gg.cmd`:

```terminal
sh ./gg.cmd jbang jabsrv@jabref --help
```
