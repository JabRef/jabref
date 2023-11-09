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
Id:      com.squareup.okhttp3:okhttp
Project: OkHttp
URL:     https://square.github.io/okhttp/
License: Apache-2.0
```

```yaml
Id:      com.squareup.okio:okio
Project: OkHttp
URL:     https://github.com/square/okio/
License: Apache-2.0
```

```yaml
Id:      com.squareup.retrofit2:retrofit
Project: Retrofit 2
URL:     https://github.com/square/retrofit
License: Apache-2.0
```

```yaml
Id:      com.sun.istack:istack-commons-runtime
Project: iStack Common Utility Code
URL:     https://github.com/eclipse-ee4j/jaxb-istack-commons
License: BSD-3-Clause (with copyright as described in Eclipse Distribution License - v 1.0 - see https://wiki.spdx.org/view/Legal_Team/License_List/Licenses_Under_Consideration for details)
```

```yaml
Id:      com.tobiasdiez:easybind
Project: EasyBind
URL:     https://github.com/tobiasdiez/EasyBind
License: BSD-2-Clause
```

```yaml
Id:      com.vladsch.flexmark:flexmark-all
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
Id:      io.github.java-diff-utils:java-diff-utils
Project: java-diff-utils
URL:     https://github.com/java-diff-utils/java-diff-utils
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
Id:      org.antlr:antlr4-runtime
Project: ANTLR 4
URL:     http://www.antlr.org/
License: BSD-3-Clause
```

```yaml
Id:      org.apache.commons:*
Project: Apache Commons CSV
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
Note:    Our own fork https://github.com/JabRef/icu. Upstream PR: https://github.com/unicode-org/icu/pull/2127
Path:    lib/icu4j.jar
SourcePath: lib/ic4j-src.jar
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
Project  JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-controls
Project  JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-fxml
Project  JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-graphics
Project  JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-media
Project  JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-swing
Project  JavaFX
URL:     https://openjfx.io/
License: GPL-2.0 WITH Classpath-exception-2.0
```

```yaml
Id:      org.openjfx:javafx-web
Project  JavaFX
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

1. `./gradlew dependencies > build/dependencies.txt`
2. Manually edit depedencies.txt to contain the tree of "compileClasspath" and "implementation" only. Otherwise, libraries such as "Apache Commons Lang 3" are missed.
3. (on WSL) `sed 's/[^a-z]*//' < build/dependencies.txt | sed "s/\(.*\) .*/\1/" | grep -v "\->" | sort | uniq > build/dependencies-for-external-libraries.txt`

```text
at.favre.lib:hkdf:1.1.0
com.dlsc.gemsfx:gemsfx:1.82.0
com.dlsc.pickerfx:pickerfx:1.2.0
com.dlsc.unitfx:unitfx:1.0.10
com.fasterxml.jackson.core:jackson-annotations:2.15.3
com.fasterxml.jackson.core:jackson-core:2.15.3
com.fasterxml.jackson.core:jackson-databind:2.15.3
com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3
com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3
com.fasterxml.jackson:jackson-bom:2.15.3
com.fasterxml:aalto-xml:1.3.1
com.github.hypfvieh:dbus-java-core:4.2.1
com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.2.1
com.github.javakeyring:java-keyring:1.0.4
com.github.sialcasa.mvvmFX:mvvmfx-validation:f195849ca9
com.github.tomtung:latex2unicode_2.13:0.3.2
com.google.code.gson:gson:2.10
com.google.errorprone:error_prone_annotations:2.21.1
com.google.guava:failureaccess:1.0.1
com.google.guava:guava:32.1.3-jre
com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
com.google.j2objc:j2objc-annotations:2.8
com.googlecode.javaewah:JavaEWAH:1.2.3
com.h2database:h2-mvstore:2.2.224
com.konghq:unirest-java:3.14.5
com.oracle.ojdbc:ojdbc10:19.3.0.0
com.oracle.ojdbc:ons:19.3.0.0
com.oracle.ojdbc:osdt_cert:19.3.0.0
com.oracle.ojdbc:osdt_core:19.3.0.0
com.oracle.ojdbc:simplefan:19.3.0.0
com.oracle.ojdbc:ucp:19.3.0.0
com.squareup.okhttp3:okhttp:3.12.0
com.squareup.okio:okio:1.15.0
com.squareup.retrofit2:retrofit:2.6.1
com.sun.istack:istack-commons-runtime:4.1.2
com.tobiasdiez:easybind:2.2.1-SNAPSHOT
com.vladsch.flexmark:flexmark-util-ast:0.64.8
com.vladsch.flexmark:flexmark-util-builder:0.64.8
com.vladsch.flexmark:flexmark-util-collection:0.64.8
com.vladsch.flexmark:flexmark-util-data:0.64.8
com.vladsch.flexmark:flexmark-util-dependency:0.64.8
com.vladsch.flexmark:flexmark-util-format:0.64.8
com.vladsch.flexmark:flexmark-util-html:0.64.8
com.vladsch.flexmark:flexmark-util-misc:0.64.8
com.vladsch.flexmark:flexmark-util-sequence:0.64.8
com.vladsch.flexmark:flexmark-util-visitor:0.64.8
com.vladsch.flexmark:flexmark:0.64.8
commons-beanutils:commons-beanutils:1.9.4
commons-cli:commons-cli:1.5.0
commons-codec:commons-codec:1.16.0
commons-collections:commons-collections:3.2.2
commons-digester:commons-digester:2.1
commons-logging:commons-logging:1.2
commons-validator:commons-validator:1.7
de.rototor.jeuclid:jeuclid-core:3.1.11
de.rototor.snuggletex:snuggletex-core:1.3.0
de.rototor.snuggletex:snuggletex-jeuclid:1.3.0
de.rototor.snuggletex:snuggletex:1.3.0
de.saxsys:mvvmfx:1.8.0
de.swiesend:secret-service:1.8.1-jdk17
de.undercouch:citeproc-java:3.0.0-beta.2
eu.lestard:doc-annotations:0.2
info.debatty:java-string-similarity:2.0.0
io.github.java-diff-utils:java-diff-utils:4.12
jakarta.activation:jakarta.activation-api:2.1.2
jakarta.annotation:jakarta.annotation-api:2.1.1
jakarta.inject:jakarta.inject-api:2.0.1
jakarta.validation:jakarta.validation-api:3.0.2
jakarta.ws.rs:jakarta.ws.rs-api:3.1.0
jakarta.xml.bind:jakarta.xml.bind-api:4.0.1
javax.measure:unit-api:2.1.2
net.harawata:appdirs:1.2.2
net.java.dev.jna:jna-platform:5.13.0
net.java.dev.jna:jna:5.13.0
net.jcip:jcip-annotations:1.0
net.jodah:typetools:0.6.1
one.jpro.jproutils:tree-showing:0.2.2
org.antlr:antlr4-runtime:4.13.1
org.apache.commons:commons-csv:1.10.0
org.apache.commons:commons-lang3:3.13.0
org.apache.httpcomponents:httpasyncclient:4.1.5
org.apache.httpcomponents:httpclient:4.5.13
org.apache.httpcomponents:httpcore-nio:4.4.13
org.apache.httpcomponents:httpcore:4.4.13
org.apache.httpcomponents:httpmime:4.5.13
org.apache.logging.log4j:log4j-api:2.20.0
org.apache.logging.log4j:log4j-to-slf4j:2.20.0
org.apache.lucene:lucene-analysis-common:9.8.0
org.apache.lucene:lucene-core:9.8.0
org.apache.lucene:lucene-highlighter:9.8.0
org.apache.lucene:lucene-queries:9.8.0
org.apache.lucene:lucene-queryparser:9.8.0
org.apache.lucene:lucene-sandbox:9.8.0
org.apache.pdfbox:fontbox:3.0.0
org.apache.pdfbox:pdfbox-io:3.0.0
org.apache.pdfbox:pdfbox:3.0.0
org.apache.pdfbox:xmpbox:3.0.0
org.bouncycastle:bcprov-jdk18on:1.76
org.checkerframework:checker-qual:3.37.0
org.codehaus.woodstox:stax2-api:4.2
org.controlsfx:controlsfx:11.1.2
org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r
org.fxmisc.flowless:flowless:0.7.1
org.fxmisc.richtext:richtextfx:0.11.1
org.fxmisc.undo:undofx:2.1.1
org.fxmisc.wellbehaved:wellbehavedfx:0.3.3
org.glassfish.grizzly:grizzly-framework:4.0.0
org.glassfish.grizzly:grizzly-http-server:4.0.0
org.glassfish.grizzly:grizzly-http:4.0.0
org.glassfish.hk2.external:aopalliance-repackaged:3.0.4
org.glassfish.hk2:hk2-api:3.0.4
org.glassfish.hk2:hk2-locator:3.0.4
org.glassfish.hk2:hk2-utils:3.0.4
org.glassfish.hk2:osgi-resource-locator:1.0.3
org.glassfish.jaxb:jaxb-core:4.0.3
org.glassfish.jaxb:jaxb-runtime:4.0.3
org.glassfish.jaxb:txw2:4.0.3
org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.3
org.glassfish.jersey.core:jersey-client:3.1.3
org.glassfish.jersey.core:jersey-common:3.1.3
org.glassfish.jersey.core:jersey-server:3.1.3
org.glassfish.jersey.inject:jersey-hk2:3.1.3
org.jabref:afterburner.fx:2.0.0
org.javassist:javassist:3.29.2-GA
org.jbibtex:jbibtex:1.0.20
org.jetbrains:annotations:24.0.1
org.jooq:jool:0.9.15
org.jsoup:jsoup:1.16.1
org.kordamp.ikonli:ikonli-bootstrapicons-pack:12.3.1
org.kordamp.ikonli:ikonli-core:12.3.1
org.kordamp.ikonli:ikonli-javafx:12.3.1
org.kordamp.ikonli:ikonli-material-pack:12.3.1
org.kordamp.ikonli:ikonli-materialdesign-pack:12.3.1
org.kordamp.ikonli:ikonli-materialdesign2-pack:12.3.1
org.libreoffice:libreoffice:7.6.1
org.libreoffice:unoloader:7.6.1
org.mariadb.jdbc:mariadb-java-client:2.7.9
org.openjfx:javafx-base:20.0.2
org.openjfx:javafx-controls:20.0.2
org.openjfx:javafx-fxml:20.0.2
org.openjfx:javafx-graphics:20.0.2
org.openjfx:javafx-media:20.0.2
org.openjfx:javafx-swing:20.0.2
org.openjfx:javafx-web:20.0.2
org.postgresql:postgresql:42.6.0
org.reactfx:reactfx:2.0-M5
org.scala-lang:scala-library:2.13.8
org.slf4j:jul-to-slf4j:2.0.9
org.slf4j:slf4j-api:2.0.9
org.tinylog:slf4j-tinylog:2.6.2
org.tinylog:tinylog-api:2.6.2
org.tinylog:tinylog-impl:2.6.2
org.yaml:snakeyaml:2.1
pt.davidafsilva.apple:jkeychain:1.1.0
tech.units:indriya:2.1.2
tech.uom.lib:uom-lib-common:2.1
```
