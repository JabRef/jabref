plugins {
    id("org.graalvm.buildtools.native")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
                "-H:IncludeLocales=en",
                "--enable-all-security-services",
                "--enable-native-access=ALL-UNNAMED",
                "--enable-url-protocols=https"
            )
        }
    }
}
