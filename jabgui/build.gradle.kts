plugins {
    id("buildlogic.java-common-conventions")

    application

    id("org.openjfx.javafxplugin") version("0.1.0")

    // This is https://github.com/java9-modularity/gradle-modules-plugin/pull/282
    id("com.github.koppor.gradle-modules-plugin") version "v1.8.15-cmd-1"

    id("com.github.andygoossens.modernizer") version "1.10.0"
    id("org.openrewrite.rewrite") version "7.3.0"

    // nicer test outputs during running and completion
    // Homepage: https://github.com/radarsh/gradle-test-logger-plugin
    id("com.adarshr.test-logger") version "4.0.0"

    id("org.itsallcode.openfasttrace") version "3.0.1"

    id("org.beryx.jlink") version "3.1.1"
}

group = "org.jabref"
version = project.findProperty("projVersion") ?: "100.0.0"

val luceneVersion = "10.2.0"

dependencies {
    implementation(project(":jablib"))

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:12.3.1")
    implementation("com.github.sialcasa.mvvmFX:mvvmfx-validation:f195849ca9") //jitpack
    implementation("de.saxsys:mvvmfx:1.8.0")
    implementation("org.fxmisc.flowless:flowless:0.7.4")
    implementation("org.fxmisc.richtext:richtextfx:0.11.5")
    implementation("com.dlsc.gemsfx:gemsfx:2.96.0") {
        exclude(module = "javax.inject") // Split package, use only jakarta.inject
        exclude(module = "commons-lang3")
        exclude(group = "org.apache.commons.validator")
        exclude(group = "org.apache.commons.commons-logging")
        exclude(module = "kotlin-stdlib-jdk8")
        exclude(group = "com.squareup.retrofit2")
        exclude(group = "org.openjfx")
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "tech.units")
    }

    // Required by gemsfx
    implementation("tech.units:indriya:2.2.2")
    // Required by gemsfx and langchain4j
    implementation ("com.squareup.retrofit2:retrofit:2.11.0") {
        exclude(group = "com.squareup.okhttp3")
    }

    implementation("org.controlsfx:controlsfx:11.2.2")
    implementation("org.jabref:easybind:2.2.1-SNAPSHOT") {
        exclude(group = "org.openjfx")
    }

    implementation("org.apache.lucene:lucene-core:${luceneVersion}")
    implementation("org.apache.lucene:lucene-queryparser:${luceneVersion}")
    implementation("org.apache.lucene:lucene-queries:${luceneVersion}")
    implementation("org.apache.lucene:lucene-analysis-common:${luceneVersion}")
    implementation("org.apache.lucene:lucene-highlighter:${luceneVersion}")

    implementation("org.jsoup:jsoup:1.19.1")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

    testImplementation("org.testfx:testfx-core:4.0.16-alpha")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.5.0"))
    rewrite("org.openrewrite.recipe:rewrite-static-analysis")
    rewrite("org.openrewrite.recipe:rewrite-logging-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
}

javafx {
    version = "24"
    modules = listOf("javafx.base", "javafx.graphics", "javafx.fxml", "javafx.web")
}

application {
    mainClass.set("org.jabref.Launcher")
    mainModule.set("org.jabref")

    applicationDefaultJvmArgs = listOf(
        // On a change here, also adapt
        //   1. "run > moduleOptions"
        //   2. "deployment.yml" (macOS part)
        //   3. "deployment-arm64.yml"

        // Note that the arguments are cleared for the "run" task to avoid messages like "WARNING: Unknown module: org.jabref.merged.module specified to --add-exports"

        // Fix for https://github.com/JabRef/jabref/issues/11188
        "--add-exports=javafx.base/com.sun.javafx.event=org.jabref.merged.module",
        "--add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref.merged.module",

        // Fix for https://github.com/JabRef/jabref/issues/11198
        "--add-opens=javafx.graphics/javafx.scene=org.jabref.merged.module",
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref.merged.module",
        "--add-opens=javafx.controls/com.sun.javafx.scene.control=org.jabref.merged.module",
        // fix for https://github.com/JabRef/jabref/issues/11426
        "--add-opens=javafx.controls/javafx.scene.control.skin=org.jabref.merged.module",

        // Fix for https://github.com/JabRef/jabref/issues/11225 on linux
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref",
        "--add-exports=javafx.base/com.sun.javafx.event=org.jabref",
        "--add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref",
        "--add-opens=javafx.graphics/javafx.scene=org.jabref",
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref",
        "--add-opens=javafx.controls/com.sun.javafx.scene.control=org.jabref",

        "--add-opens=javafx.base/javafx.collections=org.jabref",
        "--add-opens=javafx.base/javafx.collections.transformation=org.jabref",

        "--enable-native-access=javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core"
    )
}

