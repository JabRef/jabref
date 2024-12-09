# External libraries

This document lists the fonts, icons, and libraries used by JabRef.
This file is manually kept in sync with build.gradle and the binary jars contained in the lib/ directory.

One can list all dependencies by using Gradle task `dependencyReport`.
It generates the file `build/reports/project/dependencies.txt`.
Below, there is a howto to generate the content at "Sorted list of runtime dependencies output by gradle".

## Legend

### License

We follow the [SPDX license identifiers](https://spdx.org/licenses/).
In case you add a library, please use these identifiers.
For instance, "BSD" is not exact enough, there are numerous variants out there: BSD-2-Clause, BSD-3-Clause-No-Nuclear-Warranty, ...
Note that the SPDX license identifiers are different from the ones used by debian. See <https://wiki.debian.org/Proposals/CopyrightFormat> for more information.

## bst files

```yaml
Project: IEEEtran
Path:    src/main/resources/bst/IEEEtran.bst
URL:     https://www.ctan.org/tex-archive/macros/latex/contrib/IEEEtran/bibtex
License: LPPL-1.3
```

## Fonts and Icons

The loading animation during loading of recommendations from Mr. DLib is created by <http://loading.io/> and is free of use under license CC0 1.0.

```yaml
Id:      material-design-icons.font
Project: Material Design Icons
Version: v1.5.54
URL:     https://materialdesignicons.com/
License: SIL Open Font License, Version 1.1
Note:    It is important to include v1.5.54 or later as v1.5.54 is the first version offering fixed code points. Do not confuse with http://zavoloklom.github.io/material-design-iconic-font/
```

## Libraries

(Sorted alphabetically by Id)

```yaml
Id:      ai.djl.*:*
Project: Deep Java Library
URL:     https://djl.ai/
License: Apache-2.0
```

```yaml
Id:      at.favre.lib
Project: HMAC-based Key Derivation Function (HKDF) RFC 5869
URL:     https://github.com/patrickfav/hkdf
License: Apache-2.0
```

```yaml
Id:      com.dlsc.gemsfx:gemsfx
Project: GemsFX
URL:     https://github.com/dlsc-software-consulting-gmbh/GemsFX
License: Apache-2.0
```

```yaml
Id:      com.dlsc.pickerfx:pickerfx
Project: GemsFX
URL:     https://github.com/dlsc-software-consulting-gmbh/PickerFX
License: Apache-2.0
```

```yaml
Id:      com.dlsc.unitfx:unitfx
Project: UnitFX
URL:     https://github.com/dlsc-software-consulting-gmbh/UnitFX
License: Apache-2.0
```

```yaml
Id:      com.fasterxml:aalto-xml
Project: Jackson Project
URL:     https://github.com/FasterXML/aalto-xml
License: Apache-2.0
```

```yaml
Id:      com.fasterxml.jackson
Project: Jackson Project
URL:     https://github.com/FasterXML/jackson
License: Apache-2.0
```

```yaml
Id:      com.knuddels:jtokkit
Project: JTokkit - Java Tokenizer Kit
URL:     https://github.com/knuddelsgmbh/jtokkit
License: MIT
```

```yaml
Id:      com.github.hypfvieh.dbus-java
Project: dbus-java
URL:     https://github.com/hypfvieh/dbus-java
License: MIT
```

```yaml
Id:      com.github.hypfvieh.java-utils
Project: java-utils
URL:     https://github.com/hypfvieh/java-utils
License: MIT
```

```yaml
Id:      com.github.javakeyring
Project: Java Keyring
URL:     https://github.com/javakeyring/java-keyring
License: BSD-3-Clause
```

```yaml
Id:      com.github.JabRef
Project: afterburner.fx
URL:     https://github.com/JabRef/afterburner.fx
License: Apache-2.0
```

```yaml
Id:      com.github.tomtung
Project: latex2unicode
URL:     https://github.com/tomtung/latex2unicode
License: Apache-2.0
```

```yaml
Id:      com.github.vatbub:mslinks
Project: mslinks
URL:     https://github.com/vatbub/mslinks
License: Apache-2.0
```

```yaml
Id:      com.github.weisj:jsvg
Project: JSVG - A Java SVG implementation
URL:     https://github.com/weisJ/jsvg
License: MIT
```

```yaml
Id:      com.google.code.gson:gson
Project: Google Guava
URL:     https://github.com/google/gson
License: Apache-2.0
```

```yaml
Id:      com.google.guava:failureaccess
Project: Google Guava
URL:     https://github.com/google/guava
License: Apache-2.0
Note:    See https://github.com/google/guava/issues/3437 for a discussion that this dependency is really required.
```

```yaml
Id:      com.google.guava:guava
Project: Google Guava
URL:     https://github.com/google/guava
License: Apache-2.0
```

```yaml
Id:      com.google.j2objc:j2objc-annotations
Project: j2objc-annotations
URL:     https://github.com/google/j2objc
License: Apache-2.0
```

```yaml
Id:      com.googlecode.plist
Project: com.dd.plist
URL:     https://github.com/3breadt/dd-plist
License: MIT
```

```yaml
Id:      com.googlecode.javaewah:JavaEWAH
Project: JavaEWAH
URL:     https://github.com/lemire/javaewah
License: Apache-2.0
```

```yaml
Id:      com.jthemedetecor.OsThemeDetector
Project: jSystemThemeDetector
URL:     https://github.com/Dansoftowner/jSystemThemeDetector
License: Apache-2.0
```

```yaml
Id:      com.kohlschutter.junixsocket
Project: junixsocket
URL:     https://github.com/kohlschutter/junixsocket
License: Apache-2.0
```

```yaml
Id:      com.konghq.unirest
Project: Unirest for Java
URL:     https://github.com/Kong/unirest-java
License: MIT
```

```yaml
Id:      com.oracle.ojdbc:ojdbc10
Project: Oracle's JDBC drivers
URL:     https://repo1.maven.org/maven2/com/oracle/ojdbc/ojdbc10/19.3.0.0/ojdbc10-19.3.0.0.pom
License: Oracle Free Use Terms and Conditions (FUTC)
```

```yaml
Id:      com.sun.istack:istack-commons-runtime
Project: iStack Common Utility Code
URL:     https://github.com/eclipse-ee4j/jaxb-istack-commons
License: BSD-3-Clause (with copyright as described in Eclipse Distribution License - v 1.0 - see https://wiki.spdx.org/view/Legal_Team/License_List/Licenses_Under_Consideration for details)
```

```yaml
Id:      com.vladsch.flexmark:flexmark-all
Project: flexmark-java
URL:     https://github.com/vsch/flexmark-java
License: BSD-2-Clause
```

```yaml
Id:      com.vladsch.flexmark:flexmark-html2md-converter
Project: flexmark-java
URL:     https://github.com/vsch/flexmark-java
License: BSD-2-Clause
```

```yaml
Id:      commons-beanutils:commons-beanutils
Project: Apache Commons Beanutils
URL:     https://commons.apache.org/proper/commons-beanutils/
License: Apache-2.0
```

```yaml
Id:      commons-cli:commons-cli
Project: Apache Commons CLI
URL:     http://commons.apache.org/cli/
License: Apache-2.0
```

```yaml
Id:      commons-codec:commons-codec
Project: Apache Commons Codec
URL:     https://commons.apache.org/proper/commons-codec/
License: Apache-2.0
```

```yaml
Id:      commons-collections:commons-collections
Project: Apache Commons Collections
URL:     https://commons.apache.org/proper/commons-collections/
License: Apache-2.0
```

```yaml
Id:      commons-io:commons-io
Project: Apache Commons IO
URL:     https://commons.apache.org/proper/commons-io/
License: Apache-2.0
```

```yaml
Id:      commons-logging:commons-logging
Project: Apache Commons Logging
URL:     http://commons.apache.org/logging/
License: Apache-2.0
```

```yaml
Id:      commons-digester:commons-digester
Project: Apache Commons Digester
URL:     https://commons.apache.org/proper/commons-digester/
```

```yaml
Id:      commons-io:commons-io
Project: Apache Commons IO
URL:     https://commons.apache.org/proper/commons-io/
```

```yaml
Id:      de.rototor.jeuclid:jeuclid-core
Project: JEuclid
URL:     https://github.com/rototor/jeuclid
License: Apache-2.0
```

```yaml
Id:      de.rototor.snuggletex:snuggletex-core
Project: SnuggleTeX
URL:     https://github.com/rototor/snuggletex
License: BSD
```

```yaml
Id:      de.saxsys:mvvmfx
Project: mvvm(fx)
URL:     https://github.com/sialcasa/mvvmFX
License: Apache-2.0
```

```yaml
Id:      de.swiesend:secret-service
Project: Secret Service
URL:     https://github.com/swiesend/secret-service
License: MIT
```

```yaml
Id:      de.saxsys:mvvmfx-validation
Project: mvvm(fx)
URL:     https://github.com/sialcasa/mvvmFX
License: Apache-2.0
```

```yaml
Id:      de.undercouch.citeproc-java
Project: Citeproc-Java
URL:     http://michel-kraemer.github.io/citeproc-java/
Licence: Apache-2.0
```

```yaml
Id:      eu.lestard:doc-annotations
Project: doc annotations
URL:     https://github.com/lestard/doc-annotations
License: MIT
```

```yaml
Id:      info.debatty:java-string-similarity
Project: Java String Similarity
URL:     https://github.com/tdebatty/java-string-similarity
License: MIT
```

```yaml
Id:      io.github.adr:e-adr
Project: EmbeddedArchitecturalDecisionRecords
URL:     https://github.com/adr/e-adr/
License: EPL-2.0
```

```yaml
Id:      io.github.java-diff-utils:java-diff-utils
Project: java-diff-utils
URL:     https://github.com/java-diff-utils/java-diff-utils
License: Apache-2.0
```

```yaml
Id:      io.zonky.test:embedded-postgres
Project: embedded-postgres
URL:     https://github.com/zonkyio/embedded-postgres
License: Apache-2.0
```

```yaml
Id:      jakarta.annotation:jakarata.annotation-api
Project: Jakarta Annotations
URL:     https://projects.eclipse.org/projects/ee4j.ca
License: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      jakarta.activation:jakarata.activation-api
Project: Jakarta Activation
URL:     https://projects.eclipse.org/projects/ee4j.ca
License: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      jakarta.inject:jakarata.inject-api
Project: Jakarta Inject
URL:     https://projects.eclipse.org/projects/ee4j.ca
License: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      jakarta.xml.bind:jakarta.xml.bind-api
Project: Jakarta XML Binding project
URL:     https://github.com/eclipse-ee4j/jaxb-api
License: BSD-3-Clause; sometimes EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      net.harawata:appdirs
Project: AppDirs
URL:     https://github.com/harawata/appdirs
License: Apache-2.0
```

```yaml
Id:      net.java.dev.jna
Project: Java Native Access (JNA)
URL:     https://github.com/java-native-access/jna
License: Apache-2.0
```

```yaml
Id:      net.java.dev.jna-platform
Project: Java Native Access (JNA)
URL:     https://github.com/java-native-access/jna
License: Apache-2.0
```

```yaml
Id:      net.jcip:jcip-annotations
Project: JCIP (Java Concurrency in Practice) Annotations under Apache License
URL:     http://stephenc.github.io/jcip-annotations/
License: Apache-2.0
```

```yaml
Id:      net.jodah:typetools
Project: TypeTools
URL:     https://github.com/jhalterman/typetools
License: Apache-2.0
```

```yaml
Id:      net.synedra:validatorfx
Project: ValidatorFX
URL:     https://github.com/effad/ValidatorFX
License: BSD 3-Clause
```

```yaml
Id:      org.antlr:antlr4-runtime
Project: ANTLR 4
URL:     http://www.antlr.org/
License: BSD-3-Clause
```

```yaml
Id:      org.apache.commons:*
Project: Apache Commons *
URL:     https://commons.apache.org/
License: Apache-2.0
```

```yaml
Id:      org.apache.lucene:*
Project: Apache Lucene
URL:     https://lucene.apache.org/
License: Apache-2.0
```

```yaml
Id:      org.apache.pdfbox:fontbox
Project: Apache PDFBox
URL:     http://pdfbox.apache.org
License: Apache-2.0
```

```yaml
Id:      org.apache.pdfbox:jempbox
Project: Apache PDFBox
URL:     http://pdfbox.apache.org
License: Apache-2.0
```

```yaml
Id:      org.apache.pdfbox:pdfbox
Project: Apache PDFBox
URL:     http://pdfbox.apache.org
License: Apache-2.0
```

```yaml
Id:      org.apiguardian:apiguardian-api
Project: @API Guardian
URL:     https://github.com/apiguardian-team/apiguardian
License: Apache-2.0
```

```yaml
Id:      org.bouncycastle:bcprov-jdk15on
Project: The Legion of the Bouncy Castle
URL:     https://www.bouncycastle.org/
License: MIT
```

```yaml
Id:      org.citationstyles.styles
Project: CSL Styles
URL:     https://github.com/citation-style-language/styles
Licence: Creative Commons Attribution-ShareAlike 3.0 Unported license
```

```yaml
Id:      org.citationstyles.locales
Project: CSL Locales
URL:     https://github.com/citation-style-language/locales
Licence: CC-BY-SA-3.0
```

```yaml
Id:      org.controlsfx:controlsfx
Project: ControlsFX
URL:     http://fxexperience.com/controlsfx/
License: BSD-3-Clause
```

```yaml
Id:      org.eclipse.jgit:org.eclipse.jgit
Project: Eclipse JGit
URL:     https://www.eclipse.org/jgit/
License: BSD-3-Clause
```

```yaml
Id:      org.fxmisc.flowless:flowless
Project: Flowless
URL:     https://github.com/TomasMikula/Flowless
License: BSD-2-Clause
```

```yaml
Id:      org.fxmisc.richtext:richtextfx
Project: RichTextFX
URL:     https://github.com/TomasMikula/RichTextFX
License: BSD-2-Clause
```

```yaml
Id:      org.glassfish.*
Project: Eclipse GlassFish
URL:     https://glassfish.org/
License: BSD-3-Clause (with copyright as described in Eclipse Distribution License - v 1.0 - see https://wiki.spdx.org/view/Legal_Team/License_List/Licenses_Under_Consideration for details)
```

```yaml
Id:      com.ibm.icu:*
Project: International Components for Unicode
URL:     https://icu.unicode.org/
License: Unicode License (https://www.unicode.org/copyright.html)
Note:    Our own fork https://github.com/JabRef/icu. [Upstream PR](https://github.com/unicode-org/icu/pull/2127)
Path:    lib/icu4j.jar
SourcePath: lib/ic4j-src.jar
```

```yaml
Id:      org.jabref:easybind
Project: EasyBind
URL:     https://github.com/JabRef/EasyBind
License: BSD-2-Clause
```

```yaml
Id:      org.jooq:jool
Project: JOOλ
URL:     https://github.com/jOOQ/jOOL
License: Apache-2.0
```

```yaml
Id:      org.jsoup:jsoup
Project: jsoup
URL:     https://github.com/jhy/jsoup/
License: MIT
```

```yaml
Id:      org.jspecify:jspecify
Project: jspecify
URL:     https://jspecify.dev/
License: Apache-2.0
```

```yaml
Id:      org.kordamp.ikonli
Project: Ikonli
URL:     https://kordamp.org/ikonli/
License: Apache-2.0
```

```yaml
Id:      org.mariadb.jdbc:mariadb-java-client
Project: MariaDB Java Client
URL:     https://mariadb.com/kb/en/library/about-mariadb-connector-j/
License: LGPL-2.1-or-later
```

```yaml
Id:      org.openjfx:javafx-base
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-controls
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-fxml
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-graphics
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-media
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-swing
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-web
Project: JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.libreoffice:libreoffice
Project: LibreOffice
URL:     https://api.libreoffice.org/
License: MPL-2.0 OR LGPL 3.0+
```

```yaml
Id:      org.libreoffice:unloader
Project: LibreOffice UNO Loader
URL:     https://api.libreoffice.org/
License: MPL-2.0 AND Apache-2.0
```

```yaml
Id:      org.tinylog:slf4j-tinylog
Project: tinylog 2
URL:     https://github.com/tinylog-org/tinylog
License: Apache-2.0
```

```yaml
Id:      org.tinylog:tinylog-api
Project: tinylog 2
URL:     https://github.com/tinylog-org/tinylog
License: Apache-2.0
```

```yaml
Id:      org.tinylog:tinylog-impl
Project: tinylog 2
URL:     https://github.com/tinylog-org/tinylog
License: Apache-2.0
```

```yaml
Id:      org.yaml:snakeyaml
Project: SnakeYAML
URL:     https://bitbucket.org/snakeyaml/snakeyaml-engine/src/master/
License: Apache-2.0
```

```yaml
Id:      pt.davidafsilva.apple:jkeychain
Project: JKeyChain
URL:     https://github.com/davidafsilva/jkeychain
License: BSD-2-Clause
```

```yaml
Id:      tech.units:indriya
Project: Indriya - JSR 385 - Reference Implementation
URL:     https://github.com/unitsofmeasurement/indriya
License: BSD-3-Clause
```

```yaml
Id:      tech.uom.lib:uom-lib-common
Project: Units of Measurement Libraries - extending and complementing JSR 385
URL:     https://github.com/unitsofmeasurement/uom-lib
License: BSD-3-Clause
```

## Sorted list of runtime dependencies output by gradle

1. `./gradlew dependencyReport --configuration compileClasspath`
2. Fix `build/reports/project/dependencies.txt`

   - Change line endings to `LF`
   - Remove text above and below the tree

3. (on WSL) `sed 's/[^a-z]*//' < build/reports/project/dependencies.txt | sed "s/\(.*\) .*/\1/" | grep -v "\->" | sort | uniq > build/dependencies-for-external-libraries.txt`

```text
ai.djl.huggingface:tokenizers:0.30.0
ai.djl.pytorch:pytorch-engine:0.30.0
ai.djl.pytorch:pytorch-model-zoo:0.30.0
ai.djl:api:0.30.0
ai.djl:bom:0.30.0
at.favre.lib:hkdf:1.1.0
com.dlsc.gemsfx:gemsfx:2.48.0
com.dlsc.pickerfx:pickerfx:1.3.1
com.dlsc.unitfx:unitfx:1.0.10
com.fasterxml.jackson.core:jackson-annotations:2.17.2
com.fasterxml.jackson.core:jackson-core:2.17.2
com.fasterxml.jackson.core:jackson-databind:2.17.2
com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2
com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2
com.fasterxml.jackson:jackson-bom:2.17.2
com.fasterxml:aalto-xml:1.3.3
com.github.hypfvieh:dbus-java-core:4.2.1
com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.2.1
com.github.javakeyring:java-keyring:1.0.4
com.github.sialcasa.mvvmFX:mvvmfx-validation:f195849ca9
com.github.tomtung:latex2unicode_2.13:0.3.2
com.github.vatbub:mslinks:1.0.6.2
com.github.weisj:jsvg:1.2.0
com.google.code.gson:gson:2.11.0
com.google.errorprone:error_prone_annotations:2.27.0
com.google.guava:failureaccess:1.0.2
com.google.guava:guava:33.1.0-jre
com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
com.google.j2objc:j2objc-annotations:3.0.0
com.googlecode.javaewah:JavaEWAH:1.2.3
com.googlecode.plist:dd-plist:1.28
com.h2database:h2-mvstore:2.3.232
com.knuddels:jtokkit:1.1.0
com.kohlschutter.junixsocket:junixsocket-common:2.10.0
com.kohlschutter.junixsocket:junixsocket-core:2.10.0
com.kohlschutter.junixsocket:junixsocket-mysql:2.10.0
com.kohlschutter.junixsocket:junixsocket-native-common:2.10.0
com.konghq:unirest-java-core:4.4.4
com.konghq:unirest-modules-gson:4.4.4
com.oracle.ojdbc:ojdbc10:19.3.0.0
com.oracle.ojdbc:ons:19.3.0.0
com.oracle.ojdbc:osdt_cert:19.3.0.0
com.oracle.ojdbc:osdt_core:19.3.0.0
com.oracle.ojdbc:simplefan:19.3.0.0
com.oracle.ojdbc:ucp:19.3.0.0
com.sun.istack:istack-commons-runtime:4.1.2
com.vladsch.flexmark:flexmark-ext-emoji:0.64.8
com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:0.64.8
com.vladsch.flexmark:flexmark-ext-ins:0.64.8
com.vladsch.flexmark:flexmark-ext-superscript:0.64.8
com.vladsch.flexmark:flexmark-ext-tables:0.64.8
com.vladsch.flexmark:flexmark-ext-wikilink:0.64.8
com.vladsch.flexmark:flexmark-html2md-converter:0.64.8
com.vladsch.flexmark:flexmark-jira-converter:0.64.8
com.vladsch.flexmark:flexmark-util-ast:0.64.8
com.vladsch.flexmark:flexmark-util-builder:0.64.8
com.vladsch.flexmark:flexmark-util-collection:0.64.8
com.vladsch.flexmark:flexmark-util-data:0.64.8
com.vladsch.flexmark:flexmark-util-dependency:0.64.8
com.vladsch.flexmark:flexmark-util-format:0.64.8
com.vladsch.flexmark:flexmark-util-html:0.64.8
com.vladsch.flexmark:flexmark-util-misc:0.64.8
com.vladsch.flexmark:flexmark-util-options:0.64.8
com.vladsch.flexmark:flexmark-util-sequence:0.64.8
com.vladsch.flexmark:flexmark-util-visitor:0.64.8
com.vladsch.flexmark:flexmark-util:0.64.8
com.vladsch.flexmark:flexmark:0.64.8
commons-beanutils:commons-beanutils:1.9.4
commons-cli:commons-cli:1.9.0
commons-codec:commons-codec:1.17.1
commons-collections:commons-collections:3.2.2
commons-digester:commons-digester:2.1
commons-io:commons-io:2.16.1
commons-logging:commons-logging:1.3.4
commons-validator:commons-validator:1.8.0
de.rototor.jeuclid:jeuclid-core:3.1.11
de.rototor.snuggletex:snuggletex-core:1.3.0
de.rototor.snuggletex:snuggletex-jeuclid:1.3.0
de.rototor.snuggletex:snuggletex:1.3.0
de.saxsys:mvvmfx:1.8.0
de.swiesend:secret-service:1.8.1-jdk17
de.undercouch:citeproc-java:3.1.0
eu.lestard:doc-annotations:0.2
info.debatty:java-string-similarity:2.0.0
io.github.java-diff-utils:java-diff-utils:4.12
io.zonky.test:embedded-postgres:2.0.7
jakarta.activation:jakarta.activation-api:2.1.3
jakarta.annotation:jakarta.annotation-api:2.1.1
jakarta.inject:jakarta.inject-api:2.0.1
jakarta.validation:jakarta.validation-api:3.0.2
jakarta.ws.rs:jakarta.ws.rs-api:4.0.0
jakarta.xml.bind:jakarta.xml.bind-api:4.0.2
javax.measure:unit-api:2.2
net.harawata:appdirs:1.2.2
net.java.dev.jna:jna-platform:5.13.0
net.java.dev.jna:jna:5.14.0
net.jcip:jcip-annotations:1.0
net.jodah:typetools:0.6.1
net.synedra:validatorfx:0.5.0
one.jpro.jproutils:tree-showing:0.2.2
org.antlr:antlr4-runtime:4.13.2
org.apache.commons:commons-compress:1.27.1
org.apache.commons:commons-csv:1.11.0
org.apache.commons:commons-lang3:3.17.0
org.apache.commons:commons-text:1.12.0
org.apache.httpcomponents.client5:httpclient5:5.3.1
org.apache.httpcomponents.core5:httpcore5-h2:5.2.4
org.apache.httpcomponents.core5:httpcore5:5.2.4
org.apache.logging.log4j:log4j-api:2.24.0
org.apache.logging.log4j:log4j-to-slf4j:2.24.0
org.apache.lucene:lucene-analysis-common:9.11.1
org.apache.lucene:lucene-core:9.11.1
org.apache.lucene:lucene-highlighter:9.11.1
org.apache.lucene:lucene-queries:9.11.1
org.apache.lucene:lucene-queryparser:9.11.1
org.apache.lucene:lucene-sandbox:9.11.1
org.apache.pdfbox:fontbox:3.0.3
org.apache.pdfbox:pdfbox-io:3.0.3
org.apache.pdfbox:pdfbox:3.0.3
org.apache.pdfbox:xmpbox:3.0.3
org.apiguardian:apiguardian-api:1.1.2
org.bouncycastle:bcprov-jdk18on:1.78.1
org.checkerframework:checker-qual:3.42.0
org.codehaus.woodstox:stax2-api:4.2.2
org.controlsfx:controlsfx:11.2.1
org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r
org.fxmisc.flowless:flowless:0.7.3
org.fxmisc.richtext:richtextfx:0.11.3
org.fxmisc.undo:undofx:2.1.1
org.fxmisc.wellbehaved:wellbehavedfx:0.3.3
org.glassfish.grizzly:grizzly-framework:4.0.2
org.glassfish.grizzly:grizzly-http-server:4.0.2
org.glassfish.grizzly:grizzly-http:4.0.2
org.glassfish.hk2.external:aopalliance-repackaged:3.1.1
org.glassfish.hk2:hk2-api:3.1.1
org.glassfish.hk2:hk2-locator:3.0.6
org.glassfish.hk2:hk2-utils:3.1.1
org.glassfish.hk2:osgi-resource-locator:1.0.3
org.glassfish.jaxb:jaxb-core:4.0.3
org.glassfish.jaxb:jaxb-runtime:4.0.3
org.glassfish.jaxb:txw2:4.0.3
org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.8
org.glassfish.jersey.core:jersey-client:3.1.8
org.glassfish.jersey.core:jersey-common:3.1.8
org.glassfish.jersey.core:jersey-server:3.1.8
org.glassfish.jersey.inject:jersey-hk2:3.1.8
org.jabref:afterburner.fx:2.0.0
org.jabref:easybind:2.2.1-SNAPSHOT
org.javassist:javassist:3.30.2-GA
org.jbibtex:jbibtex:1.0.20
org.jetbrains:annotations:24.0.1
org.jooq:jool:0.9.15
org.jsoup:jsoup:1.18.1
org.jspecify:jspecify:1.0.0
org.kordamp.ikonli:ikonli-bootstrapicons-pack:12.3.1
org.kordamp.ikonli:ikonli-core:12.3.1
org.kordamp.ikonli:ikonli-javafx:12.3.1
org.kordamp.ikonli:ikonli-material-pack:12.3.1
org.kordamp.ikonli:ikonli-materialdesign-pack:12.3.1
org.kordamp.ikonli:ikonli-materialdesign2-pack:12.3.1
org.libreoffice:libreoffice:24.2.3
org.libreoffice:unoloader:24.2.3
org.mariadb.jdbc:mariadb-java-client:2.7.9
org.openjfx:javafx-base:23
org.openjfx:javafx-controls:23
org.openjfx:javafx-fxml:23
org.openjfx:javafx-graphics:23
org.openjfx:javafx-media:23
org.openjfx:javafx-swing:23
org.openjfx:javafx-web:23
org.postgresql:postgresql:42.7.4
org.reactfx:reactfx:2.0-M5
org.scala-lang:scala-library:2.13.8
org.slf4j:jul-to-slf4j:2.0.16
org.slf4j:slf4j-api:2.0.16
org.tinylog:slf4j-tinylog:2.7.0
org.tinylog:tinylog-api:2.7.0
org.tinylog:tinylog-impl:2.7.0
org.tukaani:xz:1.9
org.yaml:snakeyaml:2.3
pt.davidafsilva.apple:jkeychain:1.1.0
tech.units:indriya:2.2
tech.uom.lib:uom-lib-common:2.2
```
