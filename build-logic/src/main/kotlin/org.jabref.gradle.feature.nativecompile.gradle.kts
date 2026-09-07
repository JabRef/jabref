plugins {
    id("org.graalvm.buildtools.native")
}

graalvmNative {
    metadataRepository {
        enabled = true
    }
    binaries {
        named("main") {
            buildArgs.addAll(
                "--no-fallback",
                "-march=compatibility",
                "-H:+ReportExceptionStackTraces",
                "-H:IncludeLocales=en",
                "--enable-all-security-services",
                "--enable-native-access=ALL-UNNAMED",
                "--enable-url-protocols=http,https",
                // Only the default (file:) NIO provider is needed. Otherwise the builder snapshots every
                // FileSystemProvider found on the classpath into the image heap, which fails for sshd's
                // rooted:/sftp: providers (their instances hold loggers that cannot be initialized at build time).
                "-H:-AddAllFileSystemProviders"
            )
        }
    }
}
