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

class MscCodeLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readMscCodesFromCsvFile() throws IOException {
        Path csvFile = tempDir.resolve("MSC_2020.csv");
        Files.writeString(csvFile, """
                code\ttext\tdescription
                "34B60"\t"Applications of boundary value problems involving ordinary differential equations"\t"Applications of boundary value problems involving ordinary differential equations"
                "53C99"\t"Global differential geometry"\t"None of the above, but in this section"
                """, StandardCharsets.ISO_8859_1);

        List<MscCodeEntry> entries = MscCodeLoader.readMscCodesFromCsvFile(csvFile);

        assertEquals(2, entries.size());
        assertEquals("34B60", entries.getFirst().code());
        assertEquals("Applications of boundary value problems involving ordinary differential equations", entries.getFirst().description());
        assertEquals("53C99", entries.get(1).code());
        assertEquals("None of the above, but in this section", entries.get(1).description());
    }

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
}
