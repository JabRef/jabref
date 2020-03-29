package org.jabref.logic.util.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
}
