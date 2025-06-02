import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.*

plugins {
    id("buildlogic.java-common-conventions")

    `java-library`

    id("idea")

    id("antlr")
    id("com.github.bjornvester.xjc") version "1.8.1"

    id("me.champeau.jmh") version "0.7.3"

    id("com.vanniktech.maven.publish") version "0.32.0"
}

val pdfbox = "3.0.5"
val luceneVersion = "10.2.1"

var version: String = project.findProperty("projVersion")?.toString() ?: "0.1.0"
if (project.findProperty("tagbuild")?.toString() != "true") {
    version += "-SNAPSHOT"
}

val javafxVersion = "24.0.1"

dependencies {
    implementation(fileTree(mapOf("dir" to("lib"), "includes" to listOf("*.jar"))))

    implementation("org.openjfx:javafx-base:$javafxVersion")

    // Required by afterburner.fx
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")
    implementation("org.openjfx:javafx-graphics:$javafxVersion")

    // Fix "error: module not found: javafx.controls" during compilation
    // implementation("org.openjfx:javafx-controls:$javafxVersion")

    // We do not use [Version Catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html#sec:dependency-bundles), because
    // exclusions are not supported

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }
    implementation("org.jabref:easybind:2.2.1-SNAPSHOT") {
        exclude(group = "org.openjfx")
    }

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

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation("info.debatty:java-string-similarity:2.0.0")

    implementation("com.github.javakeyring:java-keyring:1.0.4")

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.1.202505142326-r")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")

    // required by XJC
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")

    implementation("com.fasterxml:aalto-xml:1.3.3")

    implementation("org.postgresql:postgresql:42.7.5")

    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // region HTTP clients
    implementation("org.jsoup:jsoup:1.20.1")
    implementation("com.konghq:unirest-java-core:4.4.7")
    implementation("com.konghq:unirest-modules-gson:4.4.7")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.4")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")
    // endregion

    implementation("org.slf4j:slf4j-api:2.0.17")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog in the CLI and GUI)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    // required by org.jabref.generators (only)
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")

    implementation("de.undercouch:citeproc-java:3.3.0") {
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

    // XJC related
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.5")

    // region AI
    implementation("dev.langchain4j:langchain4j:1.0.0")
    // Even though we use jvm-openai for LLM connection, we still need this package for tokenization.
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("dev.langchain4j:langchain4j-mistral-ai:1.0.0-beta5") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:1.0.1-beta6") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
    }
    implementation("dev.langchain4j:langchain4j-hugging-face:1.0.0-beta5") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.retrofit2", module = "retrofit")
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation("org.apache.velocity:velocity-engine-core:2.4.1")
    implementation(platform("ai.djl:bom:0.33.0"))
    implementation("ai.djl:api")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.pytorch:pytorch-model-zoo")
    implementation("io.github.stefanbratanov:jvm-openai:0.11.0")
    // openai depends on okhttp, which needs kotlin - see https://github.com/square/okhttp/issues/5299 for details
    implementation("com.squareup.okhttp3:okhttp:4.12.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    // GemxFX also (transitively) depends on kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.21")
    // endregion

    implementation("commons-io:commons-io:2.19.0")

    implementation("com.github.tomtung:latex2unicode_2.13:0.3.2") {
        exclude(module = "fastparse_2.13")
    }

    implementation ("de.rototor.snuggletex:snuggletex-jeuclid:1.3.0") {
        exclude(group = "org.apache.xmlgraphics")
    }

    // Even if("compileOnly") is used, IntelliJ always adds to module-info.java. To avoid issues during committing, we use("implementation") instead of("compileOnly")
    implementation("io.github.adr:e-adr:2.0.0-SNAPSHOT")

    implementation("io.zonky.test:embedded-postgres:2.1.0")
    implementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:17.4.0"))
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8")

    testImplementation(project(":test-support"))

    // loading of .fxml files in localization tests requires JabRef's GUI classes
    testImplementation(project(":jabgui"))

    testImplementation("io.github.classgraph:classgraph:4.8.179")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.12.2")

    testImplementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")

    testImplementation("org.xmlunit:xmlunit-core:2.10.1")
    testImplementation("org.xmlunit:xmlunit-matchers:2.10.2")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:1.4.1")
    testImplementation("com.tngtech.archunit:archunit-junit5-api:1.4.1")

    testImplementation("org.hamcrest:hamcrest-library:3.0")

    // recommended by https://github.com/wiremock/wiremock/issues/2149#issuecomment-1835775954
    testImplementation("org.wiremock:wiremock-standalone:3.12.1")

    // Required for LocalizationConsistencyTest
    testImplementation("org.testfx:testfx-core:4.0.16-alpha")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
}
/*
jacoco {
    toolVersion = "0.8.13"
}
 */

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages")
}

