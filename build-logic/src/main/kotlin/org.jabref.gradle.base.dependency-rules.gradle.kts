import org.gradlex.javamodule.dependencies.tasks.ModuleDirectivesOrderingCheck

plugins {
    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.jvm-dependency-conflict-resolution")
    id("org.gradlex.java-module-dependencies")
    id("com.autonomousapps.dependency-analysis")
}

// module-info entry order not checked
tasks.withType<ModuleDirectivesOrderingCheck> { enabled = false }

jvmDependencyConflicts {
    consistentResolution {
        platform(":versions")

        // Use a consistent set of versions (including all transitive versions) for all applications
        providesVersions(":jabgui")
        providesVersions(":jabkit")
        providesVersions(":jabls-cli")
        providesVersions(":jabsrv-cli")
    }
    conflictResolution {
        select("org.gradlex:jna", "net.java.dev.jna:jna-jpms")
        select("org.gradlex:jna-platform", "net.java.dev.jna:jna-platform-jpms")
    }
}

// Tell gradle which jar to use for which platform
// Source: https://github.com/jjohannes/java-module-system/blob/be19f6c088dca511b6d9a7487dacf0b715dbadc1/gradle/plugins/src/main/kotlin/metadata-patch.gradle.kts#L14-L22
listOf("javafx-base", "javafx-controls", "javafx-fxml", "javafx-graphics", "javafx-swing", "javafx-web", "javafx-media", "jdk-jsobject").forEach { jfxModule ->
    addJfxTarget(jfxModule, "", "none", "none") // matches the empty Jars: to get better errors
    addJfxTarget(jfxModule, "linux", OperatingSystemFamily.LINUX, MachineArchitecture.X86_64)
    addJfxTarget(jfxModule, "linux-aarch64", OperatingSystemFamily.LINUX, MachineArchitecture.ARM64)
    addJfxTarget(jfxModule, "mac", OperatingSystemFamily.MACOS, MachineArchitecture.X86_64)
    addJfxTarget(jfxModule, "mac-aarch64", OperatingSystemFamily.MACOS, MachineArchitecture.ARM64)
    addJfxTarget(jfxModule, "win", OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64)
}

fun addJfxTarget(jfxModule: String, name: String, os: String, arch: String) {
    if (jfxModule == "javafx-web" && name.isNotEmpty()) {
        // Special treatment of 'javafx-web' for the time being due to https://bugs.openjdk.org/browse/JDK-8342623.
        // Can be remove once Java 26 is the minimum version JabRef is built with.
        dependencies.components.withModule<JDKjsobjectDependencyMetadataRule>("org.openjfx:$jfxModule") {
            params(name, os, arch, 11)
        }
        dependencies.components.withModule<JDKjsobjectDependencyMetadataRule>("org.openjfx:$jfxModule") {
            params(name, os, arch, 26)
        }
    } else {
        jvmDependencyConflicts.patch.module("org.openjfx:$jfxModule") {
            addTargetPlatformVariant(name, os, arch)
        }
    }
}

