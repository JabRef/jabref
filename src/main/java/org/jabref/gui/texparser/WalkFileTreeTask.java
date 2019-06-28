package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.util.BackgroundTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.FileVisitResult.CONTINUE;

public class WalkFileTreeTask extends BackgroundTask<List<Path>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalkFileTreeTask.class);
    private static final String TEX_EXT = ".tex";

    private final Path directory;

    public WalkFileTreeTask(Path directory) {
        this.directory = directory;
    }

    @Override
    protected List<Path> call() {
        List<Path> searchPathList = new ArrayList<>();

        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    searchPathList.add(dir);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && file.toString().endsWith(TEX_EXT)) {
                        searchPathList.add(file);
                    }
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Problem finding files", e);
        }

        Collections.sort(searchPathList);

        return searchPathList;
    }
}
