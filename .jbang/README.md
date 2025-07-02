# JBang scripts for JabRef

This directory contains [JBang](https://www.jbang.dev/) scripts for JabRef.

Three use cases:

- Try out any pull request with minimal installation. See [our blog entry](https://blog.jabref.org/2025/05/31/run-pr/) for details.
- Run `JabKit`
- Run `JabSrv`

## Running `JabKit`

`JabKit` is the CLI tool of JabRef.

If you have JBang installed, just run `jbang jabkit@jabref --help`

IF not, download [`gg.cmd`](https://github.com/eirikb/gg/tree/main?tab=readme-ov-file#ggcmd), please it in the current directory or on your `PATH` and execute

    gg.cmd jbang jabkit@jabref --help

## Running `JabSrv`

`JabSrv` is the http server of JabRef.

If you have JBang installed, just run `jbang jabsrv@jabref --help`

WIth `gg.cmd`:

    gg.cmd jbang jabsrv@jabref --help

