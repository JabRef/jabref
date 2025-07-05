///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION jabsrv - serve BibTeX files using JabRef

//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//SOURCES ../jabsrv-cli/src/main/java/org/jabref/http/server/cli/ServerCli.java
//FILES tinylog.properties=../jabsrv-cli/src/main/resources/tinylog.properties

//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/BibEntryDTO.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/cayw/SimpleJson.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/GlobalExceptionMapper.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/GsonFactory.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/dto/LinkedPdfFileDTO.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/JabrefMediaType.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/CAYWResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/CAYWQueryParams.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/CAYWEntry.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SearchDialog.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SearchField.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SearchResultContainer.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/gui/SelectedItemsContainer.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/BibLatexFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/CAYWFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/FormatterService.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/cayw/format/SimpleJsonFormatter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/CORSFilter.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/LibrariesResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/LibraryResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/PreferencesFactory.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/RootResource.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/Server.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/services/ContextsToServe.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/services/FilesToServe.java
//SOURCES ../jabsrv/src/main/java/org/jabref/http/server/services/ServerUtils.java

// REPOS mavencentral,snapshots=https://central.sonatype.com/repository/maven-snapshots/
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots
// REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/
// REPOS mavencentral,jitpack=https://jitpack.io

// TODO: ASCII things won't work, but we accept for now to keep things going
//DEPS com.ibm.icu:icu4j:77.1

// Choose one - both should work
// https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/jabref/jablib/
// DEPS org.jabref:jablib:6.+
// https://jitpack.io/#jabref/jabref/main-SNAPSHOT
//DEPS com.github.jabref:jabref:main-SNAPSHOT

//DEPS info.picocli:picocli:4.7.7
//DEPS org.jspecify:jspecify:1.0.0

// from jabsrv
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.tinylog:slf4j-tinylog:2.7.0
//DEPS org.tinylog:tinylog-impl:2.7.0
//DEPS org.slf4j:jul-to-slf4j:2.0.17
//DEPS org.apache.logging.log4j:log4j-to-slf4j:2.25.0
//DEPS info.picocli:picocli:4.7.7
//DEPS org.postgresql:postgresql:42.7.7
//DEPS org.bouncycastle:bcprov-jdk18on:1.81
//DEPS com.konghq:unirest-modules-gson:4.4.7
//DEPS jakarta.ws.rs:jakarta.ws.rs-api:4.0.0
//DEPS org.glassfish.jersey.core:jersey-server:3.1.10
//DEPS org.glassfish.jersey.inject:jersey-hk2:3.1.10
//DEPS org.glassfish.hk2:hk2-api:3.1.1
//DEPS org.glassfish.hk2:hk2-utils:3.1.1
//DEPS org.glassfish.hk2:hk2-locator:3.1.1
//DEPS org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.10
//DEPS org.glassfish.grizzly:grizzly-http-server:4.0.2
//DEPS org.glassfish.grizzly:grizzly-framework:4.0.2
//DEPS jakarta.validation:jakarta.validation-api:3.1.1
//DEPS org.hibernate.validator:hibernate-validator:9.0.1.Final
//DEPS com.konghq:unirest-modules-gson:4.4.7
//DEPS com.google.guava:guava:33.4.8-jre
//DEPS org.jabref:afterburner.fx:2.0.0
//DEPS net.harawata:appdirs:1.4.0
//DEPS de.undercouch:citeproc-java:3.3.0

/// This class is required for [jbang](https://www.jbang.dev/)
public class JabSrvLauncher {
    public static void main(String[] args) throws Exception {
        org.jabref.http.server.cli.ServerCli.main(args);
    }
}
