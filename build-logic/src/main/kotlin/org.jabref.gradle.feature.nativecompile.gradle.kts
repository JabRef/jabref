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
                "-H:+EnableAllSecurityServices"
            )
        }
    }
}
