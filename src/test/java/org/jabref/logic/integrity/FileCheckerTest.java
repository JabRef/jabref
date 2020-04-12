package org.jabref.logic.integrity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class FileCheckerTest {

    @Test
    void testFileChecks() {
        MetaData metaData = mock(MetaData.class);
        Mockito.when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("."));
        Mockito.when(metaData.getUserFileDirectory(any(String.class))).thenReturn(Optional.empty());
        // FIXME: must be set as checkBibtexDatabase only activates title checker based on database mode
        Mockito.when(metaData.getMode()).thenReturn(Optional.of(BibDatabaseMode.BIBTEX));

        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.FILE, ":build.gradle:gradle", metaData));
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.FILE, "description:build.gradle:gradle", metaData));
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.FILE, ":asflakjfwofja:PDF", metaData));
    }

    @Test
    void fileCheckFindsFilesRelativeToBibFile(@TempDir Path testFolder) throws IOException {
        Path bibFile = testFolder.resolve("lit.bib");
        Files.createFile(bibFile);
        Path pdfFile = testFolder.resolve("file.pdf");
        Files.createFile(pdfFile);

        BibDatabaseContext databaseContext = IntegrityCheckTest.createContext(StandardField.FILE, ":file.pdf:PDF");
        databaseContext.setDatabasePath(bibFile);

        IntegrityCheckTest.assertCorrect(databaseContext);
    }

}
