package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;

public class FileNameUniqueness {
    private static final Pattern DUPLICATE_MARK_PATTERN = Pattern.compile("(.*) \\(\\d+\\)");

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
     * This function decide whether the newly downloaded file has the same content with other files
     * It returns ture when the content is duplicate, while returns false if it is not
     *
     * @param directory The directory which saves the files (.pdf, for example)
     * @param fileName Suggest name for the newly downloaded file
     * @param dialogService To display the error and success message
     * @return true when the content of the newly downloaded file is same as the file with "similar" name,
     *         false when there is no "similar" file name or the content is different from that of files with "similar" name
     * @throws IOException Fail when the file is not exist or something wrong when reading the file
     */
    public static boolean isDuplicatedFile(Path directory, Path fileName, DialogService dialogService) throws IOException {

        Objects.requireNonNull(directory);
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(dialogService);

        String extensionSuffix = FileUtil.getFileExtension(fileName).orElse("");
        extensionSuffix = extensionSuffix.equals("") ? extensionSuffix : "." + extensionSuffix;
        String newFilename = FileUtil.getBaseName(fileName) + extensionSuffix;

        String fileNameWithoutDuplicated = eraseDuplicateMarks(FileUtil.getBaseName(fileName));
        String originalFileName = fileNameWithoutDuplicated + extensionSuffix;

        if (newFilename.equals(originalFileName)) {
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

            if (newFilename.equals(originalFileName)) {
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
        Matcher m = DUPLICATE_MARK_PATTERN.matcher(fileName);
        return m.find() ? fileName.substring(0, fileName.lastIndexOf('(') - 1) : fileName;
    }
}
