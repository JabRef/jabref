package org.jabref.gui.texparser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.util.BackgroundTask;

import static java.nio.file.FileVisitResult.CONTINUE;

public class WalkFileTreeTask extends BackgroundTask<List<Path>> {

    private static final String TEX_EXT = ".tex";

    private final Path directory;

    public WalkFileTreeTask(Path directory) {
        this.directory = directory;
    }

    @Override
    protected List<Path> call() {
        return searchDirectory(directory);
    }

    private List<Path> searchDirectory(Path directory) {
        List<Path> searchPathList = new ArrayList<>();

        try {
            Files.walkFileTree(directory, new FileVisitor<Path>() {
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

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(searchPathList);

        return searchPathList;
    }
}
