package org.jabref.cli.converter;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.util.io.FileUtil;

import picocli.CommandLine;

/// Converts Cygwin-style paths to Path objects using platform-specific formatting.
public class PathConverter implements CommandLine.ITypeConverter<Path> {

    @Override
    public Path convert(String value) {
        String normalizedPath = FileUtil.convertCygwinPathToWindows(value);
        return Paths.get(normalizedPath);
    }
}