jvmDependencyConflicts.patch {
    // Source: https://github.com/jjohannes/java-module-system/blob/be19f6c088dca511b6d9a7487dacf0b715dbadc1/gradle/plugins/src/main/kotlin/metadata-patch.gradle.kts#L9
    module("com.google.guava:guava") {
        removeDependency("com.google.code.findbugs:jsr305")
        removeDependency("org.checkerframework:checker-qual")
        removeDependency("com.google.errorprone:error_prone_annotations")
    }
    module("org.jetbrains.kotlin:kotlin-stdlib") {
        removeDependency("org.jetbrains.kotlin:kotlin-stdlib-common") // not needed
    }
    module("com.konghq:unirest-modules-gson") {
        addApiDependency("com.konghq:unirest-java-core")
    }
    module("de.rototor.jeuclid:jeuclid-core") {
        removeDependency("org.apache.xmlgraphics:batik-svg-dom")
        removeDependency("org.apache.xmlgraphics:batik-ext")
        removeDependency("org.apache.xmlgraphics:xmlgraphics-commons")
    }

    module("org.apache.logging.log4j:log4j-to-slf4j") {
        // remove non-module annotation libraries only used at compile time
        removeDependency("com.github.spotbugs:spotbugs-annotations")
        removeDependency("org.osgi:org.osgi.annotation.versioning")
        removeDependency("org.osgi:org.osgi.annotation.bundle")
        removeDependency("biz.aQute.bnd:biz.aQute.bnd.annotation")
    }
    module("org.apache.logging.log4j:log4j-api") {
        // remove non-module annotation libraries only used at compile time
        removeDependency("com.github.spotbugs:spotbugs-annotations")
        removeDependency("org.osgi:org.osgi.annotation.versioning")
        removeDependency("org.osgi:org.osgi.annotation.bundle")
        removeDependency("biz.aQute.bnd:biz.aQute.bnd.annotation")
    }

    module("org.testfx:testfx-core") {
        removeDependency("org.osgi:org.osgi.core")
    }
    module("org.xmlunit:xmlunit-legacy") {
        removeDependency("junit:junit")
    }
    module("dev.langchain4j:langchain4j-core") {
        addRuntimeOnlyDependency("com.knuddels:jtokkit")
    }
    module("org.jabref:afterburner.fx") {
        // metadata decared these as runtime only, but they are 'requires transitive' in module-info
        addApiDependency("org.openjfx:javafx-fxml")
        addApiDependency("org.openjfx:javafx-controls")
    }
    module("org.libreoffice:libreoffice") {
        // no dependency in metadata, but 'requires org.libreoffice.unoloader' in module-info
        addRuntimeOnlyDependency("org.libreoffice:unoloader")
    }
}

