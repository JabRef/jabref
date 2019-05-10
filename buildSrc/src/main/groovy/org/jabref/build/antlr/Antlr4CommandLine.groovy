package org.jabref.build.antlr

import org.gradle.api.file.FileCollection

class Antlr4CommandLine implements AntlrCommandLine {

    private final AntlrTask task

    Antlr4CommandLine(AntlrTask task) {
        this.task = task
    }

    @Override
    String getMain() {
        return "org.antlr.v4.Tool"
    }

    @Override
    FileCollection getClasspath() {
        return task.project.configurations.antlr4
    }

    @Override
    List<String> getArguments() {
        return ["-o", file(task.outputDir), "-visitor", "-no-listener", "-package", task.javaPackage, file(task.inputFile)]
    }

    private String file(String path) {
        return task.project.file(path).toString()
    }
}
