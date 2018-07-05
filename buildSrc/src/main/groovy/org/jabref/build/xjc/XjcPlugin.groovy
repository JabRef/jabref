package org.jabref.build.xjc

import org.gradle.api.Plugin
import org.gradle.api.Project

class XjcPlugin implements Plugin<Project> {

    static final def CONFIGURATION_NAME = "xjc"

    @Override
    void apply(Project target) {
        def configuration = target.configurations.create(CONFIGURATION_NAME)
        configuration.description = "Dependencies needed to run the XJC tool."

        target.afterEvaluate { evaluated ->
            evaluated.logger.info(evaluated.configurations.xjc.asPath)
            evaluated.ant.taskdef(name: 'xjc', classname: 'com.sun.tools.xjc.XJCTask', classpath: evaluated.configurations.getByName(CONFIGURATION_NAME).asPath)
        }
    }
}
