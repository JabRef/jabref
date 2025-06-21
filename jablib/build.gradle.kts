import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import dev.jbang.gradle.tasks.JBangTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.net.URI
import java.util.*

plugins {
    id("org.jabref.gradle.module")
    id("java-library")

    id("antlr")

    id("me.champeau.jmh") version "0.7.3"

    id("com.vanniktech.maven.publish") version "0.32.0"

    // id("dev.jbang") version "0.2.0"
    // Workaround for https://github.com/jbangdev/jbang-gradle-plugin/issues/7
    id("com.github.koppor.jbang-gradle-plugin") version "fix-7-SNAPSHOT"
}

var version: String = project.findProperty("projVersion")?.toString() ?: "0.1.0"
if (project.findProperty("tagbuild")?.toString() != "true") {
    version += "-SNAPSHOT"
}

configurations.antlr {
    extendsFrom(configurations.internal.get())
}

configurations {
    // Treat the ANTLR compiler as a separate tool that should not end up on the compile/runtime
    // classpath of our runtime.
    // https://github.com/gradle/gradle/issues/820
    api { setExtendsFrom(extendsFrom.filterNot { it == antlr.get() }) }
    // Get ANTLR version from 'hiero-dependency-versions'
    antlr { extendsFrom(configurations["internal"]) }
}
tasks.withType<com.autonomousapps.tasks.CodeSourceExploderTask>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

dependencies {
    implementation("org.openjfx:javafx-base")

    // Required by afterburner.fx
    implementation("org.openjfx:javafx-controls")
    implementation("org.openjfx:javafx-fxml")
    implementation("org.openjfx:javafx-graphics")
    implementation("com.ibm.icu:icu4j")

    // Fix "error: module not found: javafx.controls" during compilation
    // implementation("org.openjfx:javafx-controls:$javafxVersion")

    // We do not use [Version Catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html#sec:dependency-bundles), because
    // exclusions are not supported

    implementation("org.jabref:afterburner.fx")
    implementation("org.jabref:easybind")

    implementation ("org.apache.pdfbox:pdfbox")
    implementation ("org.apache.pdfbox:fontbox")
    implementation ("org.apache.pdfbox:xmpbox")

    implementation("org.apache.lucene:lucene-core")
    implementation("org.apache.lucene:lucene-queryparser")
    implementation("org.apache.lucene:lucene-queries")
    implementation("org.apache.lucene:lucene-analysis-common")
    implementation("org.apache.lucene:lucene-highlighter")

    implementation("org.apache.commons:commons-csv")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.commons:commons-text")
    implementation("commons-logging:commons-logging")

    implementation("com.h2database:h2-mvstore")

    // required for reading write-protected PDFs - see https://github.com/JabRef/jabref/pull/942#issuecomment-209252635
    implementation("org.bouncycastle:bcprov-jdk18on")

    // region: LibreOffice
    implementation("org.libreoffice:unoloader")
    implementation("org.libreoffice:libreoffice")
    // Required for ID generation
    implementation("io.github.thibaultmeyer:cuid")
    // endregion

    implementation("io.github.java-diff-utils:java-diff-utils")
    implementation("info.debatty:java-string-similarity")

    implementation("com.github.javakeyring:java-keyring")

    implementation("org.eclipse.jgit:org.eclipse.jgit")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("com.fasterxml:aalto-xml")

    implementation("org.postgresql:postgresql")

    antlr("org.antlr:antlr4")
    implementation("org.antlr:antlr4-runtime")

    implementation("com.google.guava:guava")

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.inject:jakarta.inject-api")

    // region HTTP clients
    implementation("org.jsoup:jsoup")
    implementation("com.konghq:unirest-java-core")
    implementation("com.konghq:unirest-modules-gson")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api")
    // endregion

    implementation("org.slf4j:slf4j-api")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog in the CLI and GUI)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")

    // required by org.jabref.generators (only)
    implementation("org.tinylog:slf4j-tinylog")
    implementation("org.tinylog:tinylog-api")
    implementation("org.tinylog:tinylog-impl")

    implementation("de.undercouch:citeproc-java")

    implementation("com.vladsch.flexmark:flexmark")
    implementation("com.vladsch.flexmark:flexmark-html2md-converter")

    implementation("net.harawata:appdirs")

    implementation("org.jooq:jool")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify")

    // parse plist files
    implementation("com.googlecode.plist:dd-plist")

    // Parse lnk files
    implementation("com.github.vatbub:mslinks")

    // YAML reading and writing
    implementation("org.yaml:snakeyaml")

    // region AI
    implementation("dev.langchain4j:langchain4j")
    // Even though we use jvm-openai for LLM connection, we still need this package for tokenization.
    implementation("dev.langchain4j:langchain4j-open-ai")
    implementation("dev.langchain4j:langchain4j-mistral-ai")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini")
    implementation("dev.langchain4j:langchain4j-hugging-face")

    implementation("org.apache.velocity:velocity-engine-core")
    implementation("ai.djl:api")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.pytorch:pytorch-model-zoo")
    implementation("io.github.stefanbratanov:jvm-openai")
    // openai depends on okhttp, which needs kotlin - see https://github.com/square/okhttp/issues/5299 for details
    implementation("com.squareup.okhttp3:okhttp")
    // GemxFX also (transitively) depends on kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // endregion

    implementation("commons-io:commons-io")

    implementation("com.github.tomtung:latex2unicode_2.13")

    implementation("de.rototor.snuggletex:snuggletex-jeuclid")

    // Even if("compileOnly") is used, IntelliJ always adds to module-info.java. To avoid issues during committing, we use("implementation") instead of("compileOnly")
    implementation("io.github.adr:e-adr")

    implementation("io.zonky.test:embedded-postgres")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8")
    implementation("io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8")

    testImplementation(project(":test-support"))

    // loading of .fxml files in localization tests requires JabRef's GUI classes
    testImplementation(project(":jabgui"))

    testImplementation("io.github.classgraph:classgraph")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")

    testImplementation("org.mockito:mockito-core")
    testImplementation("net.bytebuddy:byte-buddy")

    testImplementation("org.xmlunit:xmlunit-core")
    testImplementation("org.xmlunit:xmlunit-matchers")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine")
    testImplementation("com.tngtech.archunit:archunit-junit5-api")

    testImplementation("org.hamcrest:hamcrest-library")

    testImplementation("org.wiremock:wiremock")

    // Required for LocalizationConsistencyTest
    testImplementation("org.testfx:testfx-core")
    testImplementation("org.testfx:testfx-junit5")
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


