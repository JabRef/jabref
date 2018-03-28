This isssue tracks the status of JabRef packaging to Debian. The current version at Debian is shown at https://tracker.debian.org/pkg/jabref.

At [koppor's repository](https://github.com/koppor/jabref/), required Debian adaptions to branches are tracked in `debian_x.y` branches.

The corresponding debian bug is https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=810725

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
- [ ] packaging of [pgjdbc-ng](http://impossibl.github.io/pgjdbc-ng/): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841011
- [ ] packaging of [java-diff-utils](https://github.com/bkromhout/java-diff-utils/). Need some more work due to licensing issues. See https://github.com/bkromhout/java-diff-utils/pull/4. The token is currently @koppor
- [x] CSL styles - styles: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841018 
  - [x] https://github.com/citation-style-language/styles/issues/2372 needs to be solved
  - Resulting package: https://packages.debian.org/sid/citation-style-language-styles
- [x] CSL styles - locale files: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841019
  - [x] https://github.com/citation-style-language/locales/issues/151 needs to be solved
  - Resulting package: https://packages.debian.org/sid/citation-style-language-locales
- [ ] CSL styles - citeproc-java: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=841020
  - [ ] depends on [jbibtex](https://github.com/jbibtex/jbibtex) - https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=888087
- [x] packaging of [LGoodDatePicker](https://github.com/LGoodDatePicker/LGoodDatePicker): https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=847591
- [x] <s>packaging of https://github.com/JabRef/org.jabref.gui.customjfx.support - or</s> including an OpenJDK fix (https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=872619)

## Other issues

- [ ] Remove `org.json` dependency: https://github.com/JabRef/jabref/issues/3703
- [ ] Make JabRef running on Java9. Triggered by the discussion at https://lists.debian.org/debian-java/2017/11/msg00028.html. This is WIP at see https://github.com/JabRef/jabref/issues/2594#issuecomment-346829982

# Done for JabRef 3.8.2 in Debian

- [x] packaging of libunirest-java: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=819672 
  - blocked by https://github.com/Mashape/unirest-java/issues/176 
  - implemented in https://github.com/Mashape/unirest-java/pull/179
  - workaround exists
- [x] #131 needs to be solved

### JGoodies (old versions already existing in Debian)
- [x] update of  libjgoodies-common-java: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=819668 
- [x] update of  libjgoodies-forms-java: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=819669 
- [x] update of  libjgoodies-looks-java: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=819670 

Newer versions of JGoodies are not distributed as open source any more. The latest open source versions are listed at https://web.archive.org/web/20150517070237/http://www.jgoodies.com/downloads/libraries/.

```
JGoodies Binding 2.14.0     Apr/02/2015
JGoodies Common 1.9.0   Apr/02/2015
JGoodies Forms 1.10.0   Apr/02/2015
JGoodies Looks 2.8.0    Apr/02/2015
JGoodies Validation 2.6.0   Apr/02/2015
```

The links to the files are 404 and there are no other mirrors where these files are available.

The files available on maven central are also the latest versions available through www.archive.org - see https://web.archive.org/web/20150407130628/http://www.jgoodies.com/downloads/libraries/ and https://web.archive.org/web/20150905122056/http://www.jgoodies.com/downloads/archive/
