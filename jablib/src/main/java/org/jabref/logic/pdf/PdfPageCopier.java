package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.io.FileNameUniqueness;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
final class PdfPageCopier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPageCopier.class);

    private PdfPageCopier() {
    }

    /// @return a new PDF in `directory` named `fileName`, or a numbered variant of it when that name is taken, holding `firstPage` to `lastPage` of `source`
    /// @throws IOException if copying fails, in which case no new file is left behind
    static Path copyToNewFile(PDDocument source, int firstPage, int lastPage, Path directory, String fileName) throws IOException {
        Path target = createUniqueFile(directory, fileName);
        try {
            copy(source, firstPage, lastPage, target);
        } catch (IOException exception) {
            deleteQuietly(target, exception);
            throw exception;
        }
        return target;
    }

    /// Deletes `files` after `failure`, adding any failure to delete them to it
    static void deleteQuietly(List<Path> files, IOException failure) {
        files.forEach(file -> deleteQuietly(file, failure));
    }

    private static void deleteQuietly(Path file, IOException failure) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException deleteFailure) {
            failure.addSuppressed(deleteFailure);
        }
    }

    static Path createUniqueFile(Path directory, String fileName) throws IOException {
        while (true) {
            Path target = directory.resolve(FileNameUniqueness.generateUniqueFileName(directory, fileName));
            try {
                return Files.createFile(target);
            } catch (FileAlreadyExistsException exception) {
                LOGGER.debug("{} was created by another task, trying the next name", target, exception);
            }
        }
    }

    private static void copy(PDDocument source, int firstPage, int lastPage, Path target) throws IOException {
        try (PDDocument copy = new PDDocument()) {
            for (int page = firstPage; page <= lastPage; page++) {
                copy.importPage(source.getPage(page - 1));
            }
            copy.save(target.toFile());
        }
    }
}
