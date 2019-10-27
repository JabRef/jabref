package org.jabref.build.antlr

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Configures the project for use with ANTLR 3 or 4.
 */
class JabRefAntlrPlugin implements Plugin<Project> {

    public static final def ANTLR3_CONFIGURATION_NAME = "antlr3"
    public static final def ANTLR4_CONFIGURATION_NAME = "antlr4"

    @Override
    void apply(Project target) {
        def antlr3Cfg = target.configurations.create(ANTLR3_CONFIGURATION_NAME)
        antlr3Cfg.description = "Dependencies required to run the ANTLR3 tool."

        def antlr4Cfg = target.configurations.create(ANTLR4_CONFIGURATION_NAME)
        antlr4Cfg.description = "Dependencies required to run the ANTLR4 tool."
    }

}
