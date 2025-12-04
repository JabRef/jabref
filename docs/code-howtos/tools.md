---
parent: Code Howtos
---
# Useful development tooling

This page lists some software we consider useful.
Tool usage hints are provided at the [FAQ page](faq.md).

## Browser plugins

* (strongly recommened) [Refined GitHub](https://github.com/sindresorhus/refined-github) - GitHub on steroids
* (recommended) [GitHub Issue Link Status](https://github.com/fregante/github-issue-link-status) - proper coloring of linked issues and PRs.
* (optional) [Codecov Browser Extension](https://github.com/codecov/browser-extension) - displaying code coverage directly when browsing GitHub
* (optional) [Sourcegraph Browser Extension](https://sourcegraph.com/docs/integration/browser_extension) - Navigate through source on GitHub

## Drawing diagrams

* A free tool to make UI mockups: <https://draw.io> with `Software -> Mockups` shapes.

## Screenshots

* We recommend [Flameshot](https://flameshot.org/) and [Greenshot](https://getgreenshot.org/) to enable proper annotation of screenshots.
  GitHub allows direct paste of images from the clipboard. Thus, copy the annotated image and paste it directly in your PR description.

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
