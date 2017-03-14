package org.jabref.logic.openoffice;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OpenOfficeFileSearch {
    /**
     * Search for Program files directory.
     * @return the File pointing to the Program files directory, or null if not found.
     *   Since we are not including a library for Windows integration, this method can't
     *   find the Program files dir in localized Windows installations.
     */
    public List<Path> findWindowsProgramFilesDir() {
        List<String> sourceList = new ArrayList<>();
        List<Path> dirList = new ArrayList<>();

        // Check default 64-bits program directory
        String progFiles = System.getenv("ProgramFiles");
        if (progFiles != null) {
            sourceList.add(progFiles);
        }

        // Check default 64-bits program directory
        progFiles = System.getenv("ProgramFiles(x86)");
        if (progFiles != null) {
            sourceList.add(progFiles);
        }

        for (String rootPath : sourceList) {
            File root = new File(rootPath);
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.getPath().contains("OpenOffice") || dir.getPath().contains("LibreOffice")) {
                        dirList.add(dir.toPath());
                    }
                }
            }
        }
        return dirList;
    }

    /**
     * Search for Program files directory.
     * @return the File pointing to the Program files directory, or null if not found.
     *   Since we are not including a library for Windows integration, this method can't
     *   find the Program files dir in localized Windows installations.
     */
    public List<Path> findOSXProgramFilesDir() {
        List<Path> dirList = new ArrayList<>();

        File rootDir = new File("/Applications");
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && ("OpenOffice.org.app".equals(file.getName())
                        || "LibreOffice.app".equals(file.getName()))) {
                    dirList.add(file.toPath());
                }
            }
        }

        return dirList;
    }
}
