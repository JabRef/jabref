package net.sf.jabref.openoffice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.logic.journals.JournalAbbreviationRepository;

public class OpenOfficeFileSearch {

    private static final Log LOGGER = LogFactory.getLog(OpenOfficeFileSearch.class);

    private static final String STYLE_FILE_EXTENSION = ".jstyle";

    private final JournalAbbreviationRepository repository;


    public OpenOfficeFileSearch(JournalAbbreviationRepository repository) {
        Objects.requireNonNull(repository);
        this.repository = repository;
    }

    /**
     * Search for Program files directory.
     * @return the File pointing to the Program files directory, or null if not found.
     *   Since we are not including a library for Windows integration, this method can't
     *   find the Program files dir in localized Windows installations.
     */
    public List<File> findWindowsProgramFilesDir() {
        List<String> sourceList = new ArrayList<>();
        List<File> dirList = new ArrayList<>();

         // 64-bits first
        String progFiles = System.getenv("ProgramFiles");
        if (progFiles != null) {
            sourceList.add(progFiles);
        }

        // Then 32-bits
        progFiles = System.getenv("ProgramFiles(x86)");
        if (progFiles != null) {
            sourceList.add(progFiles);
        }

        for (String rootPath : sourceList) {
            File root = new File(rootPath);
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                Collections.addAll(dirList, dirs);
            }
        }
        return dirList;
    }

    /**
     * If the string dir indicates a file, parse it and add it to the list of styles if
     * successful. If the string dir indicates a directory, parse all files looking like
     * style files, and add them. The parameter recurse determines whether we should
     * recurse into subdirectories.
     * @param dir the directory or file to handle.
     * @param recurse true indicates that we should recurse into subdirectories.
     * @param encoding
     */
    public void addStyles(String dir, boolean recurse, Charset encoding, List<OOBibStyle> styles) {
        File dirF = new File(dir);
        if (dirF.isDirectory()) {
            File[] fileArray = dirF.listFiles();
            List<File> files;
            if (fileArray == null) {
                files = Collections.emptyList();
            } else {
                files = Arrays.asList(fileArray);
            }
            for (File file : files) {
                // If the file looks like a style file, parse it:
                if (!file.isDirectory() && (file.getName().endsWith(STYLE_FILE_EXTENSION))) {
                    addSingleFile(file, encoding, styles);
                } else if (file.isDirectory() && recurse) {
                    // If the file is a directory, and we should recurse, do:
                    addStyles(file.getPath(), recurse, encoding, styles);
                }
            }
        } else {
            // The file wasn't a directory, so we simply parse it:
            addSingleFile(dirF, encoding, styles);
        }
    }

    /**
     * Parse a single file, and add it to the list of styles if parse was successful.
     * @param file the file to parse.
     * @param encoding the encoding of the style file
     * @param styles the list to add the style to
     */
    private void addSingleFile(File file, Charset encoding, List<OOBibStyle> styles) {
        try {
            OOBibStyle style = new OOBibStyle(file, repository, encoding);
            // Check if the parse was successful before adding it:
            if (style.isValid() && !styles.contains(style)) {
                styles.add(style);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to read style file: '" + file.getPath() + "'", e);
        }
    }


}
