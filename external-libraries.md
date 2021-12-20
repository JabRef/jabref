# External libraries

This document lists the fonts, icons, and libraries used by JabRef.
This file is manually kept in sync with build.gradle and the binary jars contained in the lib/ directory.

One can list all dependencies by using Gradle task `dependencyReport`.
It generates the file [build/reports/project/dependencies.txt](build/reports/project/dependencies.txt).
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
Id:      com.fasterxml.jackson
Project: Jackson Project
URL:     https://github.com/FasterXML/jackson
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
Id:      com.jfoenix:jfoenix
Project: JavaFX MAterial Design Library
URL:     https://github.com/jfoenixadmin/JFoenix
License: Apache-2.0
```

```yaml
Id:      com.konghq.unirest
Project: Unirest for Java
URL:     https://github.com/Kong/unirest-java
License: MIT
```

```yaml
Id:      com.microsoft.azure:applicationinsights-core
Project: Application Insights SDK for Java
URL:     https://github.com/Microsoft/ApplicationInsights-Java
License: MIT
```

```yaml
Id:      com.microsoft.azure:applicationinsights-logging-log4j2
Project: Application Insights SDK for Java
URL:     https://github.com/Microsoft/ApplicationInsights-Java
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
Id:      com.sun.xml.fastinfoset:FastInfoset
Project: Fast Infoset
URL:     https://github.com/eclipse-ee4j/jaxb-fi
License: Apache-2.0
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
Id:      de.saxsys:mvvmfx
Project: mvvm(fx)
URL:     https://github.com/sialcasa/mvvmFX
License: Apache-2.0
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
Id:      jakarta.activation:jakarata.activation-api
Project: Jakarta Activation
URL:     https://eclipse-ee4j.github.io/jaf/
License: BSD-3-Clause
```

```yaml
Id:      jakarta.annotation:jakarata.annotation-api
Project: Jakarta Annotations
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
Id:      org.antlr:antlr-runtime
Project: ANTLR 3
URL:     http://www.antlr3.org/
License: BSD-3-Clause
```

```yaml
Id:      org.antlr:antlr4-runtime
Project: ANTLR 4
URL:     http://www.antlr.org/
License: BSD-3-Clause
```

```yaml
Id:      org.apache.commons:commons-csv
Project: Apache Commons CSV
URL:     https://commons.apache.org/proper/commons-csv/
License: Apache-2.0
```

```yaml
Id:      org.apache.commons:commons-lang3
Project: Apache Commons Lang
URL:     https://commons.apache.org/proper/commons-lang/
License: Apache-2.0
```

```yaml
Id:      org.apache.commons:commons-text
Project: Apache Commons Text
URL:     https://commons.apache.org/proper/commons-text/
License: Apache-2.0
```

```yaml
Id:      org.apache.lucene:lucene-core
Project: Apache Lucene
URL:     https://lucene.apache.org/
License: Apache-2.0
```

```yaml
Id:      org.apache.lucene:lucene-queries
Project: Apache Lucene
URL:     https://lucene.apache.org/
License: Apache-2.0
```

```yaml
Id:      org.apache.lucene:lucene-queryparser
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
Id:      org.apache.tika:tika-core
Project: Apache Tika
URL:     https://tika.apache.org/
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
URL:     https://bitbucket.org/asomov/snakeyaml-engine/src/master/
License: Apache-2.0
```

## Sorted list of runtime dependencies output by gradle

1. `gradlew dependencies > build\dependencies.txt`
2. Manually edit depedencies.txt to contain the tree of "compileClasspath" and "implementation" only. Otherwise, libraries such as "Apache Commons Lang 3" are missed.
3. (on WSL) `sed 's/[^a-z]*//' < build/dependencies.txt | sed "s/\(.*\) .*/\1/" | grep -v "\->" | sort | uniq > build/dependencies-for-external-libraries.txt`

```text
com.fasterxml.jackson.core:jackson-annotations:2.13.0
com.fasterxml.jackson.core:jackson-core:2.13.0
com.fasterxml.jackson.core:jackson-databind:2.13.0
com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.0
com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0
com.fasterxml.jackson:jackson-bom:2.13.0
com.github.tomtung:latex2unicode_2.12:0.2.6
com.google.code.gson:gson:2.8.8
com.google.errorprone:error_prone_annotations:2.7.1
com.google.guava:failureaccess:1.0.1
com.google.guava:guava:31.0.1-jre
com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
com.google.j2objc:j2objc-annotations:1.3
com.googlecode.javaewah:JavaEWAH:1.1.12
com.h2database:h2-mvstore:2.0.202
com.jfoenix:jfoenix:9.0.10
com.konghq:unirest-java:3.13.4
com.microsoft.azure:applicationinsights-core:2.4.1
com.microsoft.azure:applicationinsights-logging-log4j2:2.4.1
com.oracle.ojdbc:ojdbc10:19.3.0.0
com.oracle.ojdbc:ons:19.3.0.0
com.oracle.ojdbc:osdt_cert:19.3.0.0
com.oracle.ojdbc:osdt_core:19.3.0.0
com.oracle.ojdbc:simplefan:19.3.0.0
com.oracle.ojdbc:ucp:19.3.0.0
com.sun.istack:istack-commons-runtime:3.0.8
com.sun.xml.fastinfoset:FastInfoset:1.2.16
com.tobiasdiez:easybind:2.2
com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:0.62.2
com.vladsch.flexmark:flexmark-ext-gfm-tasklist:0.62.2
com.vladsch.flexmark:flexmark-util-ast:0.62.2
com.vladsch.flexmark:flexmark-util-builder:0.62.2
com.vladsch.flexmark:flexmark-util-collection:0.62.2
com.vladsch.flexmark:flexmark-util-data:0.62.2
com.vladsch.flexmark:flexmark-util-dependency:0.62.2
com.vladsch.flexmark:flexmark-util-format:0.62.2
com.vladsch.flexmark:flexmark-util-html:0.62.2
com.vladsch.flexmark:flexmark-util-misc:0.62.2
com.vladsch.flexmark:flexmark-util-options:0.62.2
com.vladsch.flexmark:flexmark-util-sequence:0.62.2
com.vladsch.flexmark:flexmark-util-visitor:0.62.2
com.vladsch.flexmark:flexmark-util:0.62.2
com.vladsch.flexmark:flexmark:0.62.2
commons-cli:commons-cli:1.5.0
commons-codec:commons-codec:1.11
commons-io:commons-io:2.11.0
commons-logging:commons-logging:1.2
de.saxsys:mvvmfx-validation:1.9.0-SNAPSHOT
de.saxsys:mvvmfx:1.8.0
de.undercouch:citeproc-java:3.0.0-alpha.3
eu.lestard:doc-annotations:0.2
info.debatty:java-string-similarity:2.0.0
io.github.java-diff-utils:java-diff-utils:4.11
jakarta.activation:jakarta.activation-api:1.2.1
jakarta.annotation:jakarta.annotation-api:1.3.5
jakarta.xml.bind:jakarta.xml.bind-api:2.3.2
net.harawata:appdirs:1.2.1
net.java.dev.jna:jna-platform:5.6.0
net.java.dev.jna:jna:5.6.0
net.jcip:jcip-annotations:1.0
net.jodah:typetools:0.6.1
org.antlr:antlr-runtime:3.5.2
org.antlr:antlr4-runtime:4.9.3
org.apache.commons:commons-csv:1.9.0
org.apache.commons:commons-lang3:3.9
org.apache.commons:commons-text:1.8
org.apache.httpcomponents:httpasyncclient:4.1.4
org.apache.httpcomponents:httpclient:4.5.13
org.apache.httpcomponents:httpcore-nio:4.4.13
org.apache.httpcomponents:httpcore:4.4.13
org.apache.httpcomponents:httpmime:4.5.13
org.apache.pdfbox:fontbox:2.0.25
org.apache.pdfbox:pdfbox:2.0.25
org.apache.pdfbox:xmpbox:2.0.24
org.apache.tika:tika-core:2.2.0
org.bouncycastle:bcprov-jdk15on:1.70
org.checkerframework:checker-qual:3.12.0
org.codehaus.mojo:animal-sniffer-annotations:1.18
org.controlsfx:controlsfx:11.1.1
org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r
org.fxmisc.flowless:flowless:0.6.7
org.fxmisc.richtext:richtextfx:0.10.7
org.fxmisc.undo:undofx:2.1.1
org.fxmisc.wellbehaved:wellbehavedfx:0.3.3
org.glassfish.hk2.external:jakarta.inject:2.6.1
org.glassfish.jaxb:jaxb-runtime:2.3.2
org.glassfish.jaxb:txw2:2.3.2
org.jbibtex:jbibtex:1.0.17
org.jetbrains:annotations:15.0
org.jsoup:jsoup:1.14.3
org.jvnet.staxex:stax-ex:1.8.1
org.kordamp.ikonli:ikonli-core:12.2.0
org.kordamp.ikonli:ikonli-javafx:12.2.0
org.kordamp.ikonli:ikonli-materialdesign2-pack:12.2.0
org.libreoffice:libreoffice:7.2.3
org.libreoffice:unoloader:7.2.2
org.mariadb.jdbc:mariadb-java-client:2.7.4
org.openjfx:javafx-base:17.0.1
org.openjfx:javafx-controls:17.0.1
org.openjfx:javafx-fxml:17.0.1
org.openjfx:javafx-graphics:17.0.1
org.openjfx:javafx-media:17.0.1
org.openjfx:javafx-swing:17.0.1
org.openjfx:javafx-web:17.0.1
org.postgresql:postgresql:42.3.1
org.reactfx:reactfx:2.0-M5
org.scala-lang:scala-library:2.12.8
org.slf4j:slf4j-api:2.0.0-alpha5
org.tinylog:slf4j-tinylog:2.4.1
org.tinylog:tinylog-api:2.4.1
org.tinylog:tinylog-impl:2.4.1
org.yaml:snakeyaml:1.28
```
