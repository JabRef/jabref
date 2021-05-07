package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static boolean isDuplicatedFile(Path directory, String fileName, DialogService dialogService) throws IOException {

        String fileNameWithoutDuplicated = eraseDuplicateMarks(FileUtil.getBaseName(fileName));
        String extensionSuffix = FileUtil.getFileExtension(fileName).orElse("");
        extensionSuffix = extensionSuffix.equals("") ? extensionSuffix : "." + extensionSuffix;

        String originalFileName = fileNameWithoutDuplicated + extensionSuffix;
        if (fileName.equals(originalFileName)) {
            return false;
        }

        Path originalFile = directory.resolve(originalFileName);
        Path duplicateFile = directory.resolve(fileName);
        int counter = 1;

        while (Files.exists(originalFile)) {
            if (com.google.common.io.Files.equal(originalFile.toFile(), duplicateFile.toFile())) {
                if (duplicateFile.toFile().delete()) {
                    dialogService.notify(Localization.lang("File '%1' is a duplicate of '%0'. Keeping '%0'", originalFileName, fileName));
                } else {
                    dialogService.notify(Localization.lang("File '%1' is a duplicate of '%0'. Keeping both due to deletion error", originalFileName, fileName));
                }
                return true;
            }

            originalFileName = fileNameWithoutDuplicated +
                    " (" + counter + ")"
                    + extensionSuffix;
            counter++;

            if (originalFileName.equals(fileName)) {
                return false;
            }
            originalFile = directory.resolve(originalFileName);
        }
        return false;
    }

    /**
     * This is the opposite function of getNonOverWritingFileName
     * It will recover the file name to origin if it has duplicate mark such as " (1)"
     * change the String whose format is "xxxxxx (number)" into "xxxxxx", while return the same String when it does not match the format
     * This is the opposite function of getNonOverWritingFileName
     *
     * @param fileName Suggested name for the file without extensionSuffix, if it has duplicate file name with other file, it will end with something like " (1)"
     * @return Suggested name for the file without extensionSuffix and duplicate marks such as " (1)"
     */
    public static String eraseDuplicateMarks(String fileName) {
        Pattern p = Pattern.compile("(.*) \\(\\d+\\)");
        Matcher m = p.matcher(fileName);
        return m.find() ? fileName.substring(0, fileName.lastIndexOf('(') - 1) : fileName;
    }
}
