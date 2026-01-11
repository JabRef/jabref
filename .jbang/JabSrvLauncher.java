///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabsrv - serve BibTeX files using JabRef

//JAVA 24+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

// raw is for https://github.com/unicode-org/icu/pull/2127
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/

//DEPS org.jabref:jablib:6.0-SNAPSHOT

// from jabsrv-cli
//DEPS info.picocli:picocli:4.7.7

// from jabsrv
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.tinylog:slf4j-tinylog:2.7.0
//DEPS org.tinylog:tinylog-impl:2.7.0
//DEPS org.slf4j:jul-to-slf4j:2.0.17
//DEPS org.apache.logging.log4j:log4j-to-slf4j:2.25.3
//DEPS info.picocli:picocli:4.7.7
//DEPS org.postgresql:postgresql:42.7.8
//DEPS org.bouncycastle:bcprov-jdk18on:1.83
//DEPS com.konghq:unirest-modules-gson:4.7.0
//DEPS jakarta.ws.rs:jakarta.ws.rs-api:4.0.0
//DEPS org.glassfish.jersey.core:jersey-server:4.0.0
//DEPS org.glassfish.jersey.inject:jersey-hk2:4.0.0
//DEPS org.glassfish.hk2:hk2-api:3.1.1
//DEPS org.glassfish.hk2:hk2-utils:3.1.1
//DEPS org.glassfish.hk2:hk2-locator:3.1.1
//DEPS org.glassfish.jersey.containers:jersey-container-grizzly2-http:4.0.0
//DEPS org.glassfish.grizzly:grizzly-http-server:4.0.2
//DEPS org.glassfish.grizzly:grizzly-framework:4.0.2
//DEPS jakarta.validation:jakarta.validation-api:3.1.1
//DEPS org.hibernate.validator:hibernate-validator:9.1.0.Final
//DEPS com.konghq:unirest-modules-gson:4.7.0
//DEPS com.google.guava:guava:33.5.0-jre
//DEPS org.jabref:afterburner.fx:2.0.0
//DEPS net.harawata:appdirs:1.5.0
//DEPS de.undercouch:citeproc-java:3.4.1
//DEPS com.github.ben-manes.caffeine:caffeine:3.2.3
//DEPS tools.jackson.core:jackson-core:3.0.3
//DEPS tools.jackson.core:jackson-databind:3.0.3
//DEPS tools.jackson.dataformat:jackson-dataformat-yaml:3.0.3
//DEPS org.apache.commons:commons-lang3:3.20.0

//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/BibEntryDTO.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/cayw/SimpleJson.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/GlobalExceptionMapper.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/GsonFactory.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/LinkedPdfFileDTO.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/JabrefMediaType.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/JabRefSrvStateManager.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/SrvStateManager.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/CAYWQueryParams.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/CAYWResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/BibLatexFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/NatbibFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/MMDFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/PandocFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/TypstFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/CAYWFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/FormatterService.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/SimpleJsonFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/CAYWEntry.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SearchDialog.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SearchField.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SearchResultContainer.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SelectedItemsContainer.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/command/Command.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/command/CommandResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/command/SelectEntriesCommand.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/resources/LibrariesResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/resources/LibraryResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/resources/MapResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/resources/RootResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/services/FilesToServe.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/services/ServerUtils.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/CORSFilter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/Server.java

//FILES tinylog.properties=../jabsrv-cli/src/main/resources/tinylog.properties

// This is the main class - directly called by JBang
//SOURCES ../jabsrv-cli/src/main/java/org/jabref/http/server/cli/ServerCli.java
