# JabRef

JabRef is an open-source, cross-platform reference management tool built for researchers. It helps you collect, organize, and cite academic sources throughout your research workflow.

---

## Features

### Collect

- Search scientific databases including CiteSeer, CrossRef, Google Scholar, IEEEXplore, INSPIRE-HEP, Medline PubMed, MathSciNet, Springer, arXiv, and zbMATH
- Import references from over 15 file formats
- Retrieve full-text articles and link them to your entries
- Fetch bibliographic data using ISBN, DOI, PubMed-ID, or arXiv-ID
- Extract metadata directly from PDF files
- Save references from your browser using the official extension for [Firefox](https://addons.mozilla.org/en-US/firefox/addon/jabref/?src=external-github), [Chrome](https://chrome.google.com/webstore/detail/jabref-browser-extension/bifehkofibaamoeaopjglfkddgkijdlh), [Edge](https://microsoftedge.microsoft.com/addons/detail/pgkajmkfgbehiomipedjhoddkejohfna), and [Vivaldi](https://chrome.google.com/webstore/detail/jabref-browser-extension/bifehkofibaamoeaopjglfkddgkijdlh)

### Organize

- Group references into hierarchical collections using keywords, tags, search terms, or manual assignment
- Advanced search and filtering
- Verify and fix bibliographic data against Google Scholar, Springer, and MathSciNet
- Configurable citation key generation
- Add custom metadata fields and reference types
- Detect and merge duplicate entries
- Attach documents to references — 20 supported file types out of the box, with full customization
- Automatically rename and move attached files using configurable rules
- Track reading status with ratings, priority flags, and quality markers

### Cite

- Full support for BibTeX and BibLaTeX
- Cite-as-you-write support for Emacs, Kile, LyX, Texmaker, TeXstudio, Vim, and WinEdt
- Format citations using thousands of built-in styles or define your own
- Insert and format citations in Word and LibreOffice/OpenOffice

### Share

- Multiple built-in export formats, with the option to define custom formats
- Library files are plain text — easy to share via Dropbox or manage with version control
- Team collaboration via SQL database sync

---

## Installation

The latest stable release is available at [downloads.jabref.org](https://downloads.jabref.org/).

Development builds are available at [builds.jabref.org](https://builds.jabref.org/main/).

See the [Installation Guide](https://docs.jabref.org/installation) for full setup instructions.

---

## Command Line Interface (JabKit)

JabRef includes a CLI tool called JabKit. You can run it using JBang — see [`.jbang/README.md`](.jbang/README.md) for details.

You can also run JabKit via Docker:

```terminal
docker run ghcr.io/jabref/jabkit:edge --help
```

---

## Documentation and Support

- [User documentation](https://docs.jabref.org/)
- [Frequently asked questions](https://docs.jabref.org/faq)
- [Community forum](https://discourse.jabref.org/c/help/7)
- [Feature requests](http://discourse.jabref.org/c/features)
- [Bug tracker](https://github.com/JabRef/jabref/issues)

---

## Contributing

JabRef is used by tens of thousands of researchers and is actively maintained. Contributions are welcome.

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to get involved.

---

## Building from Source

See [Building from Source](https://docs.jabref.org/installation#building-from-source) in the documentation.

---

## Research and Education

A list of papers that have used or studied JabRef is maintained at the [JabRef wiki](https://github.com/JabRef/jabref/wiki/JabRef-in-the-Media).

Guidelines for using JabRef in software engineering courses are available at [devdocs.jabref.org/teaching.html](https://devdocs.jabref.org/teaching.html).

To cite JabRef in a publication, use the following:

```bibtex
@Article{jabref,
  author  = {Oliver Kopp and Carl Christian Snethlage and Christoph Schwentker},
  title   = {JabRef: BibTeX-based literature management software},
  journal = {TUGboat},
  volume  = {44},
  number  = {3},
  pages   = {441--447},
  doi     = {10.47397/tb/44-3/tb138kopp-jabref},
  issn    = {0896-3207},
  issue   = {138},
  year    = {2023},
}
```

DOI: [10.47397/tb/44-3/tb138kopp-jabref](https://doi.org/10.47397/tb/44-3/tb138kopp-jabref) — includes a [link to the full text](https://tug.org/TUGboat/tb44-3/tb138kopp-jabref.pdf).

---

## Donations

JabRef is free to use. If you find it useful, you can support development via the [donations page](https://donations.jabref.org) or [PayPal](https://paypal.me/JabRef).

---

## Sponsorship

JabRef development is supported by [YourKit Java Profiler](https://www.yourkit.com/java/profiler/).
