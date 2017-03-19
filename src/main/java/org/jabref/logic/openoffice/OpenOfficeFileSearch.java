package org.jabref.logic.openoffice;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;

public class OpenOfficeFileSearch {
    /**
     * Detects existing installation of OpenOffice and LibreOffice.
     *
     * @return a list of detected installation paths
     */
    public static List<Path> detectInstallations() {
        if (OS.WINDOWS) {
            List<Path> programDirs = findWindowsOpenOfficeDirs();
            return programDirs.stream().filter(dir -> FileUtil.find(OpenOfficePreferences.WINDOWS_EXECUTABLE, dir).isPresent()).collect(Collectors.toList());
        } else if (OS.OS_X) {
            List<Path> programDirs = findOSXOpenOfficeDirs();
            return programDirs.stream().filter(dir -> FileUtil.find(OpenOfficePreferences.OSX_EXECUTABLE, dir).isPresent()).collect(Collectors.toList());
        } else if (OS.LINUX) {
            List<Path> programDirs = findLinuxOpenOfficeDirs();
            return programDirs.stream().filter(dir -> FileUtil.find(OpenOfficePreferences.LINUX_EXECUTABLE, dir).isPresent()).collect(Collectors.toList());
        }
        return new ArrayList<>(0);
    }

    private static List<Path> findOpenOfficeDirectories(List<Path> programDirectories) {
        List<Path> result = new ArrayList<>();

        for (Path programDir : programDirectories) {
            File[] subDirs = programDir.toFile().listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File dir : subDirs) {
                    if (dir.getPath().toLowerCase(Locale.ROOT).contains("openoffice") || dir.getPath().toLowerCase(Locale.ROOT).contains("libreoffice")) {
                        result.add(dir.toPath());
                    }
                }
            }
        }
        return result;
    }

    private static List<Path> findWindowsOpenOfficeDirs() {
        List<Path> sourceList = new ArrayList<>();

        // 64-bit program directory
        String progFiles = System.getenv("ProgramFiles");
        if (progFiles != null) {
            sourceList.add(Paths.get(progFiles));
        }

        // 32-bit program directory
        progFiles = System.getenv("ProgramFiles(x86)");
        if (progFiles != null) {
            sourceList.add(Paths.get(progFiles));
        }

        return findOpenOfficeDirectories(sourceList);
    }

    private static List<Path> findOSXOpenOfficeDirs() {
        List<Path> sourceList = Arrays.asList(Paths.get("/Applications"));

        return findOpenOfficeDirectories(sourceList);
    }

    private static List<Path> findLinuxOpenOfficeDirs() {
        List<Path> sourceList = Arrays.asList(Paths.get("/usr/lib"), Paths.get("/usr/lib64"), Paths.get("/opt"));

        return findOpenOfficeDirectories(sourceList);
    }
}