extraJavaModuleInfo {
    versionsProvidingConfiguration = "mainRuntimeClasspath"
    failOnAutomaticModules = true
    failOnModifiedDerivedModuleNames = true
    skipLocalJars = true

    knownModule("com.github.hypfvieh:dbus-java-core", "org.freedesktop.dbus")
    knownModule("com.github.hypfvieh:dbus-java-transport-native-unixsocket", "org.freedesktop.dbus.transport.jre")

    module("ai.djl.huggingface:tokenizers", "ai.djl.tokenizers") {
        exportAllPackages()
        requires("ai.djl.api")
        requires("org.slf4j")
    }
    module("ai.djl.pytorch:pytorch-engine", "ai.djl.pytorch_engine") {
        exportAllPackages()
        exportAllPackages()
        requires("ai.djl.api")
        requires("org.slf4j")
    }
    module("ai.djl.pytorch:pytorch-model-zoo", "ai.djl.pytorch_model_zoo") {
        exportAllPackages()
        requires("ai.djl.api")
        requires("org.slf4j")
    }
    module("ai.djl:api", "ai.djl.api") {
        exportAllPackages()
        requires("com.google.gson")
        requires("org.slf4j")
        uses("ai.djl.engine.EngineProvider")
        uses("ai.djl.repository.zoo.ZooProvider")
        uses("ai.djl.repository.RepositoryFactory")
    }
    module("at.favre.lib:hkdf", "hkdf")

    module("cc.jilt:jilt", "jilt")         {
        exportAllPackages()
        requires("java.compiler") // Reason: javax.annotation.processor
    }

    module("com.github.javakeyring:java-keyring", "java.keyring")

    module("com.github.tomtung:latex2unicode_2.13", "com.github.tomtung.latex2unicode") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("com.lihaoyi:fastparse_2.13", "fastparse") {
        overrideModuleName() // fastparse_2.13 is not a valid name
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("scala.library")
    }
    module("com.lihaoyi:sourcecode_2.13", "com.lihaoyi.sourcecode") {
        overrideModuleName() // sourcecode_2.13 is not a valid name
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("scala.library")
    }
    module("com.lihaoyi:geny_2.13", "com.lihaoyi.geny") {
        overrideModuleName() // geny_2.13 is not a valid name
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("scala.library")
    }

    module("com.h2database:h2-mvstore", "com.h2database.mvstore")
    module("com.ibm.icu:icu4j", "com.ibm.icu")
    module("com.knuddels:jtokkit", "jtokkit")
    module("com.konghq:unirest-java-core", "unirest.java.core") {
        exportAllPackages()
        requires("java.net.http")
        uses("kong.unirest.core.json.JsonEngine")
    }
    module("com.konghq:unirest-modules-gson", "unirest.modules.gson")
    module("com.squareup.okhttp3:okhttp", "okhttp3")
    module("com.squareup.okhttp3:okhttp-jvm-sse", "okhttp3.sse")
    module("com.squareup.okio:okio", "okio")
    module("com.squareup.retrofit2:converter-jackson", "retrofit2.converter.jackson")
    module("com.squareup.retrofit2:retrofit", "retrofit2")
    module("com.vladsch.flexmark:flexmark", "flexmark")
    module("com.vladsch.flexmark:flexmark-ext-emoji", "flexmark.ext.emoji")
    module("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough", "flexmark.ext.gfm.strikethrough")
    module("com.vladsch.flexmark:flexmark-ext-ins", "flexmark.ext.ins")
    module("com.vladsch.flexmark:flexmark-ext-superscript", "flexmark.ext.superscript")
    module("com.vladsch.flexmark:flexmark-ext-tables", "flexmark.ext.tables")
    module("com.vladsch.flexmark:flexmark-ext-wikilink", "flexmark.ext.wikilink")
    module("com.vladsch.flexmark:flexmark-html2md-converter", "flexmark.html2md.converter")
    module("com.vladsch.flexmark:flexmark-jira-converter", "flexmark.jira.converter")
    module("com.vladsch.flexmark:flexmark-util", "flexmark.util")
    module("com.vladsch.flexmark:flexmark-util-ast", "flexmark.util.ast")
    module("com.vladsch.flexmark:flexmark-util-builder", "flexmark.util.builder")
    module("com.vladsch.flexmark:flexmark-util-collection", "flexmark.util.collection")
    module("com.vladsch.flexmark:flexmark-util-data", "flexmark.util.data")
    module("com.vladsch.flexmark:flexmark-util-dependency", "flexmark.util.dependency")
    module("com.vladsch.flexmark:flexmark-util-format", "flexmark.util.format")
    module("com.vladsch.flexmark:flexmark-util-html", "flexmark.util.html")
    module("com.vladsch.flexmark:flexmark-util-misc", "flexmark.util.misc")
    module("com.vladsch.flexmark:flexmark-util-options", "flexmark.util.options")
    module("com.vladsch.flexmark:flexmark-util-sequence", "flexmark.util.sequence")
    module("com.vladsch.flexmark:flexmark-util-visitor", "flexmark.util.visitor")
    module("commons-beanutils:commons-beanutils", "commons.beanutils")
    module("commons-collections:commons-collections", "commons.collections")
    module("commons-digester:commons-digester", "commons.digester")
    module("de.rototor.jeuclid:jeuclid-core", "jeuclid.core")
    module("de.rototor.snuggletex:snuggletex-core", "snuggletex.core")
    module("de.rototor.snuggletex:snuggletex-jeuclid", "snuggletex.jeuclid")
    module("de.swiesend:secret-service", "secret.service")
    module("de.undercouch:citeproc-java", "citeproc.java") {
        exportAllPackages()
        requires("java.xml")
        requires("org.antlr.antlr4.runtime")
        requires("org.apache.commons.lang3")
        requires("org.apache.commons.text")
        requires("jbibtex")
        // Compile time only
        // requires("jackson.annotations")
    }
    module("dev.langchain4j:langchain4j", "langchain4j")
    module("dev.langchain4j:langchain4j-core", "langchain4j.core") {
        // workaround for https://github.com/langchain4j/langchain4j/issues/3668
        patchRealModule()
        mergeJar("dev.langchain4j:langchain4j-http-client")
        mergeJar("dev.langchain4j:langchain4j-http-client-jdk")
        mergeJar("dev.langchain4j:langchain4j-hugging-face")
        mergeJar("dev.langchain4j:langchain4j-mistral-ai")
        mergeJar("dev.langchain4j:langchain4j-open-ai")
        mergeJar("dev.langchain4j:langchain4j-google-ai-gemini")
        requires("java.net.http")
        uses("dev.langchain4j.http.client.HttpClientBuilderFactory")
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("dev.langchain4j:langchain4j-google-ai-gemini", "langchain4j.google.ai.gemini")
    module("dev.langchain4j:langchain4j-http-client", "langchain4j.http.client")
    module("dev.langchain4j:langchain4j-http-client-jdk", "langchain4j.http.client.jdk")
    module("dev.langchain4j:langchain4j-hugging-face", "langchain4j.hugging.face")
    module("dev.langchain4j:langchain4j-mistral-ai", "langchain4j.mistral.ai")
    module("dev.langchain4j:langchain4j-open-ai", "langchain4j.open.ai")
    module("eu.lestard:doc-annotations", "doc.annotations")
    module("info.debatty:java-string-similarity", "java.string.similarity")
    module("io.github.java-diff-utils:java-diff-utils", "io.github.javadiffutils")
    module("io.zonky.test.postgres:embedded-postgres-binaries-darwin-amd64", "embedded.postgres.binaries.darwin.amd64")
    module("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8", "embedded.postgres.binaries.darwin.arm64v8")
    module("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64", "embedded.postgres.binaries.linux.amd64")
    module("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64-alpine", "embedded.postgres.binaries.linux.amd64.alpine")
    module("io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8", "embedded.postgres.binaries.linux.arm64v8")
    module("io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64", "embedded.postgres.binaries.windows.amd64")
    module("net.harawata:appdirs", "net.harawata.appdirs")
    module("net.java.dev.jna:jna", "com.sun.jna") {
        // Required as sometimes the non-jpms version if picked which should ideally be fixed differently
        // More details: https://github.com/gradlex-org/jvm-dependency-conflict-resolution/issues/346
        patchRealModule()
        exportAllPackages()
        requires("java.logging")
    }
    module("net.java.dev.jna:jna-platform", "com.sun.jna.platform")
    module("net.jcip:jcip-annotations", "jcip.annotations")
    module("net.jodah:typetools", "typetools")
    module("org.abego.treelayout:org.abego.treelayout.core", "org.abego.treelayout.core")
    module("org.antlr:antlr4-runtime", "org.antlr.antlr4.runtime")
    module("org.apache.httpcomponents.client5:httpclient5", "org.apache.httpcomponents.client5.httpclient5")
    module("org.apache.httpcomponents.core5:httpcore5", "org.apache.httpcomponents.core5.httpcore5")
    module("org.apache.httpcomponents.core5:httpcore5-h2", "org.apache.httpcomponents.core5.httpcore5.h2")
    module("org.apache.httpcomponents:httpclient", "org.apache.httpcomponents.httpclient")
    module("org.apache.opennlp:opennlp-tools", "org.apache.opennlp.tools")
    module("org.apache.pdfbox:fontbox", "org.apache.fontbox") {
        requires("java.desktop")
        requires("org.apache.pdfbox.io")
        requires("org.apache.commons.logging")
    }
    module("org.apache.pdfbox:pdfbox-io", "org.apache.pdfbox.io")
    module("org.apache.velocity:velocity-engine-core", "velocity.engine.core")
    module("org.eclipse.jgit:org.eclipse.jgit", "org.eclipse.jgit") {
        exportAllPackages()
        requires("org.slf4j")
        uses("org.eclipse.jgit.lib.SignerFactory")
    }
    module("org.fxmisc.undo:undofx", "org.fxmisc.undo")
    module("org.fxmisc.wellbehaved:wellbehavedfx", "wellbehavedfx") {
        exportAllPackages()
        requires("javafx.graphics")
    }
    module("org.javassist:javassist", "org.javassist")
    module("org.jbibtex:jbibtex", "jbibtex")
    module("org.scala-lang:scala-library", "scala.library")
    module("pt.davidafsilva.apple:jkeychain", "jkeychain")

    module("org.testfx:testfx-core", "org.testfx") {
        exportAllPackages()
        // Content based on https://github.com/TestFX/TestFX/commit/bf4a08aa82c008fdd3c296aaafee1d222f3824cb
        requires("java.desktop")
        requiresTransitive("javafx.controls")
        requiresTransitive("org.hamcrest")
    }
    module("org.testfx:testfx-junit5", "org.testfx.junit5") {
        exportAllPackages()
        requires("org.junit.jupiter.api")
        requiresTransitive("org.testfx")
    }

    module("org.xmlunit:xmlunit-core", "org.xmlunit") {
        exportAllPackages()
        requires("java.xml")
    }
    module("org.xmlunit:xmlunit-legacy", "org.custommonkey.xmlunit") {
        exportAllPackages()
        requires("java.xml")
    }

    module("org.xmlunit:xmlunit-matchers", "org.xmlunit.matchers") {
        exportAllPackages()
        requires("java.logging")
        requires("java.xml")
        requires("org.hamcrest")
        requires("org.xmlunit")
    }
    module("org.xmlunit:xmlunit-placeholders", "org.xmlunit.placeholder")

    module("net.javacrumbs.json-unit:json-unit-core", "net.javacrumbs.jsonunit.core")
    module("com.github.javaparser:javaparser-core", "com.github.javaparser.core")
    module("com.github.javaparser:javaparser-symbol-solver-core", "com.github.javaparser.symbolsolver.core")
    module("net.sf.jopt-simple:jopt-simple", "jopt.simple")

    module("org.eclipse.lsp4j:org.eclipse.lsp4j", "org.eclipse.lsp4j") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("com.google.gson")
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.debug", "org.eclipse.lsp4j.debug")
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.generator", "org.eclipse.lsp4j.generator")
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc", "org.eclipse.lsp4j.jsonrpc") {
        exportAllPackages()
        requires("com.google.gson")
        requires("java.logging")
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc.debug", "org.eclipse.lsp4j.jsonrpc.debug")
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.websocket.jakarta", "org.eclipse.lsp4j.websocket.jakarta")
    module("jakarta.websocket:jakarta.websocket-api", "jakarta.websocket")
    module("org.eclipse.xtend:org.eclipse.xtend", "xtend")
    module("org.eclipse.xtend:org.eclipse.xtend.lib", "xtend.lib")
    module("org.eclipse.xtend:org.eclipse.xtend.lib.macro", "xtend.lib.macro")
    module("org.eclipse.xtext:org.eclipse.xtext.xbase.lib", "xtext.xbase.lib")

    module("com.tngtech.archunit:archunit-junit5-api", "com.tngtech.archunit.junit5.api")
    module("com.tngtech.archunit:archunit-junit5-engine", "com.tngtech.archunit.junit5.engine")
    module("com.tngtech.archunit:archunit-junit5-engine-api", "com.tngtech.archunit.junit5.engineapi")
    module("com.tngtech.archunit:archunit", "com.tngtech.archunit") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        uses("com.tngtech.archunit.lang.extension.ArchUnitExtension")
    }

    module("org.glassfish.hk2.external:aopalliance-repackaged", "org.aopalliance")

    module("org.glassfish.hk2:hk2-locator", "org.glassfish.hk2.locator") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("org.glassfish.hk2:hk2-api", "org.glassfish.hk2.api") {
        exportAllPackages()
        requireAllDefinedDependencies()
        uses("org.glassfish.hk2.extension.ServiceLocatorGenerator")
    }
    module("org.glassfish.hk2:hk2-utils", "org.glassfish.hk2.utilities") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("org.glassfish.hk2:osgi-resource-locator", "osgi.resource.locator") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }

    module("de.saxsys:mvvmfx", "de.saxsys.mvvmfx") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.base")
    }
    module("de.saxsys:mvvmfx-validation", "de.saxsys.mvvmfx.validation") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.base")
        requiresTransitive("javafx.controls")
        requiresTransitive("org.controlsfx.controls")
    }
    // JitPack-variant because we need the latest commit
    module("com.github.sialcasa.mvvmFX:mvvmfx-validation", "de.saxsys.mvvmfx.validation") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.base")
        requiresTransitive("javafx.controls")
        requiresTransitive("org.controlsfx.controls")
    }

    module("org.reactfx:reactfx", "reactfx") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.controls")
    }
    module("org.fxmisc.flowless:flowless", "org.fxmisc.flowless") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.controls")
    }
    module("org.fxmisc.richtext:richtextfx", "org.fxmisc.richtext") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.graphics")
    }
    module("io.github.stefanbratanov:jvm-openai", "jvm.openai") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.net.http")
    }
    module("io.zonky.test:embedded-postgres", "embedded.postgres") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.sql")
    }
    module("org.postgresql:postgresql", "org.postgresql.jdbc") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.management")
        requires("java.naming")
        requires("java.sql")
    }
    module("org.apache.pdfbox:pdfbox", "org.apache.pdfbox") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.desktop")
    }
    module("org.apache.pdfbox:xmpbox", "org.apache.xmpbox") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.xml")
    }
    module("com.squareup.okio:okio-jvm", "okio") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }

    module("org.openjfx:javafx-base", "javafx.base") {
        preserveExisting()
        exports("javafx.collections")
        opens("com.sun.javafx.beans", "net.bytebuddy")
    }

    // open packages for testing of jablib
    module("org.openjfx:javafx-fxml", "javafx.fxml") {
        preserveExisting()
        opens("javafx.fxml", "org.jabref.jablib");
    }

    // Required for fxml loading (for localization test)
    module("org.openjfx:javafx-graphics", "javafx.graphics") {
        preserveExisting()
        exports("com.sun.javafx.scene")
        opens("com.sun.javafx.application", "org.testfx")
        opens("javafx.stage", "com.pixelduke.fxthemes")
        opens("com.sun.javafx.tk.quantum", "com.pixelduke.fxthemes")
    }

    module("org.controlsfx:controlsfx", "org.controlsfx.controls") {
        preserveExisting()
        exports("impl.org.controlsfx.skin")
        requires("javafx.graphics")
    }

    module("org.openjfx:javafx-controls", "javafx.controls") {
        preserveExisting()
        exports("com.sun.javafx.scene.control")
    }

    module("org.hamcrest:hamcrest", "org.hamcrest")

    module("org.mockito:mockito-core", "org.mockito") {
        preserveExisting()
        requires("java.prefs")
    }

    module("org.objenesis:objenesis", "org.objenesis")
    module("com.jayway.jsonpath:json-path", "json.path") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("com.fasterxml.jackson.databind")
    }
    module("net.minidev:json-smart", "json.smart")
    module("net.minidev:accessors-smart", "accessors.smart")

    module("org.openjdk.jmh:jmh-core", "jmh.core")
    module("org.openjdk.jmh:jmh-generator-asm", "jmh.generator.asm")
    module("org.openjdk.jmh:jmh-generator-bytecode", "jmh.generator.bytecode")
    module("org.openjdk.jmh:jmh-generator-reflection", "jmh.generator.reflection")
    module("org.apache.commons:commons-math3", "commons.math3")

    // We need to transform this, because the java modules plugin touches all Java paths
    // Otherwise, one gets following errormessage
    // configuration ':jablib:annotationProcessor'.

    module("javax.inject:javax.inject", "javax.inject")
    module("com.google.auto.value:auto-value-annotations", "auto.value.annotations")
    module("io.github.eisop:dataflow-errorprone", "org.checkerframework.dataflow")
    module("com.google.googlejavaformat:google-java-format", "com.google.googlejavaformat")
    module("com.google.errorprone:error_prone_core", "com.google.errorprone.core")
    module("com.google.errorprone:error_prone_check_api", "com.google.errorprone.check.api")
    module("com.google.errorprone:error_prone_annotation", "com.google.errorprone.annotation")
    module("com.google.auto:auto-common", "auto.common")
    module("com.google.auto.service:auto-service-annotations", "com.google.auto.service")
    module("com.google.protobuf:protobuf-java", "com.google.protobuf")
    module("com.github.kevinstern:software-and-algorithms", "software.and.algorithms")

    module("com.uber.nullaway:nullaway", "nullaway")
}

// Configure consistent resolution across the whole project
// https://github.com/gradlex-org/extra-java-module-info/issues/186
val consistentResolutionAttribute = Attribute.of("consistent-resolution", String::class.java)

configurations.consumable("allDependencies") {
    sourceSets.all {
        extendsFrom(
            configurations[this.implementationConfigurationName],
            configurations[this.compileOnlyConfigurationName],
            configurations[this.runtimeOnlyConfigurationName],
            configurations[this.annotationProcessorConfigurationName],
        )
    }
    attributes {
        attribute(consistentResolutionAttribute, "global")
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

configurations.getByName("mainRuntimeClasspath") {
    attributes.attribute(consistentResolutionAttribute, "global")
}

configurations.getByName("javaModulesMergeJars") {
    extendsFrom(configurations.getByName("mainRuntimeClasspath"))
}