// Workaround for https://github.com/openjfx/javafx-gradle-plugin/issues/89
// See also https://github.com/java9-modularity/gradle-modules-plugin/issues/165
modularity.disableEffectiveArgumentsAdjustment()

jacoco {
    toolVersion = "0.8.13"
}

tasks.named<JavaExec>("run") {
    doFirst {
        // Clear the default JVM arguments to avoid warnings
        application.applicationDefaultJvmArgs = emptyList()
    }

    extensions.configure<org.javamodularity.moduleplugin.extensions.RunModuleOptions>("moduleOptions") {
        // On a change here, also adapt "application > applicationDefaultJvmArgs"
        addExports.putAll(
            mapOf(
                // TODO: Remove access to internal API
                "javafx.base/com.sun.javafx.event" to "org.jabref.merged.module",
                "javafx.controls/com.sun.javafx.scene.control" to "org.jabref",

                // ControlsFX compatibility
                // We need to restate the ControlsFX exports, because we get following error otherwise:
                //   java.lang.IllegalAccessError:
                //     class org.controlsfx.control.textfield.AutoCompletionBinding (in module org.controlsfx.controls)
                //     cannot access class com.sun.javafx.event.EventHandlerManager (in module javafx.base) because
                //     module javafx.base does not export com.sun.javafx.event to module org.controlsfx.controls
                // Taken from here: https://github.com/controlsfx/controlsfx/blob/9.0.0/build.gradle#L1
                "javafx.graphics/com.sun.javafx.scene" to "org.controlsfx.controls",
                "javafx.graphics/com.sun.javafx.scene.traversal" to "org.controlsfx.controls",
                "javafx.graphics/com.sun.javafx.css" to "org.controlsfx.controls",
                "javafx.controls/com.sun.javafx.scene.control" to "org.controlsfx.controls",
                "javafx.controls/com.sun.javafx.scene.control.behavior" to "org.controlsfx.controls",
                "javafx.controls/com.sun.javafx.scene.control.inputmap" to "org.controlsfx.controls",
                "javafx.base/com.sun.javafx.event" to "org.controlsfx.controls",
                "javafx.base/com.sun.javafx.collections" to "org.controlsfx.controls",
                "javafx.base/com.sun.javafx.runtime" to "org.controlsfx.controls",
                "javafx.web/com.sun.webkit" to "org.controlsfx.controls"
            )
        )

        addOpens.putAll(
            mapOf(
                "javafx.controls/javafx.scene.control" to "org.jabref",
                "javafx.controls/com.sun.javafx.scene.control" to "org.jabref",
                "org.controlsfx.controls/impl.org.controlsfx.skin" to "org.jabref",
                "org.controlsfx.controls/org.controlsfx.control.textfield" to "org.jabref",
                "javafx.controls/javafx.scene.control.skin" to "org.controlsfx.controls",
                "javafx.graphics/javafx.scene" to "org.controlsfx.controls",
                "javafx.base/javafx.collections" to "org.jabref",
                "javafx.base/javafx.collections.transformation" to "org.jabref"
            )
        )

        addModules.add("jdk.incubator.vector")

        createCommandLineArgumentFile = true
    }
}

