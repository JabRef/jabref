---
parent: Code Howtos
---
# Useful development tooling

This page lists some software we consider useful.
Tool usage hints are provided at the [FAQ page](faq.md).

## Working with GitHub

GitHub is a great platform to collaborate.
It can be enhanced to be even more pleasant.

### Browser plugins to enahnce GitHub

* (strongly recommended) [Refined GitHub](https://github.com/sindresorhus/refined-github) - GitHub on steroids
* (recommended) [GitHub Issue Link Status](https://github.com/fregante/github-issue-link-status) - proper coloring of linked issues and PRs.
* (optional) [Codecov Browser Extension](https://github.com/codecov/browser-extension) - displaying code coverage directly when browsing GitHub
* (optional) [Sourcegraph Browser Extension](https://sourcegraph.com/docs/integration/browser_extension) - Navigate through source on GitHub

### Command line tooling

[`gh` tool](https://cli.github.com/) ist GitHub's CLI tool.
The most important feature is to run [`gh checkout pr <number>`](https://cli.github.com/manual/gh_pr_checkout) to checkout another PR.
This is useful for trying out other pull requests, which in turn shows community engagement.

Note: Running JabRef using gradle from the command line requires a JDK to be installed.
One can also test without a JDK installed.
See [How to try any JabRef pull request](https://blog.jabref.org/2025/05/31/run-pr/).

## Drawing diagrams

A free tool to make UI mockups: <https://draw.io> with `Software -> Mockups` shapes.

Inside the Markdown files, [Mermaid](https://mermaid.js.org/) can be used.
This is "diagrams as code", please get familiar with that concept.

## Screenshots

We recommend [Flameshot](https://flameshot.org/) and [Greenshot](https://getgreenshot.org/) to enable proper annotation of screenshots.
GitHub allows direct paste of images from the clipboard. Thus, copy the annotated image and paste it directly in your PR description.

## Tooling for Linux

In case you use `bash` as your shell, we recommend installing [Oh My Bash](https://ohmybash.nntoan.com/) to make the experience even more awesome.

## Tooling for Windows

### Better console applications: Windows Terminal plus clink

* Install [Windows Terminal](https://aka.ms/terminal)
* Install [clink](http://mridgers.github.io/clink/), to have Unix keys (<kbd>Alt</kbd>+<kbd>B</kbd>, <kbd>Ctrl</kbd>+<kbd>S</kbd>, etc.) also available at the prompt of `cmd.exe`
* Install [Oh My Posh](https://ohmyposh.dev/) for a better PowerShell.

## Tools for working with XMP

Validate XMP: <https://www.pdflib.com/pdf-knowledge-base/xmp/free-xmp-validator>

## More Readings

Check out [awesome lists](https://github.com/sindresorhus/awesome), especially [awesome-java](https://github.com/akullpp/awesome-java#readme).
