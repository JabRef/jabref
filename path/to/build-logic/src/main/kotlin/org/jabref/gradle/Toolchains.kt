# complete code
import org.gradle.api.Project
import org.gradle.api.Task

object Toolchains {
    fun selectAndFocusNewEntry(project: Project, task: Task) {
        project.tasks.findByName("compile")?.let { compileTask ->
            compileTask.doLast {
                val bibliographyPanel = project.objects.newInstance(BibliographyPanel::class.java)
                bibliographyPanel.selectAndFocusNewEntry()
            }
        }
    }
}