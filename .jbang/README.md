# JBang scripts for JabRef

This directory contains JBang scripts for JabRef.
[JBang](https://www.jbang.dev/) allows for running Java applications without having a JDK installed (before).

Five use cases:

- Runing `JabKit` - JabRef's CLI tool.
- Runing `JabLs` - JabRef's LSP Server.
- Runing `JabSrv` - JabRef's HTTP server.
- Try out any pull request with minimal installation. See [our blog entry](https://blog.jabref.org/2025/05/31/run-pr/) for details.
- See what changed since you last ran JabRef from your checkout (`WhatsNew.java`).

## Running `JabKit`

```bash
$ jbang --fresh jabkit@jabref --help

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
  check                   Check the integrity and consistency of a library.
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
> Due to the high development pace, `--fresh` is used to update `org.jabref:jablib:6.0-SNAPSHOT`.
> As soon as JabRef 6.0 is released, this won't be required any more.

> [!NOTE]
> The scripts depend on the published `org.jabref:jablib:6.0-SNAPSHOT`.
> When testing local, unmerged `jablib` changes, publish them to your local Maven repository first:
>
> ```console
> ./gradlew -PprojVersion=6.0 :jablib:publishToMavenLocal
> ```
>
> `-PprojVersion=6.0` is required: without it the artifact is published as `0.1.0-SNAPSHOT` and JBang will not pick it up.
> The `mavenlocal` repository is listed first in the scripts, so the locally published `jablib` takes precedence.

### Installing and Running `JabKit` with JBang

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

### Running `JabKit` without installation

By using [gg.cmd](https://github.com/eirikb/gg#ggcmd) you can "just run" JabKit with minimal setup:

1. Download `gg.cmd` from: <https://github.com/eirikb/gg#ggcmd>. `gg.cmd` is a "binary" running on macOS, Linux, and Windows. No need for different binaries on different operating systems.
2. Run `gg.cmd`. This will download and use JBang as wrapper around running JabKit:

    - Linux/macOS: Run `sh ./gg.cmd jbang jabkit@jabref`.
    - Windows: Run `gg.cmd jbang jabkit@jabref`.

You can also put `gg.cmd` on your `PATH` and make it executable.
Then you enable `alias jabkit='gg.cmd jbang jabkit@jabref`.

## Running `JabLs`

In case you have [JBang installed], just run following command:

```terminal
jbang --fresh jabls@jabref
```

With `gg.cmd`:

```terminal
sh ./gg.cmd jbang --fresh jabls@jabref
```

With `npx`:

```terminal
npx @jbangdev/jbang --fresh jabls@jabref
```

One can add `--help` to see available options.

## Running `JabSrv`

In case you have [JBang installed], just run following command:

```terminal
jbang --fresh jabsrv@jabref
```

With `gg.cmd`:

```terminal
sh ./gg.cmd jbang --fresh jabsrv@jabref
```

With `npx`:

```terminal
npx @jbangdev/jbang --fresh jabsrv@jabref
```

One can add `--help` to see available options. E.g., how to set another port and how to specify served libraries.

JBang installed: <https://www.jbang.dev/download/>

## Try out any pull request

See <https://blog.jabref.org/2025/05/31/run-pr/> for a howto.

## What's new since my last run

`just run-main` pulls the latest `main` and, before starting JabRef, opens a window with the `CHANGELOG.md` entries that landed since the previous run, split into *changes by others* and *changes by me* (the `git config user.email` of the checkout).
Each entry is attributed to the commit that first added it, so a link fix or a rewording by someone else keeps the original author.
"Run" starts JabRef, "Cancel run" stops the recipe.

```bash
just whats-new            # the window on its own
just whats-new --stdout   # plain text instead
```

The first run only records the current commit (in the checkout's `.git` directory) and shows nothing.

While JabRef runs from a checkout, the same news lives in a "What's new" toolbar button next to the GitHub one: every five minutes it fetches, turns blue once the checkout is behind, lists the pending entries in its tooltip and, clicked, opens the window with a *Restart to update* button.
`just run-loop` makes that restart work: it pulls, rebuilds and starts JabRef again.
