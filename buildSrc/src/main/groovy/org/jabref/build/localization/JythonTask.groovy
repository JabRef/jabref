package org.jabref.build.localization

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

class JythonTask extends JavaExec {


    public static final String JYTHON_MAIN = 'org.python.util.jython'

    @TaskAction
    @Override
    void exec() {
        main JYTHON_MAIN
        classpath project.configurations.getByName(LocalizationPlugin.CONFIGURATION_NAME).asPath

        super.exec()
    }
}
