package org.jabref.gradle

import java.util.Locale

data class EmbeddedPostgresBinary(
    val moduleName: String,
    val dependency: String
)

object EmbeddedPostgresBinaries {
    val linuxAmd64 = EmbeddedPostgresBinary(
        "embedded.postgres.binaries.linux.amd64",
        "io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64"
    )
    val linuxArm64 = EmbeddedPostgresBinary(
        "embedded.postgres.binaries.linux.arm64v8",
        "io.zonky.test.postgres:embedded-postgres-binaries-linux-arm64v8"
    )
    val macosAmd64 = EmbeddedPostgresBinary(
        "embedded.postgres.binaries.darwin.amd64",
        "io.zonky.test.postgres:embedded-postgres-binaries-darwin-amd64"
    )
    val macosArm64 = EmbeddedPostgresBinary(
        "embedded.postgres.binaries.darwin.arm64v8",
        "io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8"
    )
    val windowsAmd64 = EmbeddedPostgresBinary(
        "embedded.postgres.binaries.windows.amd64",
        "io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64"
    )

    fun forHost(): EmbeddedPostgresBinary? {
        val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
        val architectureName = System.getProperty("os.arch").lowercase(Locale.ROOT)
        val isArm64 = architectureName in setOf("aarch64", "arm64")

        return when {
            osName.contains("linux") && isArm64 -> linuxArm64
            osName.contains("linux") -> linuxAmd64
            osName.contains("mac") && isArm64 -> macosArm64
            osName.contains("mac") -> macosAmd64
            osName.contains("windows") -> windowsAmd64
            else -> null
        }
    }
}
