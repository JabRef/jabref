package org.jabref.cli.converter;

import java.io.File;

import org.jabref.logic.util.io.FileUtil;

import picocli.CommandLine;

/// Converts Cygwin-style paths to File objects using platform-specific formatting.
public class FilePathConverter implements CommandLine.ITypeConverter<File> {

    @Override
    public File convert(String filePath) {
        String normalizedPath = FileUtil.convertCygwinPathToWindows(filePath);
        return new File(normalizedPath);
    }
}
