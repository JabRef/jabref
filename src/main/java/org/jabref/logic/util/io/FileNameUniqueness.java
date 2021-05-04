package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;

public class FileNameUniqueness {

    /**
     * Returns a file name such that it does not match any existing files in targetDirectory
     *
     * @param targetDirectory The directory in which file name should be unique
     * @param fileName        Suggested name for the file
     * @return a file name such that it does not match any existing files in targetDirectory
     */
    public static String getNonOverWritingFileName(Path targetDirectory, String fileName) {

        Optional<String> extensionOptional = FileUtil.getFileExtension(fileName);

        // the suffix include the '.' , if extension is present Eg: ".pdf"
        String extensionSuffix;
        String fileNameWithoutExtension;

        if (extensionOptional.isPresent()) {
            extensionSuffix = '.' + extensionOptional.get();
            fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        } else {
            extensionSuffix = "";
            fileNameWithoutExtension = fileName;
        }

        String newFileName = fileName;

        int counter = 1;
        while (Files.exists(targetDirectory.resolve(newFileName))) {
            newFileName = fileNameWithoutExtension +
                    " (" + counter + ")" +
                    extensionSuffix;
            counter++;
        }

        return newFileName;
    }

    /**
     *
     * @param directory The directory which saves the files (.pdf, for example)
     * @param fileName Suggest name for the newly downloaded file
     * @param dialogService To display the error and success message
     * @return true when the content of files is duplicate and successfully delete the newly downloaded file ,
     *         false when it is not a duplicate file or fail to delete the duplicate file
     * @throws IOException Fail when the file is not exist or something wrong when reading the file
     */
    public static boolean isDuplicatedFile(String directory, String fileName, DialogService dialogService) throws IOException {

        Optional<String> extensionOptional = FileUtil.getFileExtension(fileName);
        String extensionSuffix;
        String fileNameWithoutDuplicated;

        if (extensionOptional.isPresent()) {
            extensionSuffix = '.' + extensionOptional.get();
            fileNameWithoutDuplicated = eraseDuplicateMarks(fileName.substring(0, fileName.lastIndexOf('.')));
        } else {
            extensionSuffix = "";
            fileNameWithoutDuplicated = eraseDuplicateMarks(fileName);
        }

        String originalFileName = fileNameWithoutDuplicated + extensionSuffix;
        if (fileName.equals(originalFileName)) {
            return false;
        }

        File originalFile = new File(directory, originalFileName);
        // deal with a very special case, when duplication does not happen, but the file name is end with something like " (1)" as it originally is
        if (!originalFile.exists()) {
            return false;
        }

        File duplicateFile = new File(directory, fileName);
        assert (duplicateFile.exists());
        int counter = 1;
        while (true) {
            if (com.google.common.io.Files.equal(originalFile, duplicateFile)) {
                if (duplicateFile.delete()) {
                    dialogService.notify(Localization.lang("Dupilcate file with '%0', succesfully delete the file '%1'", originalFileName, fileName));
                } else {
                    dialogService.notify(Localization.lang("Dupilcate file with '%0', fail to delete the file '%1'", originalFileName, fileName));
                }
                return true;
            }

            originalFileName = fileNameWithoutDuplicated +
                    " (" + counter + ")"
                    + extensionSuffix;
            counter++;

            if (originalFileName.equals(fileName)) {
                dialogService.notify(Localization.lang("Duplicate file name but different content, keep the new file '%0'", fileName));
                return false;
            }

            originalFile = new File(directory, originalFileName);
            if (!originalFile.exists()) {
                return false;
            }
        }
    }

    /**
     * recover the file name to origin if it has duplicate mark such as " (1)"
     * It will change the String whose format is "xxxxxx (number)" into "xxxxxx", while return the same String when it does not match the format
     *
     * @param fileName Suggested name for the file without extensionSuffix, if it has duplicate file name with other file, it will end with something like " (1)"
     * @return Suggested name for the file without extensionSuffix and duplicate marks such as " (1)"
     */
    public static String eraseDuplicateMarks(String fileName) {
        int dotPosition1 = fileName.lastIndexOf(')');
        if (dotPosition1 != fileName.length() - 1 || dotPosition1 <= 2) {
            return fileName;
        } else {
            if (!Character.isDigit(fileName.charAt(dotPosition1 - 1))) {
                return fileName;
            }
        }
        int dotPosition = fileName.lastIndexOf('(');
        if ((dotPosition > 0) && (dotPosition < (fileName.length() - 1))) {
            fileName = fileName.substring(0, dotPosition - 1);
        }
        return fileName;
    }
}
