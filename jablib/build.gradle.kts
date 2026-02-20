import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import dev.jbang.gradle.tasks.JBangTask
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.*

plugins {
    id("org.jabref.gradle.module")
    id("java-library")

    id("antlr")

    id("me.champeau.jmh") version "0.7.3"

    id("com.vanniktech.maven.publish") version "0.36.0"

    id("dev.jbang") version "0.4.0"

    id("net.ltgt.errorprone") version "5.0.0"
    id("net.ltgt.nullaway") version "3.0.0"
}

testModuleInfo {
    // loading of .fxml files in localization tests requires JabRef's GUI classes
    runtimeOnly("org.jabref")

    requires("org.jabref.testsupport")

    requires("io.github.classgraph")

    requires("jtokkit")
    requires("java.compiler")

    requires("org.libreoffice.unoloader")

    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.hamcrest")
    requires("org.mockito")

    // Required for LocalizationConsistencyTest
    requires("org.testfx.junit5")

    requires("org.xmlunit")
    requires("org.xmlunit.matchers")

    requires("com.tngtech.archunit")
    requires("com.tngtech.archunit.junit5.api")
    runtimeOnly("com.tngtech.archunit.junit5.engine")

    // Highly recommended builder generator - https://github.com/skinny85/jilt (used for tests only)
    requiresStatic("jilt")
    annotationProcessor("jilt")
}

// See https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
val mockitoAgent = configurations.create("mockitoAgent") {
    extendsFrom(configurations["internal"])
}
dependencies {
    antlr("org.antlr:antlr4")

    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }

    errorprone("com.google.errorprone:error_prone_core")
    errorprone("com.uber.nullaway:nullaway")
}

var version = providers.gradleProperty("projVersion")
    .orElse(providers.environmentVariable("VERSION"))
    .orElse("0.1.0")
    .get()

if (project.findProperty("tagbuild")?.toString() != "true") {
    version += "-SNAPSHOT"
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

    script = '"' + rootProject.layout.projectDirectory.file("build-support/src/main/java/JournalListMvGenerator.java").asFile.absolutePath + '"'

    inputs.dir(abbrvJabRefOrgDir)
    outputs.file(generatedJournalFile)
    val generatedJournalFileProv = generatedJournalFile
    onlyIf { !generatedJournalFileProv.get().asFile.exists() }
}

var taskGenerateCitationStyleCatalog = tasks.register<JBangTask>("generateCitationStyleCatalog") {
    group = "JabRef"
    description = "Generates a catalog of all available citation styles"
    // The JBang gradle plugin doesn't handle parallization well - thus we enforce sequential execution
    mustRunAfter(taskGenerateJournalListMV)

    script = '"' + rootProject.layout.projectDirectory.file("build-support/src/main/java/CitationStyleCatalogGenerator.java").asFile.absolutePath + '"'

    inputs.dir(layout.projectDirectory.dir("src/main/resources/csl-styles"))
    val cslCatalogJson = layout.buildDirectory.file("generated/resources/citation-style-catalog.json")
    outputs.file(cslCatalogJson)
    val cslCatalogJsonProv = cslCatalogJson
    onlyIf { !cslCatalogJsonProv.get().asFile.exists() }
}

