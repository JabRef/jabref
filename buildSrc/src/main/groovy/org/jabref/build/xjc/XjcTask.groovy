package org.jabref.build.xjc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class XjcTask extends DefaultTask {

    private def schemaFile
    @Optional
    private def bindingFile
    private def outputDirectory
    private String javaPackage
    @Optional
    private String arguments

    @TaskAction
    def generateClasses() {
        project.mkdir(outputDirectory)
        project.ant.xjc(destdir: outputDirectory, package: javaPackage) {
            schema(dir: schemaFile.getParent(), includes: schemaFile.getName())
            if (bindingFile != null) {
                binding(dir: bindingFile.getParent(), includes: bindingFile.getName())
            }
            if (arguments != null) {
                arg(value: arguments)
            }
        }
    }

    @Input
    File getSchemaFile() {
        if (schemaFile == null) {
            return null
        }
        return project.file(schemaFile)
    }

    void setSchemaFile(Object schemaFile) {
        this.schemaFile = schemaFile
    }

    File getOutputDirectory() {
        if (outputDirectory == null) {
            return null
        }
        return project.file(outputDirectory)
    }

    void setOutputDirectory(Object outputDirectory) {
        this.outputDirectory = outputDirectory
        updateOutput()
    }

    String getJavaPackage() {
        return javaPackage
    }

    void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage
        updateOutput()
    }

    String getArguments() {
        return arguments
    }

    void setArguments(String args) {
        this.arguments = args
    }

    @Input
    File getBindingFile() {
        if (bindingFile == null) {
            return null
        }
        return project.file(bindingFile)
    }

    void setBindingFile(Object binding) {
        this.bindingFile = binding
    }

    private void updateOutput() {
        if (outputDirectory != null && javaPackage != null)
        outputs.dir(new File(getOutputDirectory(), packageAsPath(javaPackage)))
    }

    private static String packageAsPath(String pkg) {
        return pkg.replace((char) '.', File.separatorChar)
    }
}
