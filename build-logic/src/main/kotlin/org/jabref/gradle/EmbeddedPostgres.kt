package org.jabref.gradle

import java.util.Locale

data class EmbeddedPostgresBinary(
    val moduleName: String
) {
    val artifactName = moduleName
        .removePrefix("embedded.postgres.binaries.")
        .replace('.', '-')
        .let { "embedded-postgres-binaries-$it" }
}

object EmbeddedPostgresBinaries {
    val linuxAmd64 = EmbeddedPostgresBinary("embedded.postgres.binaries.linux.amd64")
    val linuxArm64 = EmbeddedPostgresBinary("embedded.postgres.binaries.linux.arm64v8")
    val macosAmd64 = EmbeddedPostgresBinary("embedded.postgres.binaries.darwin.amd64")
    val macosArm64 = EmbeddedPostgresBinary("embedded.postgres.binaries.darwin.arm64v8")
    val windowsAmd64 = EmbeddedPostgresBinary("embedded.postgres.binaries.windows.amd64")

    fun forHost(osName: String, architectureName: String): EmbeddedPostgresBinary? {
        val normalizedOsName = osName.lowercase(Locale.ROOT)
        val normalizedArchitectureName = architectureName.lowercase(Locale.ROOT)
        val isArm64 = normalizedArchitectureName in setOf("aarch64", "arm64")

        return when {
            normalizedOsName.contains("linux") && isArm64 -> linuxArm64
            normalizedOsName.contains("linux") -> linuxAmd64
            normalizedOsName.contains("mac") && isArm64 -> macosArm64
            normalizedOsName.contains("mac") -> macosAmd64
            normalizedOsName.contains("windows") -> windowsAmd64
            else -> null
        }
    }
}
