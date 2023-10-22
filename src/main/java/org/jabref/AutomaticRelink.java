package org.jabref;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class AutomaticRelink {

    public static void relink(BibEntry bib, String defaultFileDirectory) {
        // Check if the file is there
        List<LinkedFile> lfs = bib.getFiles();

        // Run through all linked files
        // Check if the files exist 
        // Then if they don't change them
        for (LinkedFile currentLF : lfs) {
           Path path = Paths.get(currentLF.getLink());

            if (!Files.exists(path)) {
                // Find the File!
                String filename = path.getFileName().toString();
                File directory = new File(defaultFileDirectory);
                List<String> fileLocations = new ArrayList<>();

                if (directory.exists() && directory.isDirectory()) {
                    searchFileInDirectoryAndSubdirectories(directory, filename, fileLocations);
                    if (!fileLocations.isEmpty()) {
                        currentLF.setLink(fileLocations.get(0));
                    } else {
                        System.out.println("File not found.");
                    }
                } else {
                    System.out.println("Directory not found.");
                }
            }
            }
        bib.setFiles(lfs);
        }

    public static void searchFileInDirectoryAndSubdirectories(File directory, String targetFileName, List<String> fileLocations) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFileInDirectoryAndSubdirectories(file, targetFileName, fileLocations);
                } else if (file.getName().equals(targetFileName)) {
                    fileLocations.add(file.getAbsolutePath());
                }
            }
        }
    }
}
