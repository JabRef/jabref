import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import dev.jbang.gradle.tasks.JBangTask
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jabref.gradle.EmbeddedPostgresBinaries
import java.util.Calendar

plugins {
    id("org.jabref.gradle.module")
    id("java-library")

    id("antlr")

    id("me.champeau.jmh") version "0.7.3"

    id("com.vanniktech.maven.publish") version "0.37.0"

    id("dev.jbang") version "0.4.0"

    id("net.ltgt.errorprone") version "5.1.1"
    id("net.ltgt.nullaway") version "3.2.0"
}

val embeddedPostgresHostBinary = EmbeddedPostgresBinaries.forHost(
    providers.systemProperty("os.name").get(),
    providers.systemProperty("os.arch").get()
)

testModuleInfo {
    // loading of .fxml files in localization tests requires JabRef's GUI classes
    runtimeOnly("org.jabref")
    embeddedPostgresHostBinary?.let { runtimeOnly(it.moduleName) }

    requires("org.jabref.testsupport")

    requires("javafx.fxml")
    requires("javafx.graphics")

    requires("io.github.classgraph")

    requires("java.compiler")

    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.hamcrest")
    requires("org.mockito")

    // Required for LocalizationConsistencyTest
    requires("org.testfx.junit5")

    requires("org.xmlunit")
    requires("org.xmlunit.matchers")

	requires("com.fasterxml.jackson.databind")

    requires("com.tngtech.archunit")
    requires("com.tngtech.archunit.junit5.api")
    runtimeOnly("com.tngtech.archunit.junit5.engine")

    // Highly recommended builder generator - https://github.com/skinny85/jilt (used for tests only)
    requiresStatic("jilt")
    annotationProcessor("jilt")
}

