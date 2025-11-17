package org.jabref.toolkit.converter;

import java.nio.file.Path;

import org.jabref.logic.util.io.FileUtil;

import picocli.CommandLine;

/// Converts Cygwin-style paths to Path objects using platform-specific formatting.
public class CygWinPathConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String path) {
        return FileUtil.convertCygwinPathToWindows(path);
    }
}
