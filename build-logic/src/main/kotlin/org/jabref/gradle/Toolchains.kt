package org.jabref.gradle

import org.gradle.api.Project
import org.gradle.jvm.toolchain.JvmVendorSpec

/**
 * `true` when the build should consume the JavaFX that ships inside a BellSoft Liberica "Full" JDK
 * instead of resolving `org.openjfx:*` from Maven.
 *
 * Enable with `-PuseLibericaJdkFull` (the bare flag counts as on); pass `-PuseLibericaJdkFull=false`
 * (e.g. in `gradle.properties`) to force it off.
 *
 * Consumed by:
 * - org.jabref.gradle.feature.compile (toolchain vendor)
 * - org.jabref.gradle.base.dependency-rules (drop JavaFX module-name -> Maven GA mappings)
 * - jabgui/build.gradle.kts (runtime --add-opens/--add-exports)
 */
val Project.useLibericaJdkFull: Boolean
    get() = providers.gradleProperty("useLibericaJdkFull").map { parseUseLibericaJdkFull(it) }.getOrElse(false)

/**
 * Parses the value of `-PuseLibericaJdkFull`.
 *
 * - blank (the bare `-PuseLibericaJdkFull` flag) -> `true`
 * - `true` (case-insensitive) -> `true`
 * - `false` (case-insensitive) -> `false`
 * - anything else -> error, to avoid silently enabling the mode on a typo like `False`, `0`, `no`.
 */
private fun parseUseLibericaJdkFull(value: String): Boolean =
    when (value.trim().lowercase()) {
        "", "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException(
            "Invalid value '$value' for property 'useLibericaJdkFull'. " +
                "Use the bare flag '-PuseLibericaJdkFull', or '-PuseLibericaJdkFull=true' / '-PuseLibericaJdkFull=false'."
        )
    }

/**
 * The Java language version the toolchain targets. Default 25 (keep in sync with the comment block in
 * org.jabref.gradle.feature.compile). Override with `-PjavaVersion=26` (e.g. to test an EA JDK).
 */
val Project.javaVersion: Int
    get() = providers.gradleProperty("javaVersion").map { value ->
        value.trim().toIntOrNull()
            ?: throw IllegalArgumentException("Invalid value '$value' for property 'javaVersion'. Expected an integer like '25' or '26'.")
    }.getOrElse(25)

/**
 * The JDK vendor the toolchain resolves. Defaults to BELLSOFT when `-PuseLibericaJdkFull` is set
 * (it bundles JavaFX), otherwise AMAZON (Corretto, JavaFX comes from Maven).
 *
 * Override with `-Pjdk=<name>`, e.g. `-Pjdk=openj9` to play with Eclipse OpenJ9 (IBM Semeru). Note that
 * non-Liberica-Full vendors do NOT bundle JavaFX, so do not combine them with `-PuseLibericaJdkFull`.
 */
val Project.jdkVendor: JvmVendorSpec
    get() = providers.gradleProperty("jdk").map { parseJdkVendor(it) }
        .getOrElse(if (useLibericaJdkFull) JvmVendorSpec.BELLSOFT else JvmVendorSpec.AMAZON)

/**
 * Maps a friendly `-Pjdk` name to a Gradle [JvmVendorSpec]. Eclipse OpenJ9 ships as IBM Semeru,
 * hence `openj9`/`semeru` -> IBM.
 */
private fun parseJdkVendor(value: String): JvmVendorSpec =
    // Accepts both friendly names (used on the command line, e.g. -Pjdk=openj9) and the raw
    // JvmVendorSpec enum names that binaries.yml passes through verbatim (e.g. -Pjdk=BELLSOFT).
    when (value.trim().lowercase()) {
        "corretto", "amazon" -> JvmVendorSpec.AMAZON
        "liberica", "bellsoft" -> JvmVendorSpec.BELLSOFT
        "temurin", "adoptium" -> JvmVendorSpec.ADOPTIUM
        "adoptopenjdk" -> JvmVendorSpec.ADOPTOPENJDK
        "oracle" -> JvmVendorSpec.ORACLE
        "openj9", "semeru", "ibm" -> JvmVendorSpec.IBM
        "graalvm", "graal", "graal_vm" -> JvmVendorSpec.GRAAL_VM
        "microsoft" -> JvmVendorSpec.MICROSOFT
        "zulu", "azul" -> JvmVendorSpec.AZUL
        "sap", "sapmachine" -> JvmVendorSpec.SAP
        "apple" -> JvmVendorSpec.APPLE
        "hewlett_packard" -> JvmVendorSpec.HEWLETT_PACKARD
        "jetbrains" -> JvmVendorSpec.JETBRAINS
        "tencent" -> JvmVendorSpec.TENCENT
        else -> throw IllegalArgumentException(
            "Unknown value '$value' for property 'jdk'. Use a friendly name (corretto, liberica, " +
                "temurin, oracle, openj9, graalvm, microsoft, zulu, sap) or a JvmVendorSpec enum name."
        )
    }
