# JabRef in Debian

This file tracks the status of JabRef packaging to Debian.
The current version at Debian is shown at <https://tracker.debian.org/pkg/jabref>.
Since Ubuntu is based on Debian, the JabRef version available in ubuntu (<https://packages.ubuntu.com/search?keywords=jabref>) is not higher.

At [koppor's repository](https://github.com/koppor/jabref/), required Debian adaptions to branches are tracked in `debian_x.y` branches.

In the following, libraries needing packaging are tracked.

- [ ] Update the list based on https://github.com/JabRef/jabref/blob/master/external-libraries.txt.

## JavaFX libraries

- [x] packaging of [afterburner.fx](http://afterburner.adam-bien.com/): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=848398
- [ ] packaging of [JabRef's fork of afterburner.fx](https://github.com/JabRef/afterburner.fx).
  This packaging is required, because JabRef made his own fixes.
- [x] packaging of [ControlsFX](http://fxexperience.com/controlsfx/): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=848396
- [x] packaging of [EasyBind](https://github.com/TomasMikula/EasyBind): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=848399
- [ ] packaging of [Flowless](https://github.com/TomasMikula/Flowless)
- [x] packaging of [JavaFxSVG](https://github.com/codecentric/javafxsvg): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=848401
- [ ] packaging of [mvvmfx-validation](https://github.com/sialcasa/mvvmFX/tree/develop/mvvmfx-validation)
- [ ] packaging of [RichTextFX](https://github.com/TomasMikula/RichTextFX)

## Other libraries

- [ ] packaging of [latex2unicode](https://github.com/tomtung/latex2unicode): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=858809
  - [ ] depends on packaging of [fastparse](https://github.com/lihaoyi/fastparse): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=858810
  - [x] depends on packaging of [sbt](https://github.com/sbt/sbt): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=855304
- [ ] packaging of [pgjdbc-ng](http://impossibl.github.io/pgjdbc-ng/): Obsolete: We use `org.postgresql.Driver`
- [ ] packaging of [java-diff-utils](https://github.com/bkromhout/java-diff-utils/). Need some more work due to licensing issues. See https://github.com/bkromhout/java-diff-utils/pull/4. The token is currently @koppor
- [x] CSL styles - styles: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841018 
  - [x] https://github.com/citation-style-language/styles/issues/2372 needs to be solved
  - Resulting package: https://packages.debian.org/sid/citation-style-language-styles
- [x] CSL styles - locale files: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841019
  - [x] https://github.com/citation-style-language/locales/issues/151 needs to be solved
  - Resulting package: https://packages.debian.org/sid/citation-style-language-locales
- [ ] CSL styles - citeproc-java
  - [ ] close https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841020
  - [x] Switch to pure-java implementation https://github.com/JabRef/jabref/pull/5997
  - [ ] depends on [jbibtex](https://github.com/jbibtex/jbibtex) - https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=888087
- [x] packaging of [LGoodDatePicker](https://github.com/LGoodDatePicker/LGoodDatePicker): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=847591
- [x] <s>packaging of https://github.com/JabRef/org.jabref.gui.customjfx.support - or</s> including an OpenJDK fix (https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=872619)

## Other issues

- [x] Remove `org.json` dependency: https://github.com/JabRef/jabref/issues/3703
- [x] Make JabRef running on Java 9. JabRef now runs on jdk12: https://github.com/JabRef/jabref/pull/5426
