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

}

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

    implementation("org.apache.lucene:lucene-core:${luceneVersion}")
    implementation("org.apache.lucene:lucene-queryparser:${luceneVersion}")
    implementation("org.apache.lucene:lucene-queries:${luceneVersion}")
    implementation("org.apache.lucene:lucene-analysis-common:${luceneVersion}")
    implementation("org.apache.lucene:lucene-highlighter:${luceneVersion}")

    implementation("org.jsoup:jsoup:1.19.1")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

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

