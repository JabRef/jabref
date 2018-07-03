package org.jabref.build.antlr

import org.gradle.api.file.FileCollection

class Antlr3CommandLine implements AntlrCommandLine {

    private final task

    Antlr3CommandLine(AntlrTask task) {
        this.task = task
    }

    @Override
    String getMain() {
        return "org.antlr.Tool"
    }

    @Override
    FileCollection getClasspath() {
        return task.project.configurations.antlr3
    }

    @Override
    List<String> getArguments() {
        return ["-o", task.project.file(task.outputDir).toString(), task.project.file(task.inputFile).toString()]
    }
}
