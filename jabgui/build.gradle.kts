
import org.gradle.internal.os.OperatingSystem
import org.javamodularity.moduleplugin.extensions.CompileModuleOptions
import org.javamodularity.moduleplugin.extensions.RunModuleOptions

plugins {
    id("buildlogic.java-common-conventions")

    application

    id("org.openjfx.javafxplugin") version("0.1.0")

    // Do not activate; causes issues with the modularity plugin (no tests found etc)
    // id("com.redock.classpathtofile") version "0.1.0"

    id("org.beryx.jlink") version "3.1.1"
}

group = "org.jabref"
version = project.findProperty("projVersion") ?: "100.0.0"

val luceneVersion = "10.2.1"
val pdfbox = "3.0.5"

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
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:12.4.0")
    implementation("com.github.sialcasa.mvvmFX:mvvmfx-validation:f195849ca9") //jitpack
    implementation("de.saxsys:mvvmfx:1.8.0")
    implementation("org.fxmisc.flowless:flowless:0.7.4")
    implementation("org.fxmisc.richtext:richtextfx:0.11.5")
    implementation("com.dlsc.gemsfx:gemsfx:2.104.0") {
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
    implementation("tech.units:indriya:2.2.3")
    // Required by gemsfx and langchain4j
    implementation ("com.squareup.retrofit2:retrofit:3.0.0") {
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

    implementation("org.jsoup:jsoup:1.20.1")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("dev.langchain4j:langchain4j:1.0.0")

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")

    implementation("org.jooq:jool:0.9.15")

    implementation("commons-io:commons-io:2.19.0")

    implementation ("org.apache.pdfbox:pdfbox:$pdfbox") {
        exclude(group = "commons-logging")
    }

    // implementation("net.java.dev.jna:jna:5.16.0")
    implementation("net.java.dev.jna:jna-platform:5.17.0")

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.1.202505142326-r")

    implementation("com.konghq:unirest-java-core:4.4.7")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.4")

    implementation("com.vladsch.flexmark:flexmark-html2md-converter:0.64.8")

    implementation("io.github.adr:e-adr:2.0.0-SNAPSHOT")

    implementation("org.libreoffice:unoloader:24.8.4")
    implementation("org.libreoffice:libreoffice:24.8.4")

    implementation("com.github.javakeyring:java-keyring:1.0.4")

    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    implementation("de.undercouch:citeproc-java:3.3.0") {
        exclude(group = "org.antlr")
    }

    testImplementation(project(":test-support"))

    testImplementation("io.github.classgraph:classgraph:4.8.179")
    testImplementation("org.testfx:testfx-core:4.0.16-alpha")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")

    testImplementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")

    // recommended by https://github.com/wiremock/wiremock/issues/2149#issuecomment-1835775954
    testImplementation("org.wiremock:wiremock-standalone:3.12.1")

    testImplementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
}

javafx {
    version = "24"
    // javafx.swing required by com.dlsc.gemsfx
    modules = listOf("javafx.base", "javafx.graphics", "javafx.fxml", "javafx.web", "javafx.swing")
}

application {
    mainClass.set("org.jabref.Launcher")
    mainModule.set("org.jabref")

    applicationDefaultJvmArgs = listOf(
        // On a change here, also adapt
        //   1. "run > moduleOptions"
        //   2. "binaries.yml" (macOS part)

        // Note that the arguments are cleared for the "run" task to avoid messages like "WARNING: Unknown module: org.jabref.merged.module specified to --add-exports"

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        "-XX:+UseZGC", "-XX:+ZUncommit",
        "-XX:+UseStringDeduplication",

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

        "--enable-native-access=org.jabref.merged.module,ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core"
    )
}

// Workaround for https://github.com/openjfx/javafx-gradle-plugin/issues/89
// See also https://github.com/java9-modularity/gradle-modules-plugin/issues/165
modularity.disableEffectiveArgumentsAdjustment()

/*
jacoco {
    toolVersion = "0.8.13"
}
*/

tasks.named<JavaExec>("run") {
    // "assert" statements in the code should activated when running using gradle
    enableAssertions = true

    doFirst {
        // Clear the default JVM arguments to avoid warnings
        // application.applicationDefaultJvmArgs = emptyList()
        application.applicationDefaultJvmArgs = listOf("--enable-native-access=ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core")
    }

    extensions.configure<RunModuleOptions>("moduleOptions") {
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

tasks.compileJava {
    extensions.configure<CompileModuleOptions> {
        addExports.putAll(
            mapOf(
                // TODO: Remove access to internal api
                "javafx.controls/com.sun.javafx.scene.control" to "org.jabref",
                "org.controlsfx.controls/impl.org.controlsfx.skin" to "org.jabref"
            )
        )
    }
}

tasks.named("jpackage") {
    dependsOn("deleteInstallerTemp")
}

tasks.named("jlinkZip") {
    dependsOn("jpackage")
}

tasks.register<Delete>("deleteInstallerTemp") {
    delete(file("$buildDir/installer"))
}

jlink {
    // https://github.com/beryx/badass-jlink-plugin/issues/61#issuecomment-504640018
    addExtraDependencies(
        "javafx"
    )

    // We keep debug statements - otherwise "--strip-debug" would be included
    addOptions(
        "--compress",
        "zip-6",
        "--no-header-files",
        "--no-man-pages",
        "--bind-services"
    )

    launcher {
        name =
            "JabRef"
    }

    // TODO: Remove as soon as dependencies are fixed (upstream)
    forceMerge(
        "controlsfx",
        "bcprov",
        "jaxb",
        "istack",
        "stax"
    )

    mergedModule {
        requires("com.google.gson")
        requires("com.fasterxml.jackson.annotation")
        requires("com.fasterxml.jackson.databind")
        requires("com.fasterxml.jackson.core")
        requires("com.fasterxml.jackson.datatype.jdk8")
        requires("jakarta.xml.bind")
        requires("java.compiler")
        requires("java.datatransfer")
        requires("java.desktop")
        requires("java.logging")
        requires("java.management")
        requires("java.naming")
        requires("java.net.http")
        requires("java.rmi")
        requires("java.scripting")
        requires("java.security.jgss")
        requires("java.security.sasl")
        requires("java.sql")
        requires("java.sql.rowset")
        requires("java.transaction.xa")
        requires("java.xml")
        requires("javafx.base")
        requires("javafx.controls")
        requires("javafx.fxml")
        requires("javafx.graphics")
        requires("javafx.media")
        requires("javafx.swing")
        requires("jdk.security.jgss")
        requires("jdk.unsupported")
        requires("jdk.unsupported.desktop")
        requires("jdk.xml.dom")
        requires("org.apache.commons.lang3")
        requires("org.apache.commons.logging")
        requires("org.apache.commons.text")
        requires("org.apache.commons.codec")
        requires("org.apache.commons.io")
        requires("org.apache.commons.compress")
        requires("org.freedesktop.dbus")
        requires("org.jsoup")
        requires("org.slf4j")
        requires("org.tukaani.xz");

        uses("ai.djl.engine.EngineProvider")
        uses("ai.djl.repository.RepositoryFactory")
        uses("ai.djl.repository.zoo.ZooProvider")
        uses("dev.langchain4j.spi.prompt.PromptTemplateFactory")
        uses("kong.unirest.core.json.JsonEngine")
        uses("org.eclipse.jgit.lib.Signer")
        uses("org.eclipse.jgit.transport.SshSessionFactory")
        uses("org.postgresql.shaded.com.ongres.stringprep.Profile")

        provides("java.sql.Driver").with(
            "org.postgresql.Driver")
        provides("java.security.Provider").with(
            "org.bouncycastle.jce.provider.BouncyCastleProvider",
            "org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider")
        provides("kong.unirest.core.json.JsonEngine").with(
            "kong.unirest.modules.gson.GsonEngine")
        provides("ai.djl.repository.zoo.ZooProvider").with(
            "ai.djl.engine.rust.zoo.RsZooProvider",
            "ai.djl.huggingface.zoo.HfZooProvider",
            "ai.djl.pytorch.zoo.PtZooProvider",
            "ai.djl.repository.zoo.DefaultZooProvider")
        provides("ai.djl.engine.EngineProvider").with(
            "ai.djl.engine.rust.RsEngineProvider",
            "ai.djl.pytorch.engine.PtEngineProvider")
    }

    jpackage {
        outputDir =
            "distribution"

        if (OperatingSystem.current().isWindows) {
            // This requires WiX to be installed: https://github.com/wixtoolset/wix3/releases
            installerType =  "msi"

            imageOptions.addAll(
                listOf(
                    "--icon", "${projectDir}/src/main/resources/icons/jabref.ico"
                )
            )

            installerOptions.addAll(
                listOf(
                    "--vendor", "JabRef",
                    "--app-version", "$version",
                    "--verbose",
                    "--win-upgrade-uuid", "d636b4ee-6f10-451e-bf57-c89656780e36",
                    "--win-dir-chooser",
                    "--win-shortcut",
                    "--win-menu",
                    "--win-menu-group", "JabRef",
                    "--temp", "${layout.buildDirectory.get()}/installer",
                    "--resource-dir", "$projectDir/buildres/windows",
                    "--license-file", "$projectDir/buildres/LICENSE_with_Privacy.md",
                    "--file-associations", "$projectDir/buildres/windows/bibtexAssociations.properties"
                )
            )
        } else if (OperatingSystem.current().isLinux) {
            imageOptions.addAll(
                listOf(
                    "--icon", "$projectDir/src/main/resources/icons/JabRef-linux-icon-64.png",
                    "--app-version", "$version"
                )
            )

            installerOptions.addAll(
                listOf(
                    "--verbose",
                    "--vendor",  "JabRef",
                    "--app-version", "$version",
                    // "--temp", "$buildDir/installer",
                    "--resource-dir", "$projectDir/buildres/linux",
                    "--linux-menu-group", "Office;",
                    "--linux-rpm-license-type", "MIT",
                    // "--license-file", "$projectDir/LICENSE.md",
                    "--description", "JabRef is an open source bibliography reference manager. Simplifies reference management and literature organization for academic researchers by leveraging BibTeX, native file format for LaTeX.",
                    "--linux-shortcut",
                    "--file-associations", "$projectDir/buildres/linux/bibtexAssociations.properties"
                )
            )
        } else if (OperatingSystem.current().isMacOsX) {
            imageOptions.addAll(
                listOf(
                    "--icon",  "$projectDir/src/main/resources/icons/jabref.icns",
                    "--resource-dir", "$projectDir/buildres/mac"
                )
            )

            skipInstaller = true

            installerOptions.addAll(
                listOf(
                    "--verbose",
                    "--vendor", "JabRef",
                    "--mac-package-identifier", "JabRef",
                    "--mac-package-name", "JabRef",
                    "--app-version", "$version",
                    "--file-associations", "$projectDir/buildres/mac/bibtexAssociations.properties",
                    "--resource-dir", "$projectDir/buildres/mac"
                )
            )
        }
    }
}

if (OperatingSystem.current().isWindows) {
    tasks.named("jpackageImage").configure {
        doLast {
            copy {
                from(file("$projectDir/buildres/windows")) {
                    include(
                        "jabref-firefox.json",
                        "jabref-chrome.json",
                        "JabRefHost.bat",
                        "JabRefHost.ps1"
                    )
                }
                into(file("${layout.buildDirectory.get()}/distribution/JabRef"))
            }
        }
    }
} else if (OperatingSystem.current().isLinux) {
    tasks.named("jpackageImage").configure {
        doLast {
            copy {
                from(file("$projectDir/buildres/linux")) {
                    include("native-messaging-host/**", "jabrefHost.py")
                }
                into(file("${layout.buildDirectory.get()}/distribution/JabRef/lib"))
            }
        }
    }
} else if (OperatingSystem.current().isMacOsX) {
    tasks.named("jpackageImage").configure {
        doLast {
            copy {
                from(file("$projectDir/buildres/mac")) {
                    include("native-messaging-host/**", "jabrefHost.py")
                }
                into(file("${layout.buildDirectory.get()}/distribution/JabRef.app/Contents/Resources"))
            }
        }
    }
}