dependencies {
    antlr("org.antlr:antlr4")

    errorprone("com.google.errorprone:error_prone_core")
    errorprone("com.uber.nullaway:nullaway")

    embeddedPostgresHostBinary?.let { testRuntimeOnly(javaModuleDependencies.ga(it.moduleName)) }
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

evaluationDependsOn(":versions")
val jbangVersion = project(":versions").extra["jbangVersion"] as String

tasks.withType<JBangTask>().configureEach {
    version = jbangVersion
    // The plugin defaults installDir to <user.home>/.gradle/caches/jbang, ignoring GRADLE_USER_HOME.
    // On the Windows CI runners GRADLE_USER_HOME is D:\a\.gradle, so JBang ended up outside the
    // cached Gradle user home and was downloaded from github.com in every run.
    installDir.set(gradle.gradleUserHomeDir.resolve("caches/jbang"))
}

val abbrvJabRefOrgDir = layout.projectDirectory.dir("src/main/abbrv.jabref.org")
val generatedJournalFile = layout.buildDirectory.file("generated/resources/journals/journal-list.mv")

// JBang compiles the files listed in `//SOURCES` into the script, so they must be task inputs
// as well - otherwise a change there leaves the generated output stale.
fun jbangSources(script: RegularFile): FileCollection {
    val scriptDir = script.asFile.parentFile
    val paths = script.asFile.readLines()
        .filter { it.startsWith("//SOURCES ") }
        .map { it.removePrefix("//SOURCES ").trim() }
    return files(paths.map { path ->
        if (path.contains('*')) {
            fileTree(scriptDir.resolve(path.substringBeforeLast('/'))) { include(path.substringAfterLast('/')) }
        } else {
            scriptDir.resolve(path)
        }
    })
}

var taskGenerateJournalListMV = tasks.register<JBangTask>("generateJournalListMV") {
    group = "JabRef"
    description = "Converts the comma-separated journal abbreviation file to a H2 MVStore"
    dependsOn(tasks.named("generateGrammarSource"))
    val generatorScript = rootProject.layout.projectDirectory.file("build-support/src/main/java/JournalListMvGenerator.java")
    script = '"' + generatorScript.asFile.absolutePath + '"'

    inputs.dir(abbrvJabRefOrgDir)
    inputs.file(generatorScript)
    inputs.files(jbangSources(generatorScript))
    outputs.file(generatedJournalFile)
}

var taskGenerateCitationStyleCatalog = tasks.register<JBangTask>("generateCitationStyleCatalog") {
    group = "JabRef"
    description = "Generates a catalog of all available citation styles"
    // The JBang gradle plugin doesn't handle parallization well - thus we enforce sequential execution
    mustRunAfter(taskGenerateJournalListMV)
    val generatorScript = rootProject.layout.projectDirectory.file("build-support/src/main/java/CitationStyleCatalogGenerator.java")
    script = '"' + generatorScript.asFile.absolutePath + '"'

    inputs.dir(layout.projectDirectory.dir("src/main/resources/csl-styles"))
    inputs.file(generatorScript)
    inputs.files(jbangSources(generatorScript))
    outputs.file(layout.buildDirectory.file("generated/resources/citation-style-catalog.json"))
}

var taskGenerateLtwaListMV = tasks.register<JBangTask>("generateLtwaListMV") {
    group = "JabRef"
    description = "Converts the LTWA CSV file to a H2 MVStore"
    dependsOn(tasks.named("generateGrammarSource"))
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

    // JabRef only reads the top-level styles (CitationStyleCatalogGenerator scans with depth 1),
    // but these ~8000 unused files dominate the cost of processResources on Windows.
    exclude("csl-styles/dependent/**")
    exclude("csl-styles/spec/**")
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

val extractMaintainers = tasks.register<JoinNonCommentedLines>("extractMaintainers") {
    inputFile = layout.projectDirectory.file("../MAINTAINERS")
    outputFile = layout.buildDirectory.file("maintainers.txt")
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
val scholarApiKey = providers.environmentVariable("ScholarApiKey").orElse("")
val springerNatureAPIKey = providers.environmentVariable("SpringerNatureAPIKey").orElse("")
val unpaywallEmail = providers.environmentVariable("UNPAYWALL_EMAIL").orElse("")
val wileyTdmApiKey = providers.environmentVariable("WileyTdmApiKey").orElse("")
val crossRefEmail = providers.environmentVariable("CROSSREF_EMAIL").orElse("")

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
    inputs.property("scholarApiKey", scholarApiKey)
    inputs.property("scopusApiKey", scopusApiKey)
    inputs.property("semanticScholarApiKey", semanticScholarApiKey)
    inputs.property("unpaywallEmail", unpaywallEmail)
    inputs.property("wileyTdmApiKey", wileyTdmApiKey)
    inputs.property("crossRefEmail", crossRefEmail)

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
                "scholarApiKey" to inputs.properties["scholarApiKey"],
                "springerNatureAPIKey" to inputs.properties["springerNatureAPIKey"],
                "unpaywallEmail" to inputs.properties["unpaywallEmail"],
                "wileyTdmApiKey" to inputs.properties["wileyTdmApiKey"],
                "crossRefEmail" to inputs.properties["crossRefEmail"],
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
        disableAllChecks = true
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
        excludeTags("DatabaseTest", "ExternalServicesTest")
    }
    jvmArgs = listOf(
        "-javaagent:${configurations.mockitoAgent.get().asPath}",
        "--add-opens", "java.base/jdk.internal.ref=org.apache.pdfbox.io",
        "--add-opens", "java.base/java.nio=org.apache.pdfbox.io",
        "--enable-native-access=com.sun.jna,javafx.graphics,org.apache.lucene.core"
    )
    testLogging {
        showStandardStreams = false
    }
}

jmh {
    warmupIterations = 5
    iterations = 10
    fork = 2
    zip64  = true
}

val testSourceSet = sourceSets.test.get()

tasks.register<Test>("externalServicesTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform {
        includeTags("ExternalServicesTest")
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
externalServicesTest.mustRunAfter jacocoPrepare

jacocoTestReport {
    dependsOn jacocoPrepare, test, externalServicesTest, databaseTest

    executionData files(
            layout.buildDirectory.file('jacoco/test.exec').get().asFile,
            layout.buildDirectory.file('jacoco/externalServicesTest.exec').get().asFile,
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
    sourcesJar = SourcesJar.Sources(),
  ))

  publishToMavenCentral()
  signAllPublications()

  coordinates("org.jabref", "jablib", version)

  pom {
    name = "jablib"
    description = "JabRef's Java library to work with BibTeX"
    inceptionYear = "2025"
    url = "https://github.com/JabRef/jabref/"
    licenses {
      license {
        name = "MIT"
        url = "https://github.com/JabRef/jabref/blob/main/LICENSE"
      }
    }
    developers {
      developer {
        id = "jabref"
        name = "JabRef Developers"
        url = "https://github.com/JabRef/"
      }
    }
    scm {
        url = "https://github.com/JabRef/jabref"
        connection = "scm:git:https://github.com/JabRef/jabref"
        developerConnection = "scm:git:git@github.com:JabRef/jabref.git"
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
