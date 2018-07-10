package org.jabref.build

class Dependencies {
    
    static def libraries = [
            libreOffice: [
                    "org.libreoffice:juh:5.4.2",
                    "org.libreoffice:jurt:5.4.2",
                    "org.libreoffice:ridl:5.4.2",
                    "org.libreoffice:unoil:5.4.2",
            ],

            loggingApi: "org.slf4j:slf4j-api:1.8.0-beta2",

            logging: [
                    "org.apache.logging.log4j:log4j-slf4j-impl:2.11.0",
                    "org.apache.logging.log4j:log4j-jcl:2.11.0",
                    "org.apache.logging.log4j:log4j-api:2.11.0",
                    "org.apache.logging.log4j:log4j-core:2.11.0",
            ],

            pdfbox: [
                    "org.apache.pdfbox:pdfbox:2.0.10",
                    "org.apache.pdfbox:fontbox:2.0.10",
                    "org.apache.pdfbox:xmpbox:2.0.10",
            ],

            antlr3: "org.antlr:antlr:3.5.2",
            antlr3runtime: "org.antlr:antlr-runtime:3.5.2",
            antlr4: "org.antlr:antlr4:4.7.1",
            antlr4runtime: "org.antlr:antlr4-runtime:4.7.1",

            guava: "com.google.guava:guava:25.1-jre",

            applicationInsights: [
                    "com.microsoft.azure:applicationinsights-core:2.1.1",
                    "com.microsoft.azure:applicationinsights-logging-log4j2:2.1.1",
            ],

            bouncyCastle: "org.bouncycastle:bcprov-jdk15on:1.59",

            latex2unicode: "com.github.tomtung:latex2unicode_2.12:0.2.2",

            citationStyles: [
                    "org.citationstyles:styles:1.0.1-SNAPSHOT",
                    "org.citationstyles:locales:1.0.1-SNAPSHOT",
            ],

            citeproc: "de.undercouch:citeproc-java:1.0.1",

            commonsCli: "commons-cli:commons-cli:1.4",

            jGoodiesCommon: "com.jgoodies:jgoodies-common:1.8.1",
            jGoodiesForms: "com.jgoodies:jgoodies-forms:1.9.0",

            diffUtils: "com.github.bkromhout:java-diff-utils:2.1.1",
            stringSimilarity: "info.debatty:java-string-similarity:1.1.0",

            mySqlConnector: "mysql:mysql-connector-java:5.1.46",
            postgres: "org.postgresql:postgresql:42.2.2",

            glazedLists: "net.java.dev.glazedlists:glazedlists_java15:1.9.1",

            fontawesome: "de.jensd:fontawesomefx-materialdesignfont:1.7.22-4",
            mvvmfx     : [
                    "de.saxsys:mvvmfx-validation:1.7.0",
                    "de.saxsys:mvvmfx:1.7.0",
            ],
            easybind   : "org.fxmisc.easybind:easybind:1.0.3",
            flowless   : "org.fxmisc.flowless:flowless:0.6.1",
            richtextfx : "org.fxmisc.richtext:richtextfx:0.9.0",
            dndtabpane : "com.sibvisions.external.jvxfx:dndtabpane:0.1",
            javaInject : "javax.inject:javax.inject:1",
            controlsFx: "org.controlsfx:controlsfx:8.40.15-SNAPSHOT",

            jsoup: "org.jsoup:jsoup:1.11.3",
            unirest: "com.mashape.unirest:unirest-java:1.4.9",

            junit: [
                    "org.junit.jupiter:junit-jupiter-api:5.2.0",
                    "org.junit.jupiter:junit-jupiter-params:5.2.0",
            ],

            junitRuntime: [
                    "org.junit.jupiter:junit-jupiter-engine:5.2.0",
                    "org.junit.vintage:junit-vintage-engine:5.2.0",
            ],

            junitPlatformLauncher: "org.junit.platform:junit-platform-launcher:1.2.0",
            junitPioneer: "org.junit-pioneer:junit-pioneer:0.1-SNAPSHOT",

            testLogging: [
                    "org.apache.logging.log4j:log4j-core:2.11.0",
                    "org.apache.logging.log4j:log4j-jul:2.11.0",
            ],

            mockito: "org.mockito:mockito-core:2.19.0",
            wiremock: "com.github.tomakehurst:wiremock:2.18.0",
            assertjSwing: "org.assertj:assertj-swing-junit:3.8.0",
            reflections: "org.reflections:reflections:0.9.11",
            xmlUnit: [
                    "org.xmlunit:xmlunit-core:2.6.0",
                    "org.xmlunit:xmlunit-matchers:2.6.0",
            ],
            archUnit: "com.tngtech.archunit:archunit-junit:0.8.2",
            testFx: [
                    "org.testfx:testfx-core:4.0.+",
                    "org.testfx:testfx-junit5:4.0.+",
            ],

            checkstyle: "com.puppycrawl.tools:checkstyle:8.10.1",
            xjc: "com.sun.xml.bind:jaxb-xjc:2.2.4-1",
    ]

}
