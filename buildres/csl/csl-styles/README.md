<p align="center"><a href="https://citationstyles.org/" target="_blank"><img width="300" src="https://raw.githubusercontent.com/citation-style-language/logo/master/assets/rgb/%C2%ABCSL%C2%BB.svg" alt="CSL logo"></a></p>

<h1 align="center">Citation Style Language - Style Repository</h1>

<p align="center">
  <a href="https://github.com/citation-style-language/styles#licensing"><img src="https://img.shields.io/badge/license-CC%20BY%20SA%203.0-blue.svg" alt="License"></a>
  <a href="https://github.com/citation-style-language/styles/actions"><img src="https://github.com/citation-style-language/styles/workflows/Merge%20to%20release/badge.svg?event=push" alt="Build Status"></a>
</p>

Introduction
------------

The independent open source [Citation Style Language](https://citationstyles.org/) (CSL) project aims to facilitate scholarly communication by automating the formatting of citations and bibliographies.
The primary components of the CSL ecosystem are:

* The CSL schema and specification, which describe how the XML-based CSL styles and locale files should be written and interpreted
* Curated repositories of CSL styles and locale files
* Third party CSL processors, software libraries for rendering formatted citation and bibliographies from CSL styles, CSL locale files, and item metadata

This README describes our official curated repository of CSL styles, hosted at https://github.com/citation-style-language/styles/.
CSL locale files, which provide default localization data for CSL styles (such as translations and date formats), can be found at https://github.com/citation-style-language/locales.

For more information about CSL and CSL styles, check out https://citationstyles.org/ and the information files in this repository ([Style Requirements](https://github.com/citation-style-language/styles/blob/master/STYLE_REQUIREMENTS.md), [Style Development](https://github.com/citation-style-language/styles/blob/master/STYLE_DEVELOPMENT.md), [Requesting Styles](https://github.com/citation-style-language/styles/blob/master/REQUESTING.md), [Contributing Styles](https://github.com/citation-style-language/styles/blob/master/CONTRIBUTING.md), and [Quality Control](https://github.com/citation-style-language/styles/blob/master/QUALITY_CONTROL.md)).

Criteria for inclusion
----------------------

The official CSL style repository is the only repository of its kind, is used by dozens of third-party software products, and is relied upon by hundreds of thousands of users.
The popularity of this repository is in large part due to its crowd-sourced nature, and, we believe, also due to our careful curation.
While we evaluate style submissions on a case-by-case basis, we generally use the following criteria for inclusion in the CSL style repository:

* Styles must be of sufficient quality and meet our [style requirements](https://github.com/citation-style-language/styles/blob/master/STYLE_REQUIREMENTS.md).
  While we may be able to assist with this, its ultimately the submitter's responsibility to provide a style that meets our standards.
* Styles should be based on an official style guide (and link to the style guide in online or printed form).
* Styles should be of interest to a wider audience.
  We are happy to accept styles with a niche audience, but as a rule of thumb, style submitters should not know all individuals who would be interested in their style.
  If you do, it's generally better to distribute your style to them yourself.

Based on these criteria, we generally accept:

* styles for journals with open (unsolicited) submissions (and styles for publishers of such journals).
* styles for published style guides, such as those from professional organizations, universities, and university departments.

However, we typically won't accept:

* styles for personal use, or for internal use within small organizations.
* styles solely for use with an (internal) API.

If in doubt whether your style is a good fit for the repository, feel free to open an issue or pull request and ask for our opinion.
If we don't accept your submission, we encourage you to distribute your CSL style yourself.

Versioning and style distribution
---------------------------------

We currently only actively maintain CSL styles in the "master" branch for the latest released version of CSL.

In addition, in order to provide a stable location for styles of a given CSL version, styles in "master" are automatically copied to a branch named after the latest version of CSL (e.g. "v1.0.2" when CSL 1.0.2 is the latest version).
Once a new version of CSL is released (e.g. CSL 1.1.0), "master" will upgrade to that version, after which styles will be copied from "master" to a new version branch ("v1.1.0").
Version branches for old versions of CSL are kept but typically become dormant, as for these branches we generally stop maintenance, won't accept third-party contributions via pull requests, nor backport changes made to styles in "master".

Starting with branch "v1.0.1", the version branches differ from "master" in the following ways:

* the latest version branch only receives updates from "master" if all tests in "master" pass
* `<updated/>` timestamps of styles in the version branch are changed to match the git modification date of each individual style in "master"
* superfluous files present in "master", e.g. for style testing, are removed.

As such, especially for downstream integrators, you are encouraged to obtain your CSL styles from the appropriate version branch (e.g. "v1.0.2" for CSL 1.0.2 styles).
Currently, the styles repository includes the following (protected) branches:

* "master"
* "v1.0.2" (for CSL 1.0.2 styles)
* "v1.0.1" (for CSL 1.0.1 styles)
* "v1.0" (for CSL 1.0 styles)

The release regimen described here is also used for our [CSL locales](https://github.com/citation-style-language/locales).

As of November 2020, https://github.com/citation-style-language/styles-distribution, which fulfilled a similar role to the version branches, has been deprecated and no longer receives updates from https://github.com/citation-style-language/styles.

Licensing
---------

All styles in this repository are released under the [Creative Commons Attribution-ShareAlike 3.0 Unported license](https://creativecommons.org/licenses/by-sa/3.0/).
For attribution, any software using CSL styles from this repository must include a clear mention of the CSL project and a link to https://citationstyles.org/.
When redistributing styles, the listings of authors and contributors in the style metadata must be kept as is.
