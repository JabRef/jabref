package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoveLinksToNotExistentFilesTest {
    private Path fileBefore;
    private BibEntry entry;
    private RemoveLinksToNotExistentFiles removeLinks;

    @BeforeEach
    void setUp(@TempDir Path bibFolder) throws IOException {
        // The folder where the files should be moved to
        Path newFileFolder = bibFolder.resolve("pdf");
        Files.createDirectory(newFileFolder);

        Path originalFileFolder = bibFolder.resolve("files");
        Path testBibFolder = bibFolder.resolve("test.bib");
        Files.createDirectory(originalFileFolder);
        fileBefore = originalFileFolder.resolve("test.pdf");
        Files.createFile(fileBefore);

        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(newFileFolder.toAbsolutePath().toString());

        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), metaData);
        Files.createFile(testBibFolder);
        databaseContext.setDatabasePath(testBibFolder);

        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath(), "");

        // Entry with one online and one normal linked file
        entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Shatakshi Sharma and Bhim Singh and Sukumar Mishra")
                .withField(StandardField.DATE, "April 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/TII.2019.2935531")
                .withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(List.of(
                    new LinkedFile("", "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912", "PDF"),
                    fileField)))
                .withField(StandardField.ISSUE, "4")
                .withField(StandardField.ISSN, "1941-0050")
                .withField(StandardField.JOURNALTITLE, "IEEE Transactions on Industrial Informatics")
                .withField(StandardField.PAGES, "2346--2356")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Economic Operation and Quality Control in PV-BES-DG-Based Autonomous System")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.KEYWORDS, "Batteries, Generators, Economics, Power quality, State of charge, Harmonic analysis, Control systems, Battery, diesel generator (DG), distributed generation, power quality, photovoltaic (PV), voltage source converter (VSC)");

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        removeLinks = new RemoveLinksToNotExistentFiles(databaseContext, filePreferences);
    }

    @Test
    void deleteFileInEntryWithMultipleFileLinks() throws IOException {
        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath(), "");
        FieldChange expectedChange = new FieldChange(entry, StandardField.FILE,
            FileFieldWriter.getStringRepresentation(List.of(
            new LinkedFile("", "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912", "PDF"),
            fileField)),
            FileFieldWriter.getStringRepresentation(new LinkedFile("", "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912", "PDF"))
        );
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Shatakshi Sharma and Bhim Singh and Sukumar Mishra")
                .withField(StandardField.DATE, "April 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/TII.2019.2935531")
                .withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(
                    new LinkedFile("", "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912", "PDF")))
                .withField(StandardField.ISSUE, "4")
                .withField(StandardField.ISSN, "1941-0050")
                .withField(StandardField.JOURNALTITLE, "IEEE Transactions on Industrial Informatics")
                .withField(StandardField.PAGES, "2346--2356")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Economic Operation and Quality Control in PV-BES-DG-Based Autonomous System")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.KEYWORDS, "Batteries, Generators, Economics, Power quality, State of charge, Harmonic analysis, Control systems, Battery, diesel generator (DG), distributed generation, power quality, photovoltaic (PV), voltage source converter (VSC)");

        Files.delete(fileBefore);
        List<FieldChange> changes = removeLinks.cleanup(entry);

        assertEquals(List.of(expectedChange), changes);
        assertEquals(expectedEntry, entry);
    }

    @Test
    void keepLinksToExistingFiles() {
        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath(), "");
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Shatakshi Sharma and Bhim Singh and Sukumar Mishra")
                .withField(StandardField.DATE, "April 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/TII.2019.2935531")
                .withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(List.of(
                    new LinkedFile("", "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912", "PDF"),
                    fileField)))
                .withField(StandardField.ISSUE, "4")
                .withField(StandardField.ISSN, "1941-0050")
                .withField(StandardField.JOURNALTITLE, "IEEE Transactions on Industrial Informatics")
                .withField(StandardField.PAGES, "2346--2356")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Economic Operation and Quality Control in PV-BES-DG-Based Autonomous System")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.KEYWORDS, "Batteries, Generators, Economics, Power quality, State of charge, Harmonic analysis, Control systems, Battery, diesel generator (DG), distributed generation, power quality, photovoltaic (PV), voltage source converter (VSC)");

        List<FieldChange> changes = removeLinks.cleanup(entry);

        assertEquals(List.of(), changes);
        assertEquals(expectedEntry, entry);
    }

    @Test
    void deleteLinkedFile() throws IOException {
        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath(), "");

        // There is only one linked file in entry
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));
        FieldChange expectedChange = new FieldChange(entry, StandardField.FILE,
            FileFieldWriter.getStringRepresentation(fileField),
            null);
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Shatakshi Sharma and Bhim Singh and Sukumar Mishra")
                .withField(StandardField.DATE, "April 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/TII.2019.2935531")
                .withField(StandardField.ISSUE, "4")
                .withField(StandardField.ISSN, "1941-0050")
                .withField(StandardField.JOURNALTITLE, "IEEE Transactions on Industrial Informatics")
                .withField(StandardField.PAGES, "2346--2356")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Economic Operation and Quality Control in PV-BES-DG-Based Autonomous System")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.KEYWORDS, "Batteries, Generators, Economics, Power quality, State of charge, Harmonic analysis, Control systems, Battery, diesel generator (DG), distributed generation, power quality, photovoltaic (PV), voltage source converter (VSC)");

        Files.delete(fileBefore);
        List<FieldChange> changes = removeLinks.cleanup(entry);

        assertEquals(List.of(expectedChange), changes);
        assertEquals(expectedEntry, entry);
    }
}
