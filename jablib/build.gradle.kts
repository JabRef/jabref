plugins {
    id("buildlogic.java-common-conventions")

    `java-library`

    id("idea")

    id("antlr")
    id("com.github.edeandrea.xjc-generation") version "1.6"

    id("org.openjfx.javafxplugin") version("0.1.0")

    // This is https://github.com/java9-modularity/gradle-modules-plugin/pull/282
    id("com.github.koppor.gradle-modules-plugin") version "v1.8.15-cmd-1"

    id("com.github.andygoossens.modernizer") version "1.10.0"
    id("org.openrewrite.rewrite") version "7.3.0"

    // nicer test outputs during running and completion
    // Homepage: https://github.com/radarsh/gradle-test-logger-plugin
    id("com.adarshr.test-logger") version "4.0.0"

    id("org.itsallcode.openfasttrace") version "3.0.1"

    id("me.champeau.jmh") version "0.7.3"
}

val pdfbox = "3.0.4"
val luceneVersion = "10.2.0"
val jaxbVersion by extra { "4.0.3" }

dependencies {
    implementation(fileTree(mapOf("dir" to("lib"), "includes" to listOf("*.jar"))))

    // We do not use [Version Catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html#sec:dependency-bundles), because
    // exclusions are not supported

    implementation ("org.apache.pdfbox:pdfbox:$pdfbox") {
        exclude(group = "commons-logging")
    }
    implementation ("org.apache.pdfbox:fontbox:$pdfbox") {
        exclude(group = "commons-logging")
    }
    implementation ("org.apache.pdfbox:xmpbox:$pdfbox") {
        exclude(group = "org.junit.jupiter")
        exclude(group = "commons-logging")
    }

    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-queries:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-highlighter:$luceneVersion")

    implementation("org.apache.commons:commons-csv:1.14.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("commons-logging:commons-logging:1.3.5")
    implementation("com.h2database:h2-mvstore:2.3.232")

    // required for reading write-protected PDFs - see https://github.com/JabRef/jabref/pull/942#issuecomment-209252635
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")

    // region: LibreOffice
    implementation("org.libreoffice:unoloader:24.8.4")
    implementation("org.libreoffice:libreoffice:24.8.4")
    // Required for ID generation
    implementation("io.github.thibaultmeyer:cuid:2.0.3")
    // endregion

    // injection framework
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.10")
    implementation("org.glassfish.hk2:hk2-api:3.1.1")

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation("info.debatty:java-string-similarity:2.0.0")

    implementation("com.github.javakeyring:java-keyring:1.0.4")

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.0.202503040940-r")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    // required by XJC
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")

    implementation("com.fasterxml:aalto-xml:1.3.3")

    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.12")

    implementation("org.postgresql:postgresql:42.7.5")

    // Support unix socket connection types
    implementation("com.kohlschutter.junixsocket:junixsocket-core:2.10.1")
    implementation("com.kohlschutter.junixsocket:junixsocket-mysql:2.10.1")

    implementation("com.oracle.ojdbc:ojdbc10:19.3.0.0") {
        // causing module issues
        exclude(module = "oraclepki")
    }

    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("org.jabref:easybind:2.2.1-SNAPSHOT") {
        exclude(group = "org.openjfx")
    }

    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // region HTTP clients
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("com.konghq:unirest-java-core:4.4.5")
    implementation("com.konghq:unirest-modules-gson:4.4.5")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.3")
    // endregion

    implementation("org.slf4j:slf4j-api:2.0.17")

    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog in the CLI and GUI)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("de.undercouch:citeproc-java:3.2.0") {
        exclude(group = "org.antlr")
    }

    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-html2md-converter:0.64.8")

    implementation("net.harawata:appdirs:1.4.0")

    implementation("org.jooq:jool:0.9.15")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

    // parse plist files
    implementation("com.googlecode.plist:dd-plist:1.28")

    // Parse lnk files
    implementation("com.github.vatbub:mslinks:1.0.6.2")

    // YAML reading and writing
    implementation("org.yaml:snakeyaml:2.4")

    // region AI
    implementation("dev.langchain4j:langchain4j:0.36.2")
    // Even though we use jvm-openai for LLM connection, we still need this package for tokenization.
    implementation("dev.langchain4j:langchain4j-open-ai:0.36.2") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("dev.langchain4j:langchain4j-mistral-ai:0.36.2") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:0.36.2") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
    }
    implementation("dev.langchain4j:langchain4j-hugging-face:0.36.2") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation("org.apache.velocity:velocity-engine-core:2.4.1")
    implementation(platform("ai.djl:bom:0.32.0"))
    implementation("ai.djl:api")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.pytorch:pytorch-model-zoo")
    implementation("io.github.stefanbratanov:jvm-openai:0.11.0")
    // openai depends on okhttp, which needs kotlin - see https://github.com/square/okhttp/issues/5299 for details
    implementation("com.squareup.okhttp3:okhttp:4.12.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    // GemxFX also (transitively) depends on kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")
    // endregion

    implementation("commons-io:commons-io:2.19.0")

    implementation("com.github.tomtung:latex2unicode_2.13:0.3.2") {
        exclude(module = "fastparse_2.13")
    }

    implementation("de.rototor.snuggletex:snuggletex:1.3.0")
    implementation ("de.rototor.snuggletex:snuggletex-jeuclid:1.3.0") {
        exclude(group = "org.apache.xmlgraphics")
    }

    // Even if("compileOnly") is used, IntelliJ always adds to module-info.java. To avoid issues during committing, we use("implementation") instead of("compileOnly")
    implementation("io.github.adr:e-adr:2.0.0-SNAPSHOT")

    implementation("io.zonky.test:embedded-postgres:2.1.0")
    implementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:17.4.0"))
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8")

    testImplementation("io.github.classgraph:classgraph:4.8.179")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.12.1")

    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.xmlunit:xmlunit-core:2.10.0")
    testImplementation("org.xmlunit:xmlunit-matchers:2.10.0")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:1.4.0")
    testImplementation("com.tngtech.archunit:archunit-junit5-api:1.4.0")

    checkstyle("com.puppycrawl.tools:checkstyle:10.23.0")
    configurations.named("checkstyle") {
        resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:0")
       }
    }

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.5.0"))
    rewrite("org.openrewrite.recipe:rewrite-static-analysis")
    rewrite("org.openrewrite.recipe:rewrite-logging-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")

    "xjc"("org.glassfish.jaxb:jaxb-xjc:$jaxbVersion")
    "xjc"("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
}

javafx {
    version = "24"
    modules = listOf(
        // properties
        "javafx.base",
        // javafx.scene.paint.Color;
        "javafx.graphics"
    )
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages")
}

xjcGeneration {
    // plugin: https://github.com/edeandrea/xjc-generation-gradle-plugin#xjc-generation-gradle-plugin
    // hint by https://stackoverflow.com/questions/62776832/how-to-generate-java-classes-from-xsd-using-java-11-and-gradle#comment130555840_62776832
    defaultAdditionalXjcOptions = mapOf("encoding" to "UTF-8")
    schemas {
        create("citavi") {
            schemaFile = "citavi/citavi.xsd"
            javaPackageName = "org.jabref.logic.importer.fileformat.citavi"
        }
    }
}
