package org.jabref.gradle

import org.gradle.api.Project

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