val abbrvJabRefOrgDir = layout.projectDirectory.dir("src/main/abbrv.jabref.org")
val generatedJournalFile = layout.buildDirectory.file("generated/resources/journals/journal-list.mv")

var taskGenerateJournalListMV = tasks.register<JBangTask>("generateJournalListMV") {
    group = "JabRef"
    description = "Converts the comma-separated journal abbreviation file to a H2 MVStore"
    dependsOn(tasks.named("generateGrammarSource"))

    script = rootProject.layout.projectDirectory.file("build-support/src/main/java/JournalListMvGenerator.java").asFile.absolutePath

    inputs.dir(abbrvJabRefOrgDir)
    outputs.file(generatedJournalFile)
    onlyIf {!generatedJournalFile.get().asFile.exists()}
}

var taskGenerateCitationStyleCatalog = tasks.register<JBangTask>("generateCitationStyleCatalog") {
    group = "JabRef"
    description = "Generates a catalog of all available citation styles"

    script = rootProject.layout.projectDirectory.file("build-support/src/main/java/CitationStyleCatalogGenerator.java").asFile.absolutePath

    inputs.dir(layout.projectDirectory.dir("src/main/resources/csl-styles"))
    val cslCatalogJson = layout.buildDirectory.file("generated/resources/citation-style-catalog.json")
    outputs.file(cslCatalogJson)
    onlyIf {!cslCatalogJson.get().asFile.exists()}
}

var ltwaCsvFile = layout.buildDirectory.file("tmp/ltwa_20210702.csv")

tasks.register("downloadLtwaFile") {
    group = "JabRef"
    description = "Downloads the LTWA file for journal abbreviations"

    val ltwaUrl = "https://www.issn.org/wp-content/uploads/2021/07/ltwa_20210702.csv"
    val ltwaDir = layout.buildDirectory.dir("resources/main/journals")

    outputs.file(ltwaCsvFile)

    // Ensure that the task really is not run if the file already exists (otherwise, the task could also run if gradle's cache is cleared, ...)
    onlyIf {!ltwaCsvFile.get().asFile.exists()}

    doLast {
        val dir = ltwaDir.get().asFile
        val file = ltwaCsvFile.get().asFile

        dir.mkdirs()

        URI(ltwaUrl).toURL().openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        logger.debug("Downloaded LTWA file to $file")
    }
}

var taskGenerateLtwaListMV = tasks.register<JBangTask>("generateLtwaListMV") {
    group = "JabRef"
    description = "Converts the LTWA CSV file to a H2 MVStore"
    dependsOn("downloadLtwaFile", tasks.named("generateGrammarSource"))

    script = rootProject.layout.projectDirectory.file("build-support/src/main/java/LtwaListMvGenerator.java").asFile.absolutePath

    inputs.file(ltwaCsvFile)
    val ltwaListMv = layout.buildDirectory.file("generated/resources/journals/ltwa-list.mv");
    outputs.file(ltwaListMv)
    onlyIf {!ltwaListMv.get().asFile.exists()}
}

// Adds ltwa, journal-list.mv, and citation-style-catalog.json to the resources directory
sourceSets["main"].resources {
    srcDir(layout.buildDirectory.dir("generated/resources"))
}


// region processResources
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
    dependsOn(extractMaintainers)
    dependsOn(taskGenerateJournalListMV)
    dependsOn(taskGenerateCitationStyleCatalog)
    dependsOn(taskGenerateLtwaListMV)
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
// endregion


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

        // We have generated/resources on the sources path, which needs to be populated
        taskGenerateJournalListMV,
        taskGenerateLtwaListMV,
        taskGenerateCitationStyleCatalog
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
    requires.add("wiremock")
    requires.add("wiremock.slf4j.spi.shim")

    // --add-reads
    //reads.add("org.jabref.jablib=io.github.classgraph")
    //reads.add("org.jabref.jablib=org.jabref.testsupport")
}