var taskGenerateLtwaListMV = tasks.register<JBangTask>("generateLtwaListMV") {
    group = "JabRef"
    description = "Converts the LTWA CSV file to a H2 MVStore"
    // The JBang gradle plugin doesn't handle parallization well - thus we enforce sequential execution
    mustRunAfter(taskGenerateCitationStyleCatalog)

    script = '"' + rootProject.layout.projectDirectory.file("build-support/src/main/java/LtwaListMvGenerator.java").asFile.absolutePath + '"'

    inputs.file(layout.buildDirectory.file("../src/main/resources/ltwa/ltwa_20210702.csv"))
    val ltwaListMv = layout.buildDirectory.file("generated/resources/journals/ltwa-list.mv")
    outputs.file(ltwaListMv)
    val ltwaListMvProv = ltwaListMv
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

val versionProvider = providers.gradleProperty("projVersionInfo")
    .orElse(providers.environmentVariable("VERSION_INFO"))
    .orElse("100.0.0")

val year = Calendar.getInstance().get(Calendar.YEAR).toString()

val azureInstrumentationKey = providers.environmentVariable("AzureInstrumentationKey").orElse("")
val astrophysicsDataSystemAPIKey = providers.environmentVariable("AstrophysicsDataSystemAPIKey").orElse("")
val biodiversityHeritageApiKey = providers.environmentVariable("BiodiversityHeritageApiKey").orElse("")
val ieeeAPIKey = providers.environmentVariable("IEEEAPIKey").orElse("")
val medlineApiKey = providers.environmentVariable("MedlineApiKey").orElse("")
val openAlexApiKey = providers.environmentVariable("OpenAlexApiKey").orElse("")
val scopusApiKey = providers.environmentVariable("ScopusApiKey").orElse("")
val semanticScholarApiKey = providers.environmentVariable("SemanticScholarApiKey").orElse("")
val springerNatureAPIKey = providers.environmentVariable("SpringerNatureAPIKey").orElse("")
val unpaywallEmail = providers.environmentVariable("UNPAYWALL_EMAIL").orElse("")

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

    inputs.property("astrophysicsDataSystemAPIKey", astrophysicsDataSystemAPIKey)
    inputs.property("biodiversityHeritageApiKey", biodiversityHeritageApiKey)
    inputs.property("ieeeAPIKey", ieeeAPIKey)
    inputs.property("medlineApiKey", medlineApiKey)
    inputs.property("openAlexApiKey", openAlexApiKey)
    inputs.property("springerNatureAPIKey", springerNatureAPIKey)
    inputs.property("scopusApiKey", scopusApiKey)
    inputs.property("semanticScholarApiKey", semanticScholarApiKey)
    inputs.property("unpaywallEmail", unpaywallEmail)

    filesMatching("build.properties") {
        expand(
            mapOf(
                "version" to inputs.properties["version"],
                "year" to inputs.properties["year"],
                "maintainers" to inputs.properties["maintainers"],
                "azureInstrumentationKey" to inputs.properties["azureInstrumentationKey"],

                "astrophysicsDataSystemAPIKey" to inputs.properties["astrophysicsDataSystemAPIKey"],
                "biodiversityHeritageApiKey" to inputs.properties["biodiversityHeritageApiKey"],
                "ieeeAPIKey" to inputs.properties["ieeeAPIKey"],
                "medlineApiKey" to inputs.properties["medlineApiKey"],
                "openAlexApiKey" to inputs.properties["openAlexApiKey"],
                "scopusApiKey" to inputs.properties["scopusApiKey"],
                "semanticScholarApiKey" to inputs.properties["semanticScholarApiKey"],
                "springerNatureAPIKey" to inputs.properties["springerNatureAPIKey"],
                "unpaywallEmail" to inputs.properties["unpaywallEmail"],
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

    options.errorprone {
        disableAllChecks.set(true)
        enable("NullAway")
    }

    options.errorprone.nullaway {
        warn()
        annotatedPackages.add("org.jabref")
    }
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
    jvmArgs = listOf(
        "-javaagent:${mockitoAgent.asPath}",
        "--add-opens", "java.base/jdk.internal.ref=org.apache.pdfbox.io",
        "--add-opens", "java.base/java.nio=org.apache.pdfbox.io",
        "--enable-native-access=com.sun.jna,javafx.graphics,org.apache.lucene.core"
    )
}

jmh {
    warmupIterations = 5
    iterations = 10
    fork = 2
    zip64  = true
}

val testSourceSet = sourceSets["test"]

tasks.register<Test>("fetcherTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform {
        includeTags("FetcherTest")
    }
    maxParallelForks = 1
}

tasks.register<Test>("databaseTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
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

  publishToMavenCentral()
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


// Include the BOM in the generated POM ("inline" / "inlining")
// Source: https://github.com/gradle/gradle/issues/10861#issuecomment-3027387345
publishing.publications.withType<MavenPublication>().configureEach {
    versionMapping {
        allVariants { fromResolutionResult() }
    }
}
