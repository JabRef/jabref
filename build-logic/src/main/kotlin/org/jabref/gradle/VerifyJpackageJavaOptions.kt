package org.jabref.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jspecify.annotations.NullMarked

@NullMarked
abstract class VerifyJpackageJavaOptions : DefaultTask() {

    @get:Input
    abstract val mismatches: ListProperty<String>

    @TaskAction
    fun verify() {
        mismatches.get().takeIf(List<String>::isNotEmpty)?.let {
            throw GradleException(it.joinToString(separator = System.lineSeparator()))
        }
    }
}
