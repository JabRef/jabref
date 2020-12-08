package org.jabref.gui.filewizard;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.application.Platform;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.AutoSetFileLinksUtil;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This class is used to check if an entry has a linked file in the target directory.
 * If not an attempt is made to copy a file from the default directory to the target directory.
 * In case no linked file could be found in or copied to the target directory,
 * the target directory is scanned for a corresponding local file and linked in case of success.
 */


public class FileWizardLocal {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWizardManager.class);

    /**
     * Checks if a linked file in target directory exists
     *
     * @param entry      the BibEntry to be checked
     * @param targetDir  the target directory
     * @param defaultDir the default directory (used for {@code copyFileFromDefaultDirectory} call)
     * @return true if a linked file exists target directory
     */
    public static boolean fileExistsLocally(BibEntry entry, Path targetDir, List<Path> defaultDir) {

        // create list with target directory
        List<Path> targetDirAsList = new ArrayList<>();
        targetDirAsList.add(targetDir);

        //return false if entry has no linked files
        if (entry.getFiles().size() == 0) {
            return false;
        }

        //iterate through linked files
        for (LinkedFile linkedFile : entry.getFiles()) {

            //return true if one linked file is in target directory
            if (linkedFile.findIn(targetDirAsList).isPresent()) {
                return true;
            }
        }

        //try to copy file from default directory to target diresctory
        return copyFileFromDefaultDirectory(entry, targetDir, defaultDir);
    }

    /**
     * Tries to copy file to target directory from default directory
     *
     * @param entry      the BibEntry to check
     * @param targetDir  the target directory
     * @param defaultDir the default directory
     * @return true if file could be copied from default to target directory
     */
    public static boolean copyFileFromDefaultDirectory(BibEntry entry, Path targetDir, List<Path> defaultDir) {

        //iterate over linked files
        Iterator<LinkedFile> linkedFileIterator = entry.getFiles().iterator();
        while (linkedFileIterator.hasNext()) {

            LinkedFile linkedFile = linkedFileIterator.next();

            try {
                // path to file in default directory
                Path oldPath = linkedFile.findIn(defaultDir).get();

                // path to file in target directory
                Path newPath = targetDir.resolve(oldPath.getFileName());

                // copy file from default to target directory
                Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);

                // add linked file in target directory to file list and return true
                entry.addFile(new LinkedFile(linkedFile.getDescription(), newPath, linkedFile.getFileType()));
                return true;

                // is not able to copy current linked file
            } catch (Exception e) {

                // delete linked file if local file does not exist
                if (!new File(linkedFile.getLink()).exists()) {
                    linkedFileIterator.remove();
                }

                continue;
            }
        }

        //if no file could be copied
        return false;
    }

    /**
     * If the BibEntry doesn't have a file field, then the directory is searched for files whose names
     * match the pattern defined in the settings, by default starting with the citation key.
     *
     * @param entry        the bibEntry to be checked
     * @param fileLinkUtil AutoSetFileLinksUtil for finding associated files
     * @return true if a file could be found and linked in target directory
     * @throws IOException
     */

    public static boolean mapToFileInDirectory(BibEntry entry, AutoSetFileLinksUtil fileLinkUtil) {

        try {
            // search for matching local files
            List<LinkedFile> linkedFiles = fileLinkUtil.findAssociatedNotLinkedFiles(entry);

            // add matching files if present and return true
            if (!linkedFiles.isEmpty()) {
                entry.setFiles(linkedFiles);
                linkedFiles.clear();
                return true;

                // return false if no matching files were found
            } else {
                return false;
            }

        } catch (IOException ioe) {
            LOGGER.error("IO exception in mapToFile");
            return false;
        }
    }

}
