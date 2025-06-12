package org.jabref.cli.converter;

import org.jabref.logic.util.io.FileUtil;

import picocli.CommandLine;

 /// Converts Cygwin-style paths to the standard format of the operating system.
 /// Especially useful on Windows to handle paths like /c/Users/... -> C:\Users\...
public class StringPathConverter implements CommandLine.ITypeConverter<String> {

    @Override
    public String convert(String filePath) {
        return FileUtil.convertCygwinPathToWindows(filePath);
    }
}
