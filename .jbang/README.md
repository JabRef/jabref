# JBang scripts for JabRef

This directory contains [JBang](https://www.jbang.dev/) scripts for JabRef.

Three use cases:

- Try out any pull request with minimal installation. See [our blog entry](https://blog.jabref.org/2025/05/31/run-pr/) for details.
- Run `JabKit` - JabRef's CLI tool.
- Run `JabSrv` - JabRef's HTTP server.

## Running `JabKit`

If you have JBang installed, just run

    jbang jabkit@jabref --help

You can also install `jabkit` permanently in your `PATH`:

    jbang app install jabkit@jabref

If have no JBang available, download [`gg.cmd`](https://github.com/eirikb/gg/tree/main?tab=readme-ov-file#ggcmd), place it in the current directory (or on your `PATH`) and execute:

    sh ./gg.cmd jbang jabkit@jabref --help

On Windows:

    gg.cmd jbang jabkit@jabref --help

## Running `JabSrv`

If you have JBang installed, just run `jbang jabsrv@jabref --help`

With `gg.cmd`:

    sh ./gg.cmd jbang jabsrv@jabref --help
