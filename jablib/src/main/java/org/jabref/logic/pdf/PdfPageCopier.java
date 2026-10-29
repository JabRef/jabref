package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /// @return a new, empty file in `directory` named `fileName`, or a numbered variant of it when that name is taken
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

    static void copy(PDDocument source, int firstPage, int lastPage, Path target) throws IOException {
        try (PDDocument copy = new PDDocument()) {
            for (int page = firstPage; page <= lastPage; page++) {
                copy.importPage(source.getPage(page - 1));
            }
            copy.save(target.toFile());
        }
    }
}
