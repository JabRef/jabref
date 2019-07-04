package org.jabref.build.localization

import org.gradle.api.Plugin
import org.gradle.api.Project

class LocalizationPlugin implements Plugin<Project> {

    public static final def CONFIGURATION_NAME = "jython"
    public static final def TASK_GROUP = 'Localization'

    @Override
    void apply(Project target) {
        def configuration = target.configurations.create("jython")
        configuration.description = "Dependencies needed to run jython."

        target.extensions.create('localization', LocalizationExtension)

        target.afterEvaluate { project ->
            initLocalizationTasks(project, project.extensions.getByType(LocalizationExtension))
        }
    }

    private def initLocalizationTasks(Project project, LocalizationExtension extension) {
        project.tasks.create('localizationStatusWithMarkdown', JythonTask) {
            description "Creates an update file in Markdown"
            group = TASK_GROUP

            args project.file(extension.script)
            args "markdown"
        }

        project.tasks.create('localizationStatus', JythonTask) {
            description "Prints the current status"
            group = TASK_GROUP

            args project.file(extension.script)
            args "status"
        }

        project.tasks.create('localizationStatusExtended', JythonTask) {
            description "Prints the current status (extended output)"
            group = TASK_GROUP

            args project.file(extension.script)
            args "status"
            args "--extended"
        }

        project.tasks.create('localizationUpdate', JythonTask) {
            description "Updates the localization files (fixes duplicates, adds missing keys, and sort them"
            group = TASK_GROUP

            args project.file(extension.script)
            args "update"
        }

        project.tasks.create('localizationUpdateExtended', JythonTask) {
            description "Updates the localization files (fixes duplicates, adds missing keys, and sort them (extended output)"
            group = TASK_GROUP

            args project.file(extension.script)
            args "update"
            args "--extended"
        }
    }
}
