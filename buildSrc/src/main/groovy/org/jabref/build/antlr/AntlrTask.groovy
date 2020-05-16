package org.jabref.build.antlr

import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

class AntlrTask extends JavaExec {

    static def ANTLR3 = Antlr3CommandLine
    static def ANTLR4 = Antlr4CommandLine

    private Class<? extends AntlrCommandLine> antlr = ANTLR3
    private String inputFile = ""
    private String outputDir = ""
    private String javaPackage = ""
    private AntlrCommandLine commandLine

    AntlrTask() {
        project.configurations {
            antlr3
            antlr4
        }
    }

    @Override
    Task configure(Closure closure) {
        commandLine = antlr.newInstance(this)

        setMain(commandLine.main)

        return super.configure(closure)
    }

    @TaskAction
    @Override
    void exec() {
        classpath(commandLine.classpath)
        args = commandLine.arguments

        super.exec()
    }

    Class<? extends AntlrCommandLine> getAntlr() {
        return antlr
    }

    void setAntlr(Class<? extends AntlrCommandLine> antlr) {
        this.antlr = antlr
    }

    String getInputFile() {
        return inputFile
    }

    void setInputFile(String inputFile) {
        this.inputFile = inputFile
        inputs.file(inputFile)
    }

    String getOutputDir() {
        return outputDir
    }

    void setOutputDir(String outputDir) {
        this.outputDir = outputDir
        outputs.dir(outputDir)
    }

    String getJavaPackage() {
        return javaPackage
    }

    void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage
    }
}
