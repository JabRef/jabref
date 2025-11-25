# JBang scripts for JabRef

This directory contains JBang scripts for JabRef.
[JBang](https://www.jbang.dev/) allows for running Java applications without having a JDK installed (before).

Four use cases:

- Runing `JabKit` - JabRef's CLI tool.
- Runing JabLs - JabRef's Language Server.
- Runing JabSrv - JabRef's HTTP server.
- Try out any pull request with minimal installation. See [our blog entry](https://blog.jabref.org/2025/05/31/run-pr/) for details.

## Running `JabKit`

```bash
$ jbang jabkit@jabref --help

   &&&    &&&&&    &&&&&&&&   &&&&&&&&   &&&&&&&&& &&&&&&&&&
   &&&    &&&&&    &&&   &&&  &&&   &&&  &&&       &&&
   &&&   &&& &&&   &&&   &&&  &&&   &&&  &&&       &&&
   &&&   &&   &&   &&&&&&&    &&&&&&&&   &&&&&&&&  &&&&&&&
   &&&  &&&&&&&&&  &&&   &&&  &&&   &&&  &&&       &&&
   &&&  &&&   &&&  &&&   &&&  &&&   &&&  &&&       &&&
&&&&&   &&&   &&&  &&&&&&&&   &&&   &&&  &&&&&&&&& &&&

Version: 6.0-alpha.164--2025-11-20--2243e72
Staying on top of your literature since 2003 - https://www.jabref.org/
Please report issues at https://github.com/JabRef/jabref/issues

JabKit - command line toolkit for JabRef
Usage: jabkit [-dhpv] [COMMAND]
  -d, --debug       Enable debug output
  -h, --help        display this help message
  -p, --porcelain   Enable script-friendly output
  -v, --version     display version info
Commands:
  check-consistency       Check consistency of the library.
  check-integrity         Check integrity of the database.
  convert                 Convert between bibliography formats.
  doi-to-bibtex           Converts a DOI to BibTeX
  fetch                   Fetch entries from a provider.
  generate-bib-from-aux   Generate small bib from aux file.
  generate-citation-keys  Generate citation keys for entries in a .bib file.
  get-cited-works         Outputs a list of works cited ("bibliography")
  get-citing-works        Outputs a list of works citing the work at hand
  pdf                     Manage PDF metadata.
  preferences             Manage JabKit preferences.
  pseudonymize            Perform pseudonymization of the library
  search                  Search in a library.
```

> [!NOTE]
> Due to the high development pace, you need to sometimes refresh the dependencies
>
> `jbang --fresh jabkit@jabref --help`

### Installing and Running JabKit with JBang

To have `jbang` working, you need to [install jbang](https://www.jbang.dev/download/).
E.g., by `brew install jbangdev/tap/jbang` or `choco install jbang`

`jbang` also enables to install `jabkit` permanently in your `PATH`:

```console
jbang app install --fresh --force jabkit@jabref
```

Then, you can run JabKit:

```console
jabkit --help
```

> [!NOTE]
> You can use the command above to update JabKit, too.
> Background: Although [JBang takes care about updating JabKit automatically](https://github.com/orgs/jbangdev/discussions/1636#discussioncomment-6150992), it does not update the `-SNAPSHOT` dependencies.

### Running JabKit without installation

By using [gg.cmd](https://github.com/eirikb/gg#ggcmd) you can "just run" JabKit with minimal setup:

1. Download `gg.cmd` from: <https://github.com/eirikb/gg#ggcmd>. `gg.cmd` is a "binary" running on macOS, Linux, and Windows. No need for different binaries on different operating systems.
2. Run `gg.cmd`. This will download and use JBang as wrapper around running JabKit:

    - Linux/macOS: Run `sh ./gg.cmd jbang jabkit@jabref`.
    - Windows: Run `gg.cmd jbang jabkit@jabref`.

You can also put `gg.cmd` on your `PATH` and make it executable.
Then you enable `alias jabkit='gg.cmd jbang jabkit@jabref`.

## Running JabLS

In case you have [JBang installed], just run following command:

```terminal
jbang jabls@jabref
```

With `gg.cmd`:

```terminal
sh ./gg.cmd jbang jabls@jabref
```

With `npx`:

```terminal
npx @jbangdev/jbang jabls@jabref
```

One can add `--help` to see available options.

## Running JabSrv

In case you have [JBang installed], just run following command:

```terminal
jbang jabsrv@jabref
```

With `gg.cmd`:

```terminal
sh ./gg.cmd jbang jabsrv@jabref
```

With `npx`:

```terminal
npx @jbangdev/jbang jabsrv@jabref
```

One can add `--help` to see available options. E.g., how to set another port and how to specify served libraries.

JBang installed: https://www.jbang.dev/download/

## Try out any pull request

See <https://blog.jabref.org/2025/05/31/run-pr/> for a howto.