xjc {
    xsdDir.set(layout.projectDirectory.dir("src/main/xsd"))
    xjcVersion.set("4.0.5")
    defaultPackage.set("org.jabref.logic.importer.fileformat.citavi")
    options.set(listOf("encoding=UTF-8"))
}

abstract class JoinNonCommentedLines : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun extract() {
        val input = inputFile.get().asFile
        val result = input.readLines()
            .filterNot { it.trim().startsWith("#") }
            .joinToString(", ")

        outputFile.get().asFile.writeText(result)
    }
}

val extractMaintainers by tasks.registering(JoinNonCommentedLines::class) {
    inputFile.set(layout.projectDirectory.file("../MAINTAINERS"))
    outputFile.set(layout.buildDirectory.file("maintainers.txt"))
}

val maintainersProvider: Provider<String> = extractMaintainers.flatMap {
    it.outputFile.map { file -> file.asFile.readText() }
}

val versionProvider = providers.gradleProperty("projVersionInfo").orElse("100.0.0")

val year = Calendar.getInstance().get(Calendar.YEAR).toString()

val azureInstrumentationKey = providers.environmentVariable("AzureInstrumentationKey").orElse("")
val springerNatureAPIKey = providers.environmentVariable("SpringerNatureAPIKey").orElse("")
val astrophysicsDataSystemAPIKey = providers.environmentVariable("AstrophysicsDataSystemAPIKey").orElse("")
val ieeeAPIKey = providers.environmentVariable("IEEEAPIKey").orElse("")
val scienceDirectApiKey = providers.environmentVariable("SCIENCEDIRECTAPIKEY").orElse("")
val biodiversityHeritageApiKey = providers.environmentVariable("BiodiversityHeritageApiKey").orElse("")
val semanticScholarApiKey = providers.environmentVariable("SemanticScholarApiKey").orElse("")

tasks.named<ProcessResources>("processResources") {
    dependsOn("extractMaintainers")
    filteringCharset = "UTF-8"

    inputs.property("version", versionProvider)
    inputs.property("year", year)
    inputs.property("maintainers", maintainersProvider)
    inputs.property("azureInstrumentationKey", azureInstrumentationKey)
    inputs.property("springerNatureAPIKey", springerNatureAPIKey)
    inputs.property("astrophysicsDataSystemAPIKey", astrophysicsDataSystemAPIKey)
    inputs.property("ieeeAPIKey", ieeeAPIKey)
    inputs.property("scienceDirectApiKey", scienceDirectApiKey)
    inputs.property("biodiversityHeritageApiKey", biodiversityHeritageApiKey)
    inputs.property("semanticScholarApiKey", semanticScholarApiKey)

    filesMatching("build.properties") {
        expand(
            mapOf(
                "version" to inputs.properties["version"],
                "year" to inputs.properties["year"],
                "maintainers" to inputs.properties["maintainers"],
                "azureInstrumentationKey" to inputs.properties["azureInstrumentationKey"],
                "springerNatureAPIKey" to inputs.properties["springerNatureAPIKey"],
                "astrophysicsDataSystemAPIKey" to inputs.properties["astrophysicsDataSystemAPIKey"],
                "ieeeAPIKey" to inputs.properties["ieeeAPIKey"],
                "scienceDirectApiKey" to inputs.properties["scienceDirectApiKey"],
                "biodiversityHeritageApiKey" to inputs.properties["biodiversityHeritageApiKey"],
                "semanticScholarApiKey" to inputs.properties["semanticScholarApiKey"]
            )
        )
    }

    filesMatching(
        listOf(
            "resources/resource/ods/meta.xml",
            "resources/resource/openoffice/meta.xml"
        )
    ) {
        expand(mapOf("version" to inputs.properties["version"]))
    }
}

tasks.register<JavaExec>("generateJournalListMV") {
    group = "JabRef"
    description = "Converts the comma-separated journal abbreviation file to a H2 MVStore"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.jabref.generators.JournalListMvGenerator")

    javaLauncher.convention(javaToolchains.launcherFor {
        languageVersion.set(java.toolchain.languageVersion)
    })

    val outputFile = layout.buildDirectory.file("resources/main/journals/journal-list.mv")
    onlyIf {
        !outputFile.get().asFile.exists()
    }
}

tasks.named("jar") {
    dependsOn("generateJournalListMV")
}

tasks.named("compileTestJava") {
    dependsOn("generateJournalListMV")
}

tasks.register<JavaExec>("generateCitationStyleCatalog") {
    group = "JabRef"
    description = "Generates a catalog of all available citation styles"
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("processResources")
    mainClass.set("org.jabref.generators.CitationStyleCatalogGenerator")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(java.toolchain.languageVersion) })
//    onlyIf {
//        !file("build/resources/main/journals/journal-list.mv").exists()
//    }
}

tasks.named("jar") {
    dependsOn("generateCitationStyleCatalog")
}

tasks.named("compileTestJava") {
    dependsOn("generateCitationStyleCatalog")
}

