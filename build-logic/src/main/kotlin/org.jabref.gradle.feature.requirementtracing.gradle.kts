import java.io.File
import java.util.Locale

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.itsallcode.openfasttrace.api.core.ItemStatus
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration
import org.itsallcode.openfasttrace.gradle.config.TracingConfig
import org.itsallcode.openfasttrace.gradle.task.CollectTask
import org.itsallcode.openfasttrace.gradle.task.TraceTask
import org.itsallcode.openfasttrace.gradle.task.config.SerializableTagPathConfig

val taskGroupName = "trace"
val requirementConfigName = "oftRequirementConfig"

allprojects {
    val tracingConfig = extensions.create("requirementTracing", TracingConfig::class.java, project)
    (tracingConfig as ExtensionAware).extensions.create("tags", TagPathConfiguration::class.java, project)
}

gradle.projectsEvaluated {
    val collectTask = tasks.register<CollectTask>("collectRequirements") {
        group = taskGroupName
        description = "Collect requirements and generate specobject file"
        inputDirectories.set(getAllInputDirectories(rootProject.allprojects))
        outputFile.set(rootProject.layout.buildDirectory.file("reports/requirements.xml"))
        pathConfig.set(getPathConfig(rootProject.allprojects))
    }

    tasks.register<TraceTask>("traceRequirements") {
        group = taskGroupName
        description = "Trace requirements and generate tracing report"
        dependsOn(collectTask)

        val tracingConfig = rootProject.getTracingConfig()
        failBuild.set(tracingConfig.failBuild)
        requirementsFile.set(collectTask.flatMap { task -> task.outputFile })

        if (tracingConfig.reportFile.isPresent) {
            outputFile.set(tracingConfig.reportFile)
        } else {
            val extension = if (tracingConfig.reportFormat.get() == "html") "html" else "txt"
            outputFile.set(rootProject.layout.buildDirectory.file("reports/tracing.$extension"))
        }

        reportVerbosity.set(tracingConfig.reportVerbosity)
        reportFormat.set(tracingConfig.reportFormat)
        importedRequirements.set(getImportedRequirements(rootProject.allprojects))
        filteredArtifactTypes.set(tracingConfig.filteredArtifactTypes)
        filteredTags.set(tracingConfig.filteredTags)
        filterAcceptsItemsWithoutTag.set(tracingConfig.filterAcceptsItemsWithoutTag)
        filterWantedStatuses.set(getWantedStatuses(tracingConfig))
        detailsSectionDisplay.set(tracingConfig.detailsSectionDisplay)
    }
}

fun Project.getTracingConfig(): TracingConfig = extensions.getByType()

fun getAllInputDirectories(allProjects: Set<Project>): Set<File> = allProjects
    .flatMap { project -> project.getTracingConfig().inputDirectories.files }
    .toSet()

fun getImportedRequirements(allProjects: Set<Project>): Set<File> = allProjects
    .flatMap(::getImportedRequirements)
    .toSet()

fun getImportedRequirements(project: Project): Set<File> {
    val importedRequirements = project.getTracingConfig().importedRequirements.orNull.orEmpty()
    if (importedRequirements.isEmpty()) {
        return emptySet()
    }

    val configuration = project.configurations.findByName(requirementConfigName)
        ?: project.configurations.create(requirementConfigName)

    importedRequirements.forEach { dependency ->
        project.dependencies.add(requirementConfigName, dependency)
    }

    return configuration.files
}

fun getPathConfig(allProjects: Set<Project>): List<SerializableTagPathConfig> = allProjects
    .mapNotNull(::getTagPathConfig)

fun getTagPathConfig(project: Project): SerializableTagPathConfig? {
    val tagPathConfig = project.getTracingConfig().tagPathConfig
    if (tagPathConfig.pathConfig.isEmpty()) {
        return null
    }
    return SerializableTagPathConfig(tagPathConfig)
}

fun getWantedStatuses(tracingConfig: TracingConfig): Set<ItemStatus> =
    tracingConfig.filterWantedStatuses.getOrElse(emptySet()).map(::convertStatus).toSet()

fun convertStatus(value: String): ItemStatus = try {
    ItemStatus.valueOf(value.uppercase(Locale.ROOT))
} catch (exception: IllegalArgumentException) {
    val validStatuses = ItemStatus.values().joinToString(", ") { status -> status.name }
    throw IllegalArgumentException(
        "Invalid status '$value'. Valid statuses are: $validStatuses",
        exception,
    )
}
