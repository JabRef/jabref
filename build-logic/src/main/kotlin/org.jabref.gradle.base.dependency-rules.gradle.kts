import org.gradle.api.internal.artifacts.dsl.dependencies.DependenciesExtensionModule.module

plugins {
    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.jvm-dependency-conflict-resolution")
    id("org.gradlex.java-module-dependencies") // only for mappings at the moment
}

javaModuleDependencies {
    // TODO remove to translate 'requires' from 'module-info.java' to Gradle dependencies
    //      and remove 'dependencies {}' block from build.gradle files
    analyseOnly = true
    moduleNameToGA.put("jul.to.slf4j", "org.slf4j:jul-to-slf4j")
}

jvmDependencyConflicts {
    consistentResolution {
        platform(":versions")
    }
}

// Tell gradle which jar to use for which platform
// Source: https://github.com/jjohannes/java-module-system/blob/be19f6c088dca511b6d9a7487dacf0b715dbadc1/gradle/plugins/src/main/kotlin/metadata-patch.gradle.kts#L14-L22
jvmDependencyConflicts.patch {
    listOf("base", "controls", "fxml", "graphics", "swing", "web", "media").forEach { jfxModule ->
        module("org.openjfx:javafx-$jfxModule") {
            addTargetPlatformVariant("", "none", "none") // matches the empty Jars: to get better errors
            addTargetPlatformVariant("linux", OperatingSystemFamily.LINUX, MachineArchitecture.X86_64)
            addTargetPlatformVariant("linux-aarch64", OperatingSystemFamily.LINUX, MachineArchitecture.ARM64)
            addTargetPlatformVariant("mac", OperatingSystemFamily.MACOS, MachineArchitecture.X86_64)
            addTargetPlatformVariant("mac-aarch64", OperatingSystemFamily.MACOS, MachineArchitecture.ARM64)
            addTargetPlatformVariant("win", OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64)
        }
    }
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
    module("org.wiremock:wiremock") {
        // workaround for https://github.com/wiremock/wiremock/issues/2874
        addApiDependency("com.github.koppor:wiremock-slf4j-spi-shim")
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
    module("org.wiremock:wiremock") {
        removeDependency("net.sf.jopt-simple:jopt-simple")
    }
    module("org.testfx:testfx-core") {
        removeDependency("org.osgi:org.osgi.core")
    }
    module("org.glassfish.jersey.containers:jersey-container-servlet") {
    }
    module("org.glassfish.jersey.containers:jersey-container-servlet-core") {
    }
    module("org.xmlunit:xmlunit-legacy") {
        removeDependency("junit:junit")
    }
}

extraJavaModuleInfo {
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

    module("com.googlecode.plist:dd-plist", "dd.plist")
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
    module("com.squareup.okhttp3:okhttp-jvm", "okhttp3")
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
        mergeJar("dev.langchain4j:langchain4j-http-client")
        mergeJar("dev.langchain4j:langchain4j-http-client-jdk")
        mergeJar("dev.langchain4j:langchain4j-hugging-face")
        mergeJar("dev.langchain4j:langchain4j-mistral-ai")
        mergeJar("dev.langchain4j:langchain4j-open-ai")
        mergeJar("dev.langchain4j:langchain4j-google-ai-gemini")
        requires("jtokkit")
        requires("java.net.http")
        uses("dev.langchain4j.http.client.HttpClientBuilderFactory")
        exportAllPackages()
        requireAllDefinedDependencies()
        patchRealModule()
    }
    module("dev.langchain4j:langchain4j-google-ai-gemini", "langchain4j.google.ai.gemini")
    module("dev.langchain4j:langchain4j-http-client", "langchain4j.http.client")
    module("dev.langchain4j:langchain4j-http-client-jdk", "langchain4j.http.client.jdk")
    module("dev.langchain4j:langchain4j-hugging-face", "langchain4j.hugging.face")
    module("dev.langchain4j:langchain4j-mistral-ai", "langchain4j.mistral.ai")
    module("dev.langchain4j:langchain4j-open-ai", "langchain4j.open.ai")
    module("eu.lestard:doc-annotations", "doc.annotations")
    module("info.debatty:java-string-similarity", "java.string.similarity")
    module("io.github.classgraph:classgraph", "io.github.classgraph") {
        overrideModuleName()
        exportAllPackages()
    }
    module("io.github.adr:e-adr", "io.github.adr") {
        patchRealModule()
        exportAllPackages()
    }
    module("io.github.java-diff-utils:java-diff-utils", "io.github.javadiffutils")
    module("io.zonky.test.postgres:embedded-postgres-binaries-darwin-amd64", "embedded.postgres.binaries.darwin.amd64")
    module("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8", "embedded.postgres.binaries.darwin.arm64v8")
    module("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64", "embedded.postgres.binaries.linux.amd64")
    module("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64-alpine", "embedded.postgres.binaries.linux.amd64.alpine")
    module("io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8", "embedded.postgres.binaries.linux.arm64v8")
    module("io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64", "embedded.postgres.binaries.windows.amd64")
    module("net.harawata:appdirs", "net.harawata.appdirs")
    module("net.java.dev.jna:jna", "com.sun.jna") {
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
    module("org.jbibtex:jbibtex", "jbibtex") {
        exportAllPackages()
    }
    module("org.scala-lang:scala-library", "scala.library")
    module("pt.davidafsilva.apple:jkeychain", "jkeychain")

    module("org.testfx:testfx-core", "org.testfx") {
        patchRealModule()
        exportAllPackages()
        // Content based on https://github.com/TestFX/TestFX/commit/bf4a08aa82c008fdd3c296aaafee1d222f3824cb
        requires("java.desktop")
        requiresTransitive("javafx.controls")
        requiresTransitive("org.hamcrest")
    }
    module("org.testfx:testfx-junit5", "org.testfx.junit5") {
        patchRealModule()
        exportAllPackages()
        requires("org.junit.jupiter.api")
        requiresTransitive("org.testfx")
    }

    module("commons-fileupload:commons-fileupload", "commons.fileupload")

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
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.debug", "org.eclipse.lsp4j.debug") {
        exportAllPackages()
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.generator", "org.eclipse.lsp4j.generator") {
        exportAllPackages()
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc", "org.eclipse.lsp4j.jsonrpc") {
        exportAllPackages()
        requires("com.google.gson")
        requires("java.logging")
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc.debug", "org.eclipse.lsp4j.jsonrpc.debug") {
        exportAllPackages()
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.websocket", "org.eclipse.lsp4j.websocket") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("org.eclipse.lsp4j:org.eclipse.lsp4j.websocket.jakarta", "org.eclipse.lsp4j.websocket.jakarta") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("jakarta.websocket:jakarta.websocket-api", "jakarta.websocket") {
        overrideModuleName()
        exportAllPackages()
    }
    module("javax.websocket:javax.websocket-api", "javax.websocket.api") {
        exportAllPackages()
    }
    module("org.eclipse.xtend:org.eclipse.xtend", "xtend") {
        exportAllPackages()
    }
    module("org.eclipse.xtend:org.eclipse.xtend.lib", "xtend.lib") {
        overrideModuleName()
        exportAllPackages()
    }
    module("org.eclipse.xtend:org.eclipse.xtend.lib.macro", "xtend.lib.macro") {
        overrideModuleName()
        exportAllPackages()
    }
    module("org.eclipse.xtext:org.eclipse.xtext.xbase.lib", "xtext.xbase.lib") {
        overrideModuleName()
        exportAllPackages()
    }

    module("com.tngtech.archunit:archunit-junit5-api", "com.tngtech.archunit.junit5.api") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("com.tngtech.archunit:archunit-junit5-engine", "com.tngtech.archunit.junit5.engine") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("com.tngtech.archunit:archunit-junit5-engine-api", "com.tngtech.archunit.junit5.engineapi") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("com.tngtech.archunit:archunit", "com.tngtech.archunit") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        uses("com.tngtech.archunit.lang.extension.ArchUnitExtension")
    }

    module("org.glassfish.hk2.external:aopalliance-repackaged", "org.aopalliance")
    module("org.glassfish.jersey.core:jersey-server", "jersey.server") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        requires("jakarta.xml.bind")
        requires("jersey.hk2")
    }
    // module("org.glassfish.jersey.containers:jersey-container-servlet", "jersey.servlet")
    module("org.glassfish.jersey.inject:jersey-hk2", "jersey.hk2") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("org.glassfish.jersey.core:jersey-client", "jersey.client") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("org.glassfish.jersey.core:jersey-common", "jersey.common") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        requires("java.xml")
    }
    module("org.glassfish.jersey.containers:jersey-container-grizzly2-http", "jersey.container.grizzly2.http") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
        requires("org.glassfish.grizzly")
        requires("org.glassfish.grizzly.http")
    }
    module("org.glassfish.jersey.test-framework:jersey-test-framework-core", "jersey.test.framework.core") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("java.logging")
    }
    module("org.glassfish.jersey.containers:jersey-container-grizzly2-servlet", "jersey.container.grizzly2.servlet") {
        // requires("jersey.servlet")
    }
    module("org.glassfish.jersey.containers:jersey-container-servlet", "jersey.container.servlet") {
        exportAllPackages()
        // requireAllDefinedDependencies()
        requires("jersey.container.servlet.core")
        requires("jakarta.servlet.api")
    }
    module("jakarta.servlet:jakarta.servlet-api", "jakarta.servlet.api") {
        patchRealModule()
        exportAllPackages()
    }
    module("org.glassfish.jersey.containers:jersey-container-servlet-core", "jersey.container.servlet.core") {
        exportAllPackages()
        requires("jakarta.servlet.api")
    }
    module("org.glassfish.jersey.media:jersey-media-jaxb", "jersey.media.jaxb") {
        requireAllDefinedDependencies()
        requires("java.logging")
        requires("java.xml")
        requires("jakarta.xml.bind")
    }
    module("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2", "jersey.test.framework.provider.grizzly2") {
        requireAllDefinedDependencies()
        requires("java.logging")
    }
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

    module("com.github.sialcasa.mvvmFX:mvvmfx-validation", "de.saxsys.mvvmfx.validation") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requiresTransitive("javafx.base")
        requiresTransitive("javafx.controls")
        requiresTransitive("org.controlsfx.controls")
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
        patchRealModule()
        // jabgui requires at least "javafx.collections"
        exportAllPackages()
    }

    // required for testing of jablib
    module("org.openjfx:javafx-fxml", "javafx.fxml") {
        patchRealModule()
        exportAllPackages()

        requiresTransitive("javafx.graphics")
        requiresTransitive("java.desktop")
    }

    // Required for fxml loading (for localization test)
    module("org.openjfx:javafx-graphics", "javafx.graphics") {
        patchRealModule()
        exportAllPackages() // required for testfx

        requiresTransitive("javafx.base")
        requiresTransitive("java.desktop")
        requiresTransitive("jdk.unsupported")
    }

    module("org.openjfx:jdk-jsobject", "jdk.jsobjectEmpty") {}

    module("org.controlsfx:controlsfx", "org.controlsfx.controls") {
        patchRealModule()

        exports("impl.org.controlsfx.skin")
        exports("org.controlsfx.control")
        exports("org.controlsfx.control.action")
        exports("org.controlsfx.control.decoration")
        exports("org.controlsfx.control.table")
        exports("org.controlsfx.control.textfield")
        exports("org.controlsfx.dialog")
        exports("org.controlsfx.validation")
        exports("org.controlsfx.validation.decoration")

        requires("javafx.controls")
        requiresTransitive("javafx.graphics")
    }

    module("org.openjfx:javafx-controls", "javafx.controls") {
        patchRealModule()

        requiresTransitive("javafx.base");
        requiresTransitive("javafx.graphics");

        exports("javafx.scene.chart")
        exports("javafx.scene.control")
        exports("javafx.scene.control.cell")
        exports("javafx.scene.control.skin")

        // PATCH REASON:
        exports("com.sun.javafx.scene.control")
    }

    module("org.hamcrest:hamcrest", "org.hamcrest") {
        exportAllPackages()
    }

    module("org.mockito:mockito-core", "org.mockito") {
        preserveExisting()
        requires("java.prefs")
    }

    // Workaround for https://github.com/wiremock/wiremock/issues/2149
    module("org.wiremock:wiremock", "wiremock") {
        exportAllPackages()

        requires("org.apache.httpcomponents.client5.httpclient5")
        requires("com.fasterxml.jackson.core")
        requires("com.fasterxml.jackson.databind")
        requires("com.fasterxml.jackson.datatype.jsr310")
        requires("com.google.common")
        requires("commons.fileupload")
        requires("java.xml")
        requires("json.path")
        requires("org.custommonkey.xmlunit")
        requires("org.eclipse.jetty.server")
        requires("org.eclipse.jetty.servlet")
        requires("org.eclipse.jetty.servlets")
        requires("org.eclipse.jetty.webapp")
        requires("org.eclipse.jetty.proxy")
        requires("org.eclipse.jetty.http2.server")
        requires("org.eclipse.jetty.alpn.server")
        requires("org.eclipse.jetty.alpn.java.server")
        requires("org.eclipse.jetty.alpn.java.client")
        requires("org.eclipse.jetty.alpn.client")
        requires("org.slf4j")
        requires("org.xmlunit")
        requires("wiremock.slf4j.spi.shim")

        uses("com.github.tomakehurst.wiremock.extension.Extension")

        // workaround for https://github.com/wiremock/wiremock/issues/2874
        mergeJar("com.github.jknack:handlebars")
        mergeJar("com.github.jknack:handlebars-helpers")

        // Required to provide package "wiremock.org.slf4j.helpers"
        mergeJar("com.github.koppor:wiremock-slf4j-shim")
    }
    module("com.github.koppor:wiremock-slf4j-shim", "wiremock.slf4j.shim") {
        patchRealModule()
        exportAllPackages()
    }
    module("com.github.koppor:wiremock-slf4j-spi-shim", "wiremock.slf4j.spi.shim") {
        patchRealModule()
        exportAllPackages()
    }
    module("org.objenesis:objenesis", "org.objenesis") {
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module("com.jayway.jsonpath:json-path", "json.path") {
        exportAllPackages()
        requireAllDefinedDependencies()
        requires("com.fasterxml.jackson.databind")
    }
    module("net.minidev:json-smart", "json.smart")
    module("net.minidev:accessors-smart", "accessors.smart")
    module("org.ow2.asm:asm", "org.objectweb.asm") {
        preserveExisting()
    }

    module("org.openjdk.jmh:jmh-core", "jmh.core")
    module("org.openjdk.jmh:jmh-generator-asm", "jmh.generator.asm")
    module("org.openjdk.jmh:jmh-generator-bytecode", "jmh.generator.bytecode")
    module("org.openjdk.jmh:jmh-generator-reflection", "jmh.generator.reflection")
    module("org.apache.commons:commons-math3", "commons.math3")
}
