# JabRef Bibliography Management

JabRef is an open-source, cross-platform citation and reference management tool.

Stay on top of your literature: JabRef helps you to collect and organize sources, find the paper you need and discover the latest research.
[![main table](docs/images/jabref-mainscreen.png)](http://www.jabref.org/img/jabref-mainscreen.png)

## Features

JabRef is available free of charge and is actively developed.
It supports you in every step of your research work.

### Collect

- Search across many online scientific catalogues like CiteSeer, CrossRef, Google Scholar, IEEEXplore, INSPIRE-HEP, Medline PubMed, MathSciNet, Springer, arXiv, and zbMATH
- Import options for over 15 reference formats
- Easily retrieve and link full-text articles
- Fetch complete bibliographic information based on ISBN, DOI, PubMed-ID and arXiv-ID
- Extract metadata from PDFs
- Import new references directly from the browser with one click using the [official browser extension](https://github.com/JabRef/JabRef-Browser-Extension) for [Firefox](https://addons.mozilla.org/en-US/firefox/addon/jabref/?src=external-github),  [Chrome](https://chrome.google.com/webstore/detail/jabref-browser-extension/bifehkofibaamoeaopjglfkddgkijdlh), [Edge](https://microsoftedge.microsoft.com/addons/detail/pgkajmkfgbehiomipedjhoddkejohfna) and [Vivaldi](https://chrome.google.com/webstore/detail/jabref-browser-extension/bifehkofibaamoeaopjglfkddgkijdlh)

### Organize

- Group your research into hierarchical collections and organize research items based on keywords/tags, search terms or your manual assignments
- Advanced search and filter features
- Complete and fix bibliographic data by comparing with curated online catalogues such as Google Scholar, Springer or MathSciNet
- Customizable citation key generator
- Customize and add new metadata fields or reference types
- Find and merge duplicates
- Attach related documents: 20 different kinds of documents supported out of the box, completely customizable and extendable
- Automatically rename and move associated documents according to customizable rules
- Keep track of what you read: ranking, priority, printed, quality-assured

### Cite

- Native BibTeX and Biblatex support
- Cite-as-you-write functionality for external applications such as Emacs, Kile, LyX, Texmaker, TeXstudio, Vim and WinEdt.
- Format references in one of the many thousand built-in citation styles or create your style
- Support for Word and LibreOffice/OpenOffice for inserting and formatting citations

### Share

- Many built-in export options or create your export format
- Library is saved as a simple text file and thus it is easy to share with others via Dropbox and is version-control friendly
- Work in a team: sync the contents of your library via a SQL database

## Installation

Fresh development builds are available at [builds.jabref.org](https://builds.jabref.org/main/).
The [latest stable release is available at FossHub](https://downloads.jabref.org/).

Please see our [Installation Guide](https://docs.jabref.org/installation).

## Bug Reports, Suggestions, Other Feedback

[![Donation](https://img.shields.io/badge/donate%20to-jabref-orange.svg)](https://donations.jabref.org)
[![Paypal Donate](https://img.shields.io/badge/donate-paypal-00457c.svg?logo=paypal&style=flat-square)](https://paypal.me/JabRef)

We are thankful for any bug reports or other feedback.
If you have ideas for new features you want to be included in JabRef, [tell us in our forum](http://discourse.jabref.org/c/features)!
If you need support in using JabRef, please read [the documentation](https://docs.jabref.org/) first, the [frequently asked questions (FAQ)](https://docs.jabref.org/faq) and also have a look at our [community forum](https://discourse.jabref.org/c/help).
You can use our [GitHub issue tracker](https://github.com/JabRef/jabref/issues) to file bug reports.

An explanation of donation possibilities and usage of donations is available at our [donations page](https://donations.jabref.org).

## Contributing

[![dev-docs](https://img.shields.io/badge/dev-docs-blue)](https://devdocs.jabref.org/)
[![Help Contribute to Open Source](https://www.codetriage.com/jabref/jabref/badges/users.svg)](https://www.codetriage.com/jabref/jabref)
[![Join the chat at https://gitter.im/JabRef/jabref](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/JabRef/jabref?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![OpenHub](https://www.openhub.net/p/jabref/widgets/project_thin_badge.gif)](https://www.openhub.net/p/jabref)
[![Deployment Status](https://github.com/JabRef/jabref/workflows/Deployment/badge.svg)](https://github.com/JabRef/jabref/actions?query=workflow%3ADeployment)
[![Test Status](https://github.com/JabRef/jabref/workflows/Tests/badge.svg)](https://github.com/JabRef/jabref/actions?query=workflow%3ATests)
[![codecov.io](https://codecov.io/github/JabRef/jabref/coverage.svg?branch=master)](https://codecov.io/github/JabRef/jabref?branch=master)

Want to be part of a free and open-source project that tens of thousands of scientists use every day?
Check out the ways you can contribute, below:

- Not a programmer? Help translating JabRef at [Crowdin](https://crowdin.com/project/jabref) or learn how to help at [contribute.jabref.org](https://contribute.jabref.org)
- Quick overview on the architecture needed? Look at our [high-level documentation](https://devdocs.jabref.org/getting-into-the-code/high-level-documentation)
- For details on how to contribute, have a look at our [guidelines for contributing](CONTRIBUTING.md).
- You are welcome to contribute new features. To get your code included into JabRef, just [fork](https://help.github.com/en/articles/fork-a-repo) the JabRef repository, make your changes, and create a [pull request](https://help.github.com/en/articles/about-pull-requests).
- To work on existing JabRef issues, check out our [issue tracker](https://github.com/JabRef/jabref/issues). New to open source contributing? Look for issues with the ["good first issue"](https://github.com/JabRef/jabref/labels/good%20first%20issue) label to get started.

We view pull requests as a collaborative process.
Submit a pull request early to get feedback from the team on work in progress.
We will discuss improvements with you and agree to merge them once the [developers](https://github.com/JabRef/jabref/blob/master/DEVELOPERS) approve.

If you want a step-by-step walk-through on how to set-up your workspace, please check [this guideline](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace).

To compile JabRef from source, you need a Java Development Kit 15 and `JAVA_HOME` pointing to this JDK.
To run it, just execute `gradlew run`.
When you want to develop, it is necessary to generate additional sources using `gradlew generateSource`
and then generate the Eclipse `gradlew eclipse`.
For IntelliJ IDEA, just import the project via a Gradle Import by pointing at the `build.gradle`.

`gradlew test` executes all tests. We use [Github Actions](https://github.com/JabRef/jabref/actions) for executing the tests after each commit. For developing, it is sufficient to locally only run the associated test for the classes you changed. Github will report any other failure.

## Sponsoring

JabRef development is powered by YourKit Java Profiler [![YourKit Java Profiler](https://www.yourkit.com/images/yk_logo.png)](https://www.yourkit.com/java/profiler/)

[JabRef]: https://www.jabref.org
