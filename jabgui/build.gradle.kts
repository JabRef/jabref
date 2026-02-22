plugins {
    id("org.jabref.gradle.module")
    id("org.jabref.gradle.feature.shadowjar")
    id("application")

    // Do not activate; causes issues with the modularity plugin (no tests found etc)
    // id("com.redock.classpathtofile") version "0.1.0"
}

group = "org.jabref"
version = providers.gradleProperty("projVersion")
    .orElse(providers.environmentVariable("VERSION"))
    .orElse("100.0.0")
    .get()

testModuleInfo {
    requires("org.jabref.testsupport")

    requires("com.github.javaparser.core")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.mockito")
    requires("org.hamcrest")

    requires("org.testfx")
    requires("org.testfx.junit5")

    requires("com.tngtech.archunit")
    requires("com.tngtech.archunit.junit5.api")

    runtimeOnly("com.tngtech.archunit.junit5.engine")
}

application {
    mainClass= "org.jabref.Launcher"

    applicationDefaultJvmArgs = listOf(
        "--add-modules", "jdk.incubator.vector",
        "--enable-native-access=ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core,jkeychain",

        "--add-opens", "java.base/java.nio=org.apache.pdfbox.io",
        // https://github.com/uncomplicate/neanderthal/issues/55
        "--add-opens", "java.base/jdk.internal.ref=org.apache.pdfbox.io",

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        "-XX:+UseStringDeduplication"

        // Default garbage collector (G1) is sufficient
        // More informaiton: https://learn.microsoft.com/en-us/azure/developer/java/containers/overview#understand-jvm-default-ergonomics
        // "-XX:+UseZGC", "-XX:+ZUncommit"
        // "-XX:+UseG1GC"
    )
}

tasks.named<JavaExec>("run") {
    // "assert" statements in the code should activated when running using gradle
    enableAssertions = true
}

// Below should eventually replace the 'jlink {}' and doLast-copy configurations above
javaModulePackaging {
    applicationName = "JabRef"
    verbose = true
    addModules.add("jdk.incubator.vector")

    // general jLinkOptions are set in org.jabref.gradle.base.targets.gradle.kts
    jlinkOptions.addAll("--launcher", "JabRef=org.jabref/org.jabref.Launcher")
    targetsWithOs("windows") {
        jpackageResources = layout.projectDirectory.dir("buildres").dir("windows")
        options.addAll(
            // Needs to be listed everyhwere, because of https://github.com/gradlex-org/java-module-packaging/issues/104
            "--description", "JabRef is an open source bibliography reference manager. Simplifies reference management and literature organization for academic researchers by leveraging BibTeX, native file format for LaTeX.",
            "--license-file", "$projectDir/buildres/LICENSE_with_Privacy.md",

            // Generic options, but different for each target
            "--icon", "$projectDir/buildres/linux/JabRef.png",
            "--file-associations", "$projectDir/buildres/windows/bibtexAssociations.properties",

            // Target-speccific options
            "--win-upgrade-uuid", "d636b4ee-6f10-451e-bf57-c89656780e36",
            "--win-dir-chooser",
            "--win-shortcut",
            "--win-menu",
            "--win-menu-group", "JabRef"
        )
        targetResources.from(layout.projectDirectory.dir("buildres/windows").asFileTree.matching {
            include("jabref-firefox.json")
            include("jabref-chrome.json")
            include("JabRefHost.bat")
            include("JabRefHost.ps1")
        })
    }
    targetsWithOs("linux") {
        jpackageResources = layout.projectDirectory.dir("buildres").dir("linux")
        options.addAll(
            // Needs to be listed everyhwere, because of https://github.com/gradlex-org/java-module-packaging/issues/104
            "--description", "JabRef is an open source bibliography reference manager. Simplifies reference management and literature organization for academic researchers by leveraging BibTeX, native file format for LaTeX.",
            "--license-file", "$projectDir/buildres/LICENSE_with_Privacy.md",

            // Generic options, but different for each target
            "--icon", "$projectDir/buildres/linux/JabRef.png",
            "--file-associations", "$projectDir/buildres/linux/bibtexAssociations.properties",

            // Target-speccific options
            "--linux-menu-group", "Office;",
            // "--linux-rpm-license-type", "MIT", // We currently package for Ubuntu only, which uses deb, not rpm
            "--linux-shortcut"
        )
        targetResources.from(layout.projectDirectory.dir("buildres/linux").asFileTree.matching {
            include("native-messaging-host/**")
            include("jabrefHost.py")
        })
    }
    targetsWithOs("macos") {
        jpackageResources = layout.projectDirectory.dir("buildres").dir("macos")
        options.addAll(
            // Needs to be listed everyhwere, because of https://github.com/gradlex-org/java-module-packaging/issues/104
            "--description", "JabRef is an open source bibliography reference manager. Simplifies reference management and literature organization for academic researchers by leveraging BibTeX, native file format for LaTeX.",
            "--license-file", "$projectDir/buildres/LICENSE_with_Privacy.md",

            // Generic options, but different for each target
            "--icon", "$projectDir/buildres/macos/JabRef.icns",
            "--file-associations", "$projectDir/buildres/macos/bibtexAssociations.properties",

            // Target-speccific options
            "--mac-package-identifier", "JabRef",
            "--mac-package-name", "JabRef"
        )
        if (providers.environmentVariable("OSXCERT").orNull?.isNotBlank() ?: false) {
            options.addAll(
                "--mac-sign",
                "--mac-signing-key-user-name", "JabRef e.V. (6792V39SK3)",
                "--mac-package-signing-prefix", "org.jabref.",
            )
        }
        targetResources.from(layout.projectDirectory.dir("buildres/macos").asFileTree.matching {
            include("Resources/**")
        })
    }
}

tasks.test {
    jvmArgs = listOf(
        "-javaagent:${configurations.mockitoAgent.get().asPath}",

        // Source: https://github.com/TestFX/TestFX/issues/638#issuecomment-433744765
        "--add-opens", "javafx.graphics/com.sun.javafx.application=org.testfx",

        "--add-opens", "java.base/jdk.internal.ref=org.apache.pdfbox.io",
        "--add-opens", "java.base/java.nio=org.apache.pdfbox.io",
        "--enable-native-access=javafx.graphics,javafx.web,com.sun.jna"

        // "--add-reads", "org.mockito=java.prefs",
        // "--add-reads", "org.jabref=wiremock"
    )

    maxParallelForks = 1
}
