package org.jabref.logic.msc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.MscCodeUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MscCodeLoaderTest {

    @TempDir
    Path tempDir;


    @Test
    void convertCsvToMvStore() throws IOException {
        Path csvFile = tempDir.resolve("MSC_2020.csv");
        Files.writeString(csvFile, """
                code\ttext\tdescription
                "34B60"\t"Applications of boundary value problems involving ordinary differential equations"\t"Applications of boundary value problems involving ordinary differential equations"
                "53C99"\t"Global differential geometry"\t"None of the above, but in this section"
                """, StandardCharsets.ISO_8859_1);
        Path mvStoreFile = tempDir.resolve("msc-list.mv");

        MscCodeLoader.convertCsvToMvStore(csvFile, mvStoreFile);

        MscCodeRepository repository = new MscCodeRepository(mvStoreFile);

        assertEquals(
                "Applications of boundary value problems involving ordinary differential equations",
                repository.getDescription("34B60").get());
        assertEquals("None of the above, but in this section", repository.getDescription("53C99").get());
    }

    @Test
    void loadMscCodeRepositoryFromCsvUrl() throws IOException, org.jabref.logic.shared.exception.MscCodeLoadingException {
        Path csvFile = tempDir.resolve("MSC_2020.csv");
        Files.writeString(csvFile, """
                code\ttext\tdescription
                "34B60"\t"Applications of boundary value problems involving ordinary differential equations"\t"Applications of boundary value problems involving ordinary differential equations"
                """, StandardCharsets.ISO_8859_1);

        MscCodeRepository repository = MscCodeUtils.loadMscCodeRepositoryFromCsv(csvFile.toUri().toURL()).get();

        assertEquals("Applications of boundary value problems involving ordinary differential equations", repository.getDescription("34B60").get());
    }

    @Test
    void convertCsvToMvStoreRemovesStaleEntries() throws IOException {
        Path csvFile1 = tempDir.resolve("MSC_1.csv");
        Files.writeString(csvFile1, """
                code\ttext\tdescription
                "11-XX"\t"Number theory"\t"Number theory"
                "12-XX"\t"Field theory and polynomials"\t"Field theory and polynomials"
                """, StandardCharsets.ISO_8859_1);
        Path mvStoreFile = tempDir.resolve("msc-list.mv");

        MscCodeLoader.convertCsvToMvStore(csvFile1, mvStoreFile);

        Path csvFile2 = tempDir.resolve("MSC_2.csv");
        Files.writeString(csvFile2, """
                code\ttext\tdescription
                "11-XX"\t"Number theory updated"\t"Number theory updated"
                """, StandardCharsets.ISO_8859_1);

        MscCodeLoader.convertCsvToMvStore(csvFile2, mvStoreFile);

        MscCodeRepository repository = new MscCodeRepository(mvStoreFile);
        assertEquals("Number theory updated", repository.getDescription("11-XX").get());
        // This should fail currently because 12-XX is still there
        assertTrue(repository.getDescription("12-XX").isEmpty(), "Stale entry 12-XX should be removed");
    }
}