tasks.register("downloadLtwaFile") {
    group = "JabRef"
    description = "Downloads the LTWA file for journal abbreviations"

    val ltwaUrl = "https://www.issn.org/wp-content/uploads/2021/07/ltwa_20210702.csv"
    val ltwaDir = layout.buildDirectory.dir("resources/main/journals")
    val ltwaCsvFile = ltwaDir.map { it.file("ltwa_20210702.csv") }

    onlyIf {
        !ltwaCsvFile.get().asFile.exists()
    }

    doLast {
        val dir = ltwaDir.get().asFile
        val file = ltwaCsvFile.get().asFile

        if (!file.exists()) {
            dir.mkdirs()
            ant.withGroovyBuilder {
                "get"(
                    mapOf("src" to ltwaUrl, "dest" to file, "verbose" to true)
                )
            }
            logger.lifecycle("Downloaded LTWA file to $file")
        } else {
            logger.lifecycle("LTWA file already exists at $file")
        }
    }
}

tasks.register<JavaExec>("generateLtwaListMV") {
    group = "JabRef"
    description = "Converts the LTWA CSV file to a H2 MVStore"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.jabref.generators.LtwaListMvGenerator")

    javaLauncher.convention(javaToolchains.launcherFor {
        languageVersion.set(java.toolchain.languageVersion)
    })

    dependsOn("downloadLtwaFile")

    val outputFile = layout.buildDirectory.file("resources/main/journals/ltwa-list.mv")
    onlyIf {
        !outputFile.get().asFile.exists()
    }
}

tasks.named("jar") {
    dependsOn("generateLtwaListMV")
}

tasks.named("compileTestJava") {
    dependsOn("generateLtwaListMV")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    // Hint from https://docs.gradle.org/current/userguide/performance.html#run_the_compiler_as_a_separate_process
    options.isFork = true
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        // version = false
        // author = false
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("DatabaseTest", "FetcherTest")
    }
}

jmh {
    warmupIterations = 5
    iterations = 10
    fork = 2
    zip64  = true
}

tasks.register<Test>("fetcherTest") {
    useJUnitPlatform {
        includeTags("FetcherTest")
    }

    maxParallelForks = 1
}

tasks.register<Test>("databaseTest") {
    useJUnitPlatform {
        includeTags("DatabaseTest")
    }

    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }

    maxParallelForks = 1
}

/*
tasks.register('jacocoPrepare') {
    doFirst {
        // Ignore failures of tests
        tasks.withType(Test).tap {
            configureEach {
                ignoreFailures = true
            }
        }
    }
}
test.mustRunAfter jacocoPrepare
databaseTest.mustRunAfter jacocoPrepare
fetcherTest.mustRunAfter jacocoPrepare

jacocoTestReport {
    dependsOn jacocoPrepare, test, fetcherTest, databaseTest

    executionData files(
            layout.buildDirectory.file('jacoco/test.exec').get().asFile,
            layout.buildDirectory.file('jacoco/fetcherTest.exec').get().asFile,
            layout.buildDirectory.file('jacoco/databaseTest.exec').get().asFile)

    reports {
        csv.required = true
        html.required = true
        // coveralls plugin depends on xml format report
        xml.required = true
    }
}
*/

mavenPublishing {
  configure(JavaLibrary(
    // configures the -javadoc artifact, possible values:
    // - `JavadocJar.None()` don't publish this artifact
    // - `JavadocJar.Empty()` publish an emprt jar
    // - `JavadocJar.Javadoc()` to publish standard javadocs
    javadocJar = JavadocJar.Javadoc(),
    // whether to publish a sources jar
    sourcesJar = true,
  ))

  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

  signAllPublications()

  coordinates("org.jabref", "jablib", version)

  pom {
    name.set("jablib")
    description.set("JabRef's Java library to work with BibTeX")
    inceptionYear.set("2025")
    url.set("https://github.com/JabRef/jabref/")
    licenses {
      license {
        name.set("MIT")
        url.set("https://github.com/JabRef/jabref/blob/main/LICENSE")
      }
    }
    developers {
      developer {
        id.set("jabref")
        name.set("JabRef Developers")
        url.set("https://github.com/JabRef/")
      }
    }
    scm {
        url.set("https://github.com/JabRef/jabref")
        connection.set("scm:git:https://github.com/JabRef/jabref")
        developerConnection.set("scm:git:git@github.com:JabRef/jabref.git")
    }
  }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(
        tasks.named("generateGrammarSource"),
    )
}

tasks.withType<GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
}

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("io.github.classgraph")
    requires.add("org.junit.jupiter.api")
    requires.add("org.junit.jupiter.params")
    requires.add("org.jabref.testsupport")
    requires.add("org.mockito")

    // --add-reads
    //reads.add("org.jabref.jablib=io.github.classgraph")
    //reads.add("org.jabref.jablib=org.jabref.testsupport")
}
