import org.gradlex.javamodule.packaging.tasks.Jpackage
import org.jabref.gradle.jdkVendor

plugins {
    id("org.jabref.gradle.module")
    id("org.jabref.gradle.feature.shadowjar")
    id("org.jabref.gradle.feature.nativecompile")
    id("application")
}

group = "org.jabref.jabkit"
version = providers.gradleProperty("projVersion")
    .orElse(providers.environmentVariable("VERSION"))
    .orElse("100.0.0")
    .get()

mainModuleInfo {
    annotationProcessor("info.picocli.codegen")
}

testModuleInfo {
    requires("mockwebserver3")
    requires("okhttp3")
    requires("okio")
    requires("org.apache.pdfbox")
    requires("org.jabref.testsupport")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.mockito")
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "4g"
}

application {
    mainClass = "org.jabref.toolkit.JabKitLauncher"

    // Also passed to launcher by java-module-packaging plugin
    applicationDefaultJvmArgs = listOf(
        // JEP 158: Disable all java util logging
        "-Xlog:disable",

        "--enable-native-access=com.sun.jna,javafx.graphics,jkeychain,org.apache.lucene.core",

        // "-XX:+UseZGC", "-XX:+ZUncommit",
        // "-XX:+UseG1GC",
        "-XX:+UseSerialGC",
        // "-XX:+UseStringDeduplication",

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders"
    )
}

javaModulePackaging {
    applicationName = "jabkit"
    addModules.add("jdk.incubator.vector")

    // general jLinkOptions are set in org.jabref.gradle.base.targets.gradle.kts

    // All targets have to have "app-image" as sole target, since we do not distribute an installer
    targetsWithOs("windows") {
        appImageOptions.addAll("--win-console")
        packageTypes = listOf("app-image")
    }
    targetsWithOs("linux") {
        options.addAll(
            "--icon", "$projectDir/../jabgui/src/main/resources/icons/JabRef-linux-icon-64.png",
        )
        packageTypes = listOf("app-image")
    }
    targetsWithOs("macos") {
        packageTypes = listOf("app-image")
    }
}

// AOT cache (JEP 514): after jpackage has assembled the app image, do a training run of the packaged
// launcher (-XX:AOTCacheOutput records and assembles the cache in one go) and ship the cache inside
// the image. -XX:AOTCache and -XX:AOTCacheOutput are mutually exclusive, so the -XX:AOTCache line is
// appended to the launcher .cfg only after training. An unusable cache is non-fatal at runtime
// (default -XX:AOTMode=auto) and its diagnostics are silenced by -Xlog:disable above.
// OpenJ9 does not implement the HotSpot AOT cache (it has -Xshareclasses instead), so skip it there.
if (jdkVendor != JvmVendorSpec.IBM) {
    tasks.withType<Jpackage>().configureEach {
        val os = operatingSystem
        val dest = destination
        doLast {
            val image = dest.get().asFile.listFiles()!!.single { it.isDirectory }
            val (launcher, appDir) = when {
                os.get().contains("macos") -> image.resolve("Contents/MacOS/jabkit") to image.resolve("Contents/app")
                os.get().contains("windows") -> image.resolve("jabkit.exe") to image.resolve("app")
                else -> image.resolve("bin/jabkit") to image.resolve("lib/app")
            }
            val cache = appDir.resolve("jabkit.aot")
            val process = ProcessBuilder(launcher.absolutePath, "--version")
                .redirectErrorStream(true)
                .apply { environment()["JAVA_TOOL_OPTIONS"] = "-XX:AOTCacheOutput=${cache.absolutePath}" }
                .start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0 && cache.isFile) { "AOT cache training run failed:\n$output" }
            // ponytail: --version only trains startup classes; record a real workload if profiling data should matter
            appDir.resolve("jabkit.cfg").appendText("java-options=-XX:AOTCache=\$APPDIR/jabkit.aot\n")
        }
    }
}

tasks.register<JavaExec>("runJabKitPortableSmokeTest") {
    group = "test"
    description = "Runs JabKit from test resources dir"
    mainClass = "org.jabref.toolkit.JabKitLauncher"
    mainModule = "org.jabref.jabkit"
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs(application.applicationDefaultJvmArgs)
    workingDir = file("src/test/resources")
    args("--debug", "check", "consistency", "empty.bib")
}

graalvmNative {
    binaries {
        named("main") {
            resources {
                includedPatterns.add("build\\.properties")
            }
            imageName.set("jabkit")
            mainClass.set("org.jabref.toolkit.JabKitLauncher")
        }
    }
}
