package net.sf.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileFinder {

    private static final Log LOGGER = LogFactory.getLog(FileFinder.class);


    public static Set<File> findFiles(List<String> extensions, List<File> directories) {

        Objects.requireNonNull(directories, "Directories must not be null!");
        Objects.requireNonNull(extensions, "Extensions must not be null!");

        BiPredicate<Path, BasicFileAttributes> isDirectoryAndContainsExtension = (path,
                attr) -> !Files.isDirectory(path)
                        && extensions.contains(FileUtil.getFileExtension(path.toFile()).orElse(""));

        Set<File> result = new HashSet<>();
        for (File directory : directories) {

            try (Stream<File> files = Files.find(directory.toPath(), Integer.MAX_VALUE, isDirectoryAndContainsExtension)
                    .map(x -> x.toFile())) {
                result.addAll(files.collect(Collectors.toSet()));

            } catch (IOException e) {
                LOGGER.error("Problem in finding files", e);
            }
        }
        return result;

    }
}
