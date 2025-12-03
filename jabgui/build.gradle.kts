plugins {
    id("org.jabref.gradle.module")
    id("application")

    // Do not activate; causes issues with the modularity plugin (no tests found etc)
    // id("com.redock.classpathtofile") version "0.1.0"
}

group = "org.jabref"
version = project.findProperty("projVersion") ?: "100.0.0"

// See https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    implementation(project(":jablib"))
    // Following already provided by jablib
    // implementation("org.openjfx:javafx-base")
    // implementation("org.openjfx:javafx-controls")
    // implementation("org.openjfx:javafx-fxml")
    // implementation("org.openjfx:javafx-graphics")
    implementation("com.google.code.gson:gson:2.10.1")
   


    implementation(project(":jabls"))
    implementation(project(":jabsrv"))

    implementation("org.openjfx:javafx-swing")
    implementation("org.openjfx:javafx-web")

    implementation("com.pixelduke:fxthemes")

    // From JavaFX25 onwards
    implementation("org.openjfx:jdk-jsobject")

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

    implementation("net.java.dev.jna:jna-jpms")
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
    mockitoAgent("org.mockito:mockito-core:5.18.0") { isTransitive = false }
    testImplementation("net.bytebuddy:byte-buddy")

    testImplementation("org.hamcrest:hamcrest")

    testImplementation("com.github.javaparser:javaparser-symbol-solver-core")
    testImplementation("org.ow2.asm:asm")

    testImplementation("com.tngtech.archunit:archunit")
    testImplementation("com.tngtech.archunit:archunit-junit5-api")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine")

}

application {
    mainClass.set("org.jabref.Launcher")
    mainModule.set("org.jabref")

    application.applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core,jkeychain",
        "--add-opens", "java.base/java.nio=org.apache.pdfbox.io",
        // https://github.com/uncomplicate/neanderthal/issues/55
        "--add-opens", "java.base/jdk.internal.ref=org.apache.pdfbox.io",
        "--add-modules", "jdk.incubator.vector",

        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseCompactObjectHeaders",
        "-XX:+UseZGC",
        "-XX:+ZUncommit",
        "-XX:+UseStringDeduplication"
    )
}

tasks.named<JavaExec>("run") {
    // "assert" statements in the code should activated when running using gradle
    enableAssertions = true
}

// Below should eventually replace the 'jlink {}' and doLast-copy configurations above
javaModulePackaging {
    applicationName = "JabRef"
    jpackageResources = layout.projectDirectory.dir("buildres")
    verbose = true
    addModules.add("jdk.incubator.vector")
    jlinkOptions.addAll("--generate-cds-archive")
    targetsWithOs("windows") {
        options.addAll(
            "--win-upgrade-uuid", "d636b4ee-6f10-451e-bf57-c89656780e36",
            "--win-dir-chooser",
            "--win-shortcut",
            "--win-menu",
            "--win-menu-group", "JabRef",
            "--license-file", "$projectDir/buildres/LICENSE_with_Privacy.md",
            "--file-associations", "$projectDir/buildres/windows/bibtexAssociations.properties"
        )
        targetResources.from(layout.projectDirectory.dir("buildres/windows").asFileTree.matching {
            include("jabref-firefox.json")
            include("jabref-chrome.json")
            include("JabRefHost.bat")
            include("JabRefHost.ps1")
        })
    }
    targetsWithOs("linux") {
        options.addAll(
            "--linux-menu-group", "Office;",
            "--linux-rpm-license-type", "MIT",
            "--description", "JabRef is an open source bibliography reference manager. Simplifies reference management and literature organization for academic researchers by leveraging BibTeX, native file format for LaTeX.",
            "--icon", "$projectDir/src/main/resources/icons/JabRef-linux-icon-64.png",
            "--linux-shortcut",
            "--file-associations", "$projectDir/buildres/linux/bibtexAssociations.properties"
        )
        targetResources.from(layout.projectDirectory.dir("buildres/linux").asFileTree.matching {
            include("native-messaging-host/**")
            include("jabrefHost.py")
        })
    }
    targetsWithOs("macos") {
        options.addAll(
            "--icon", "$projectDir/src/main/resources/icons/jabref.icns",
            "--mac-package-identifier", "JabRef",
            "--mac-package-name", "JabRef",
            "--file-associations", "$projectDir/buildres/macos/bibtexAssociations.properties",
        )
        if (providers.environmentVariable("OSXCERT").orNull?.isNotBlank() ?: false) {
            options.addAll(
                "--mac-sign",
                "--mac-signing-key-user-name", "JabRef e.V. (6792V39SK3)",
                "--mac-package-signing-prefix", "org.jabref",
            )
        }
        targetResources.from(layout.projectDirectory.dir("buildres/macos").asFileTree.matching {
            include("Resources/**")
        })
    }
}

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("org.jabref.testsupport")

    // Not sure why there is no dependency for jabgui normal running for this dependency
    // requires.add("javafx.graphics")

    requires.add("com.github.javaparser.core")
    requires.add("org.junit.jupiter.api")
    requires.add("org.junit.jupiter.params")
    requires.add("org.mockito")

    requires.add("org.testfx")
    requires.add("org.testfx.junit5")

    requires.add("com.tngtech.archunit")
    requires.add("com.tngtech.archunit.junit5.api")
}

tasks.test {
    jvmArgs = listOf(
        "-javaagent:${mockitoAgent.asPath}",

        // Source: https://github.com/TestFX/TestFX/issues/638#issuecomment-433744765
        "--add-opens", "javafx.graphics/com.sun.javafx.application=org.testfx",

        "--add-opens", "java.base/jdk.internal.ref=org.apache.pdfbox.io",
        "--add-opens", "java.base/java.nio=org.apache.pdfbox.io",
        "--enable-native-access=javafx.graphics,javafx.web,com.sun.jna"

        // "--add-reads", "org.mockito=java.prefs",
        // "--add-reads", "org.jabref=wiremock"
    )
}
