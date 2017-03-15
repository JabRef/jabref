package org.jabref.logic.openoffice;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.io.FileUtil;

public class OpenOfficeFileSearch {
    /**
     * Search for Program files directory.
     * @return the File pointing to the Program files directory, or null if not found.
     *   Since we are not including a library for Windows integration, this method can't
     *   find the Program files dir in localized Windows installations.
     */
    public List<Path> findWindowsOpenOfficeDirs() {
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
    public List<Path> findOSXOpenOfficeDirs() {
        List<Path> dirList = new ArrayList<>();

        File rootDir = new File("/Applications");
        File[] dirs = rootDir.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                if ("OpenOffice.org.app".equals(dir.getName()) || "LibreOffice.app".equals(dir.getName())) {
                    dirList.add(dir.toPath());
                }
            }
        }

        return dirList;
    }

    public List<Path> findLinuxOpenOfficeDirs() {
        List<Path> dirList = new ArrayList<>();
        List<String> sourceList = new ArrayList<>();

        sourceList.add("/usr/lib");
        sourceList.add("/usr/lib64");
        sourceList.add("/opt");

        for (String rootPath : sourceList) {
            File root = new File(rootPath);
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.getPath().contains("openoffice") || dir.getPath().contains("libreoffice")) {
                        dirList.add(dir.toPath());
                    }
                }
            }
        }

        return dirList;
    }
}
