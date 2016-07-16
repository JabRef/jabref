package net.sf.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileFinder {

    private static final Log LOGGER = LogFactory.getLog(FileFinder.class);


    public static Set<File> findFiles(Collection<String> extensions, Collection<File> directories) {

        Objects.requireNonNull(directories, "Directories must not be null!");
        Objects.requireNonNull(extensions, "Extensions must not be null!");

        Set<File> result = new HashSet<>();
        for (File directory : directories) {

            try {
                Set<File> files = Files
                        .find(directory.toPath(), Integer.MAX_VALUE,
                                (path, attr) -> !Files.isDirectory(path)
                                        && extensions.contains(FileUtil.getFileExtension(path.toFile()).orElse("")))
                        .map(x -> x.toFile()).collect(Collectors.toSet());
                result.addAll(files);

            } catch (IOException e) {
                LOGGER.error("Problem in finding files", e);
            }
        }
        return result;
    }
}
