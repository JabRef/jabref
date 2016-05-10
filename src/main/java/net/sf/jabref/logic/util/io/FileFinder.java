package net.sf.jabref.logic.util.io;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FileFinder {

    public static Set<File> findFiles(Collection<String> extensions, Collection<File> directories) {
        Set<File> result = new HashSet<>();

        for (File directory : directories) {
            result.addAll(FileFinder.findFiles(extensions, directory));
        }

        return result;
    }

    private static Collection<? extends File> findFiles(Collection<String> extensions, File directory) {
        Set<File> result = new HashSet<>();

        File[] children = directory.listFiles();
        if (children == null) {
            return result; // No permission?
        }

        for (File child : children) {
            if (child.isDirectory()) {
                result.addAll(FileFinder.findFiles(extensions, child));
            } else {
                FileUtil.getFileExtension(child).ifPresent(extension -> {
                    if (extensions.contains(extension)) {
                        result.add(child);
                    }
                });
            }
        }

        return result;
    }

}
