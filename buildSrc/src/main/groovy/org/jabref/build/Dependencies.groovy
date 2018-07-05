package org.jabref.build

class Dependencies {

    static def versions = [
            libreOffice: "5.4.2",

            pdfbox: "2.0.10",

            slf4j: "1.8.0-beta2",
            log4j: "2.11.0",

            antlr3: "3.5.2",
            antlr4: "4.7.1",

            guava: "25.1-jre",

            applicationInsights: "2.1.1",

            bouncyCastle: "1.59",

            latex2unicode: "0.2.2",

            citationStyles: "1.0.1-SNAPSHOT",
            citeproc: "1.0.1",

            commonsCli: "1.4",

            jGoodiesCommon: "1.8.1",
            jGoodiesForms: "1.9.0",

            diffUtils: "2.1.1",
            stringSimilarity: "1.1.0",

            mySqlConnector: "5.1.46",
            postgres: "42.2.2",

            glazedLists: "1.9.1",

            fontawesome: "1.7.22-4",
            mvvmfx: "1.7.0",
            easybind: "1.0.3",
            flowless: "0.6.1",
            richtextfx: "0.9.0",
            dndtabpane: "0.1",
            javaInject: "1",
            controlsFx: "8.40.15-SNAPSHOT",

            jsoup: "1.11.3",
            unirest: "1.4.9",

            junit: "5.2.0",
            junitPlatformLauncher: "1.2.0",
            junitPioneer: "0.1-SNAPSHOT",

            mockito: "2.19.0",
            wiremock: "2.18.0",
            assertjSwing: "3.8.0",
            reflections: "0.9.11",
            xmlUnit: "2.6.0",
            archUnit: "0.8.2",
            testFx: "4.0.+",

            checkstyle: "8.10.1",
            xjc: "2.2.4-1",
    ]

    static def libraries = [
            libreOffice: [
                    "org.libreoffice:juh:${versions.libreOffice}",
                    "org.libreoffice:jurt:${versions.libreOffice}",
                    "org.libreoffice:ridl:${versions.libreOffice}",
                    "org.libreoffice:unoil:${versions.libreOffice}",
            ],

            loggingApi: "org.slf4j:slf4j-api:${versions.slf4j}",

            logging: [
                    "org.apache.logging.log4j:log4j-slf4j-impl:${versions.log4j}",
                    "org.apache.logging.log4j:log4j-jcl:${versions.log4j}",
                    "org.apache.logging.log4j:log4j-api:${versions.log4j}",
                    "org.apache.logging.log4j:log4j-core:${versions.log4j}",
            ],

            pdfbox: [
                    "org.apache.pdfbox:pdfbox:${versions.pdfbox}",
                    "org.apache.pdfbox:fontbox:${versions.pdfbox}",
                    "org.apache.pdfbox:xmpbox:${versions.pdfbox}",
            ],

            antlr3: "org.antlr:antlr:${versions.antlr3}",
            antlr3runtime: "org.antlr:antlr-runtime:${versions.antlr3}",
            antlr4: "org.antlr:antlr4:${versions.antlr4}",
            antlr4runtime: "org.antlr:antlr4-runtime:${versions.antlr4}",

            guava: "com.google.guava:guava:${versions.guava}",

            applicationInsights: [
                    "com.microsoft.azure:applicationinsights-core:${versions.applicationInsights}",
                    "com.microsoft.azure:applicationinsights-logging-log4j2:${versions.applicationInsights}",
            ],

            bouncyCastle: "org.bouncycastle:bcprov-jdk15on:${versions.bouncyCastle}",

            latex2unicode: "com.github.tomtung:latex2unicode_2.12:${versions.latex2unicode}",

            citationStyles: [
                    "org.citationstyles:styles:${versions.citationStyles}",
                    "org.citationstyles:locales:${versions.citationStyles}",
            ],

            citeproc: "de.undercouch:citeproc-java:${versions.citeproc}",

            commonsCli: "commons-cli:commons-cli:${versions.commonsCli}",

            jGoodiesCommon: "com.jgoodies:jgoodies-common:${versions.jGoodiesCommon}",
            jGoodiesForms: "com.jgoodies:jgoodies-forms:${versions.jGoodiesForms}",

            diffUtils: "com.github.bkromhout:java-diff-utils:${versions.diffUtils}",
            stringSimilarity: "info.debatty:java-string-similarity:${versions.stringSimilarity}",

            mySqlConnector: "mysql:mysql-connector-java:${versions.mySqlConnector}",
            postgres: "org.postgresql:postgresql:${versions.postgres}",

            glazedLists: "net.java.dev.glazedlists:glazedlists_java15:${versions.glazedLists}",

            fontawesome: "de.jensd:fontawesomefx-materialdesignfont:${versions.fontawesome}",
            mvvmfx     : [
                    "de.saxsys:mvvmfx-validation:${versions.mvvmfx}",
                    "de.saxsys:mvvmfx:${versions.mvvmfx}",
            ],
            easybind   : "org.fxmisc.easybind:easybind:${versions.easybind}",
            flowless   : "org.fxmisc.flowless:flowless:${versions.flowless}",
            richtextfx : "org.fxmisc.richtext:richtextfx:${versions.richtextfx}",
            dndtabpane : "com.sibvisions.external.jvxfx:dndtabpane:${versions.dndtabpane}",
            javaInject : "javax.inject:javax.inject:${versions.javaInject}",
            controlsFx: "org.controlsfx:controlsfx:${versions.controlsFx}",

            jsoup: "org.jsoup:jsoup:${versions.jsoup}",
            unirest: "com.mashape.unirest:unirest-java:${versions.unirest}",

            junit: [
                    "org.junit.jupiter:junit-jupiter-api:${versions.junit}",
                    "org.junit.jupiter:junit-jupiter-params:${versions.junit}",
            ],

            junitRuntime: [
                    "org.junit.jupiter:junit-jupiter-engine:${versions.junit}",
                    "org.junit.vintage:junit-vintage-engine:${versions.junit}",
            ],

            junitPlatformLauncher: "org.junit.platform:junit-platform-launcher:${versions.junitPlatformLauncher}",
            junitPioneer: "org.junit-pioneer:junit-pioneer:${versions.junitPioneer}",

            testLogging: [
                    "org.apache.logging.log4j:log4j-core:${versions.log4j}",
                    "org.apache.logging.log4j:log4j-jul:${versions.log4j}",
            ],

            mockito: "org.mockito:mockito-core:${versions.mockito}",
            wiremock: "com.github.tomakehurst:wiremock:${versions.wiremock}",
            assertjSwing: "org.assertj:assertj-swing-junit:${versions.assertjSwing}",
            reflections: "org.reflections:reflections:${versions.reflections}",
            xmlUnit: [
                    "org.xmlunit:xmlunit-core:${versions.xmlUnit}",
                    "org.xmlunit:xmlunit-matchers:${versions.xmlUnit}",
            ],
            archUnit: "com.tngtech.archunit:archunit-junit:${versions.archUnit}",
            testFx: [
                    "org.testfx:testfx-core:${versions.testFx}",
                    "org.testfx:testfx-junit5:${versions.testFx}",
            ],

            checkstyle: "com.puppycrawl.tools:checkstyle:${versions.checkstyle}",
            xjc: "com.sun.xml.bind:jaxb-xjc:${versions.xjc}",
    ]

}
