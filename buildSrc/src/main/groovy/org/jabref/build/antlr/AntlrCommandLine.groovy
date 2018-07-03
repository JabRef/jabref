package org.jabref.build.antlr

import org.gradle.api.file.FileCollection

/**
 * Encapsulates a command line call to an version of the ANTLR tools.
 */
interface AntlrCommandLine {

    String getMain()

    FileCollection getClasspath()

    List<String> getArguments()

}
