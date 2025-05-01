---
parent: Code Howtos
---
# Useful development tooling

This page lists some software we consider useful.

## Run `gradle` from command line

1. [Install mise](https://mise.jdx.dev/installing-mise.html). `mise` is an SDK manager for all popular operating systems.
2. Run `gradle`:

   * Linux/macOS: `mise exec java@21 -- ./gradlew run`
   * Windows: `mise exec java@21 -- gradlew run`

## Browser plugins

* [Refined GitHub](https://github.com/sindresorhus/refined-github) - GitHub on steroids
* [GitHub Issue Link Status](https://github.com/fregante/github-issue-link-status) - proper coloring of linked issues and PRs.
* [Codecov Browser Extension](https://github.com/codecov/browser-extension) - displaying code coverage directly when browsing GitHub
* [Sourcegraph Browser Extension](https://sourcegraph.com/docs/integration/browser_extension) - Navigate through source on GitHub

## git hints

Here, we collect some helpful git hints

* <https://github.com/blog/2019-how-to-undo-almost-anything-with-git>
* [So you need to change your commit](https://github.com/RichardLitt/knowledge/blob/master/github/amending-a-commit-guide.md#so-you-need-to-change-your-commit)
* awesome hints and tools regarding git: <https://github.com/dictcp/awesome-git>

### Rebase everything as one commit on main

* Precondition: `JabRef/jabref` is [configured as upstream](https://help.github.com/articles/configuring-a-remote-for-a-fork/).
* Fetch recent commits and prune non-existing branches: `git fetch upstream --prune`
* Merge recent commits: `git merge upstream/main`
* If there are conflicts, resolve them
* Reset index to upstream/main: `git reset upstream/main`
* Review the changes and create a new commit using git gui: `git gui`
* Do a force push: `git push -f origin`

See also: <https://help.github.com/articles/syncing-a-fork/>

## Tooling for Windows

### Better console applications: Windows Terminal plus clink

* Install [Windows Terminal](https://aka.ms/terminal)
* Install [clink](http://mridgers.github.io/clink/), to enable `mise` support and to have Unix keys (<kbd>Alt</kbd>+<kbd>B</kbd>, <kbd>Ctrl</kbd>+<kbd>S</kbd>, etc.) also available at the prompt of `cmd.exe`
* Install `mise` support for `cmd.exe`
  * Pre-condition: Install [mise](https://mise.jdx.dev/)
  * Find out script directory: `clink info | findstr scripts`
  * Place the `clink_mise.lua` script from [mise forum](https://github.com/jdx/mise/discussions/4679#discussioncomment-12841639) into that directory.

## Tools for working with XMP

* Validate XMP: <https://www.pdflib.com/pdf-knowledge-base/xmp/free-xmp-validator>
