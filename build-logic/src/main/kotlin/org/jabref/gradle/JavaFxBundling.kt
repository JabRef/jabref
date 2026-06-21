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
    get() = providers.gradleProperty("useLibericaJdkFull").map { it != "false" }.getOrElse(false)
