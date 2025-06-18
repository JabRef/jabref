import org.gradle.internal.os.OperatingSystem

plugins {
    id("org.jabref.gradle.module")
    id("application")

    // Do not activate; causes issues with the modularity plugin (no tests found etc)
    // id("com.redock.classpathtofile") version "0.1.0"

    id("org.beryx.jlink") version "3.1.1"
}

group = "org.jabref"
version = project.findProperty("projVersion") ?: "100.0.0"

dependencies {
    implementation(project(":jablib"))

    implementation("org.openjfx:javafx-base")
    implementation("org.openjfx:javafx-controls")
    implementation("org.openjfx:javafx-fxml")
    // implementation("org.openjfx:javafx-graphics")
    implementation("org.openjfx:javafx-graphics")
    implementation("org.openjfx:javafx-swing")
    implementation("org.openjfx:javafx-web")

    implementation("org.slf4j:slf4j-api")
    implementation("org.tinylog:tinylog-api")
    implementation("org.tinylog:slf4j-tinylog")
    implementation("org.tinylog:tinylog-impl")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")

    implementation("org.jabref:afterburner.fx")
    implementation("org.kordamp.ikonli:ikonli-javafx")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack")
    implementation("com.github.sialcasa.mvvmFX:mvvmfx-validation:f195849ca9") //jitpack
    implementation("de.saxsys:mvvmfx")
    implementation("org.fxmisc.flowless:flowless")
    implementation("org.fxmisc.richtext:richtextfx")
    implementation("com.dlsc.gemsfx:gemsfx")
    implementation("com.dlsc.pdfviewfx:pdfviewfx")

    // Required by gemsfx
    implementation("tech.units:indriya")
    // Required by gemsfx and langchain4j
    implementation ("com.squareup.retrofit2:retrofit")

    implementation("org.controlsfx:controlsfx")
    implementation("org.jabref:easybind")

    implementation("org.apache.lucene:lucene-core")
    implementation("org.apache.lucene:lucene-queryparser")
    implementation("org.apache.lucene:lucene-queries")
    implementation("org.apache.lucene:lucene-analysis-common")
    implementation("org.apache.lucene:lucene-highlighter")

    implementation("org.jsoup:jsoup")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify")

    implementation("com.google.guava:guava")

    implementation("dev.langchain4j:langchain4j")

    implementation("io.github.java-diff-utils:java-diff-utils")

    implementation("org.jooq:jool")

    implementation("commons-io:commons-io")

    implementation ("org.apache.pdfbox:pdfbox")

    // implementation("net.java.dev.jna:jna")
    implementation("net.java.dev.jna:jna-platform")

    implementation("org.eclipse.jgit:org.eclipse.jgit")

    implementation("com.konghq:unirest-java-core")

    implementation("org.apache.httpcomponents.client5:httpclient5")

    implementation("com.vladsch.flexmark:flexmark-html2md-converter")

    implementation("io.github.adr:e-adr")

    implementation("org.libreoffice:unoloader")
    implementation("org.libreoffice:libreoffice")

    implementation("com.github.javakeyring:java-keyring")

    implementation("info.picocli:picocli")
    annotationProcessor("info.picocli:picocli-codegen")

    implementation("de.undercouch:citeproc-java")

    testImplementation(project(":test-support"))

    testImplementation("io.github.classgraph:classgraph")
    testImplementation("org.testfx:testfx-core")
    testImplementation("org.testfx:testfx-junit5")

    testImplementation("org.mockito:mockito-core")
    testImplementation("net.bytebuddy:byte-buddy")

    testImplementation("org.wiremock:wiremock")

    testImplementation("com.github.javaparser:javaparser-symbol-solver-core")
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

        // Fix for https://github.com/JabRef/jabref/issues/11225 on linux
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref",
        "--add-exports=javafx.base/com.sun.javafx.event=org.jabref",
        "--add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref",
        "--add-opens=javafx.graphics/javafx.scene=org.jabref",
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref",
        "--add-opens=javafx.controls/com.sun.javafx.scene.control=org.jabref",

        "--add-opens=javafx.base/javafx.collections=org.jabref",
        "--add-opens=javafx.base/javafx.collections.transformation=org.jabref",

        "--enable-native-access=ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core"
    )
}

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
        application.applicationDefaultJvmArgs =
            listOf(
                "--enable-native-access=ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core"
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
    delete(file("${layout.buildDirectory.get()}/installer"))
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
        "--bind-services",
        "--add-modules", "jdk.incubator.vector"
    )

    launcher {
        name = "JabRef"
        jvmArgs = listOf(
            // Fix for https://github.com/JabRef/jabref/issues/11188
            "--add-exports=javafx.base/com.sun.javafx.event=org.jabref.merged.module",
            "--add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref.merged.module",

            // Fix for https://github.com/JabRef/jabref/issues/11198
            "--add-opens=javafx.graphics/javafx.scene=org.jabref.merged.module",
            "--add-opens=javafx.controls/javafx.scene.control=org.jabref.merged.module",
            "--add-opens=javafx.controls/com.sun.javafx.scene.control=org.jabref.merged.module",
            // fix for https://github.com/JabRef/jabref/issues/11426
            "--add-opens=javafx.controls/javafx.scene.control.skin=org.jabref.merged.module",

            "--enable-native-access=org.jabref.merged.module"
        )
    }

    // TODO: Remove as soon as dependencies are fixed (upstream)
    forceMerge(
        "controlsfx",
        "bcprov",
        "stax"
    )

    mergedModule {
        requires("com.google.gson")
        requires("com.fasterxml.jackson.annotation")
        requires("com.fasterxml.jackson.databind")
        requires("com.fasterxml.jackson.core")
        requires("com.fasterxml.jackson.datatype.jdk8")
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

    // This tasks reads resources from src/main/resourcesPackage/$OS
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

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("org.jabref.testsupport")

    requires.add("org.junit.jupiter.api")
    requires.add("org.junit.jupiter.params")
    requires.add("org.mockito")
    requires.add("wiremock")
    requires.add("wiremock.slf4j.spi.shim")
}

tasks.test {
    jvmArgs = listOf(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=org.testfx",
        "--add-reads", "org.mockito=java.prefs",
        "--add-reads", "org.jabref=wiremock"
    )
}
