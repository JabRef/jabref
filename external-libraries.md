# External libraries

This document lists the fonts, icons, and libraries used by JabRef.
This file is manually kept in sync with build.gradle and the binary jars contained in the lib/ directory.

One can list all dependencies by using Gradle task `dependencyReport`.
It generates the file [build/reports/project/dependencies.txt](build/reports/project/dependencies.txt).
There, [one can use](https://stackoverflow.com/a/49727249/873282) `sed 's/^.* //' | sort | uniq` to flatten the dependencies.

## Legend

### License

We follow the [SPDX license identifiers](https://spdx.org/licenses/).
In case you add a library, please use these identifiers.
For instance, "BSD" is not exact enough, there are numerous variants out there: BSD-2-Clause, BSD-3-Clause-No-Nuclear-Warranty, ...
Note that the SPDX license identifiers are different from the ones used by debian. See https://wiki.debian.org/Proposals/CopyrightFormat for more information.

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
Id:      com.ibm.icu:icu4j
Project: International Components for Unicode for Java (ICU4J)
URL:     https://wiki.eclipse.org/ICU4J
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
Id:      commons-logging:commons-logging
Project: Apache Commons Logging
URL:     http://commons.apache.org/logging/
License: Apache-2.0
```

```yaml
Id:      de.jensd:fontawesomefx-commons
Project: FontAwesomeFX Commons
URL:     https://bitbucket.org/Jerady/fontawesomefx
License: Apache-2.0
```

```yaml
Id:      de.jensd:fontawesomefx-materialdesignfont
Project: FontAwesomeFX Material Design Font
URL:     https://bitbucket.org/Jerady/fontawesomefx
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
Id:      org.apache.logging.log4j
Project: Apache Log2j 2
URL:     http://logging.apache.org/log4j/2.x/
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
Id:      org.openoffice:juh
Project: OpenOffice.org
URL:     http://www.openoffice.org/api/SDK
License: LGPL 3.0
```

```yaml
Id:      org.openoffice:jurt
Project: OpenOffice.org
URL:     http://www.openoffice.org/api/SDK
License: Apache-2.0
```

```yaml
Id:      org.openoffice:ridl
Project: OpenOffice.org
URL:     http://www.openoffice.org/api/SDK
License: Apache-2.0
```

```yaml
Id:      org.openoffice:unoil
Project: OpenOffice.org
URL:     http://www.openoffice.org/api/SDK
License: Apache-2.0
```

```yaml
Id:      org.ow2.asm:asm
Project: ASM
URL:     https://asm.ow2.io/
License: BSD-3-Clause
```

## Sorted list of runtime dependencies output by gradle

1. `gradlew dependencies > build\reports\project\dependencies.txt`
2. Manually edit depedencies.txt to contain the tree of "compileClasspath" and "implementation" only
3. sed 's/^.* //' < dependencies.txt | sort | uniq

```text
com.github.tomtung:latex2unicode_2.12:0.2.6
com.google.code.gson:gson:2.8.6
com.google.errorprone:error_prone_annotations:2.3.4
com.google.guava:failureaccess:1.0.1
com.google.guava:guava:29.0-jre
com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
com.google.j2objc:j2objc-annotations:1.3
com.h2database:h2-mvstore:1.4.200
com.ibm.icu:icu4j:62.1
com.jfoenix:jfoenix:9.0.10
com.konghq:unirest-java:3.10.00
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
com.tobiasdiez:easybind:2.1.0
com.vladsch.flexmark:flexmark:0.62.2
com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:0.62.2
com.vladsch.flexmark:flexmark-ext-gfm-tasklist:0.62.2
com.vladsch.flexmark:flexmark-util:0.62.2
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
commons-cli:commons-cli:1.4
commons-codec:commons-codec:1.11
commons-logging:commons-logging:1.2
de.jensd:fontawesomefx-commons:11.0
de.jensd:fontawesomefx-materialdesignfont:1.7.22-11
de.saxsys:mvvmfx:1.8.0
de.saxsys:mvvmfx-validation:1.9.0-SNAPSHOT
de.undercouch:citeproc-java:2.1.0-SNAPSHOT
eu.lestard:doc-annotations:0.2
info.debatty:java-string-similarity:2.0.0
io.github.java-diff-utils:java-diff-utils:4.7
jakarta.activation:jakarta.activation-api:1.2.1
jakarta.annotation:jakarta.annotation-api:1.3.5
jakarta.xml.bind:jakarta.xml.bind-api:2.3.2
net.jcip:jcip-annotations:1.0
net.jodah:typetools:0.6.1
org.antlr:antlr4-runtime:4.8-1
org.antlr:antlr-runtime:3.5.2
org.apache.commons:commons-csv:1.8
org.apache.commons:commons-lang3:3.9
org.apache.commons:commons-text:1.8
org.apache.httpcomponents:httpasyncclient:4.1.4
org.apache.httpcomponents:httpclient:4.5.12
org.apache.httpcomponents:httpcore:4.4.13
org.apache.httpcomponents:httpcore-nio:4.4.13
org.apache.httpcomponents:httpmime:4.5.12
org.apache.logging.log4j:log4j-api:3.0.0-SNAPSHOT
org.apache.logging.log4j:log4j-core:3.0.0-SNAPSHOT
org.apache.logging.log4j:log4j-jcl:3.0.0-SNAPSHOT
org.apache.logging.log4j:log4j-plugins:3.0.0-SNAPSHOT
org.apache.logging.log4j:log4j-slf4j18-impl:3.0.0-SNAPSHOT
org.apache.pdfbox:fontbox:2.0.20
org.apache.pdfbox:pdfbox:2.0.20
org.apache.pdfbox:xmpbox:2.0.20
org.apache.tika:tika-core:1.24.1
org.bouncycastle:bcprov-jdk15on:1.66
org.checkerframework:checker-qual:2.11.1
org.controlsfx:controlsfx:11.0.2
org.fxmisc.flowless:flowless:0.6.1
org.fxmisc.richtext:richtextfx:0.10.5
org.fxmisc.undo:undofx:2.1.0
org.fxmisc.wellbehaved:wellbehavedfx:0.3.3
org.glassfish.hk2.external:jakarta.inject:2.6.1
org.glassfish.jaxb:jaxb-runtime:2.3.2
org.glassfish.jaxb:txw2:2.3.2
org.graalvm.js:js:19.2.1
org.graalvm.regex:regex:19.2.1
org.graalvm.sdk:graal-sdk:19.2.1
org.graalvm.truffle:truffle-api:19.2.1
org.jbibtex:jbibtex:1.0.17
org.jetbrains:annotations:15.0
org.jsoup:jsoup:1.13.1
org.jvnet.staxex:stax-ex:1.8.1
org.mariadb.jdbc:mariadb-java-client:2.6.2
org.openjfx:javafx-base:14
org.openjfx:javafx-controls:14
org.openjfx:javafx-fxml:14
org.openjfx:javafx-graphics:14
org.openjfx:javafx-media:14
org.openjfx:javafx-swing:14
org.openjfx:javafx-web:14
org.ow2.asm:asm:6.2.1
org.ow2.asm:asm-analysis:6.2.1
org.ow2.asm:asm-commons:6.2.1
org.ow2.asm:asm-tree:6.2.1
org.ow2.asm:asm-util:6.2.1
org.postgresql:postgresql:42.2.16
org.reactfx:reactfx:2.0-M5
org.scala-lang:scala-library:2.12.8
org.slf4j:slf4j-api:2.0.0-alpha1
```
