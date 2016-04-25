/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.openoffice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpenOfficeFileSearch {

    private boolean fileSearchCanceled;


    /**
     * Search for Program files directory.
     * @return the File pointing to the Program files directory, or null if not found.
     *   Since we are not including a library for Windows integration, this method can't
     *   find the Program files dir in localized Windows installations.
     */
    public List<File> findWindowsProgramFilesDir() {
        List<String> sourceList = new ArrayList<>();
        List<File> dirList = new ArrayList<>();

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
                        dirList.add(dir);
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
    public List<File> findOSXProgramFilesDir() {
        List<File> dirList = new ArrayList<>();

        File rootDir = new File("/Applications");
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && ("OpenOffice.org.app".equals(file.getName())
                        || "LibreOffice.app".equals(file.getName()))) {
                    dirList.add(file);
                }
            }
        }

        return dirList;
    }

    public void resetFileSearch() {
        fileSearchCanceled = false;
    }

    public void cancelFileSearch() {
        fileSearchCanceled = true;
    }

    public List<File> findFileInDirs(List<File> dirList, String filename) {
        List<File> sofficeFiles = new ArrayList<>();
        for (File dir : dirList) {
            if (fileSearchCanceled) {
                break;
            }
            findFileInDir(dir, filename).ifPresent(sofficeFiles::add);
        }
        return sofficeFiles;
    }
    /**
    * Search for a file, starting at the given directory.
    * @param startDir The starting point.
    * @param filename The name of the file to search for.
    * @return The directory where the file was first found, or null if not found.
    */
    public Optional<File> findFileInDir(File startDir, String filename) {
        if (fileSearchCanceled) {
            return Optional.empty();
        }
        File[] files = startDir.listFiles();
        if (files == null) {
            return Optional.empty();
        }
        Optional<File> result = Optional.empty();
        for (File file : files) {
            if (fileSearchCanceled) {
                return Optional.empty();
            }
            if (file.isDirectory()) {
                result = findFileInDir(file, filename);
                if (result.isPresent()) {
                    return result;
                }
            } else if (file.getName().equals(filename)) {
                return Optional.of(startDir);
            }
        }
        return result;
    }

}
