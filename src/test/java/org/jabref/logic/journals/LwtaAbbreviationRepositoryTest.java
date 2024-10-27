package org.jabref.logic.journals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LwtaAbbreviationRepositoryTest {

    static final String[] JOURNAL_NAMES = new String[]{"international journal", "Journal of Medicine", "journal of medicine", "journal", "Physics & geobiology"};
    static final String[] ABBREVIATED_NAMES = new String[]{"int. j.", "J. Medicine", "j. medicine", "journal", "Phys. geobiol."};

    @Test
    void abbreviateJournalNameTest() throws IOException {
        String filePath = new File("").getAbsolutePath();
        filePath += "\\src\\main\\resources\\ltwa_abb.csv";

        File file = new File(filePath);
        Path path = file.toPath();

        LwtaAbbreviationRepository lwtaAbbreviationRepository = new LwtaAbbreviationRepository(path);
            for (int i = 0; i < JOURNAL_NAMES.length; i++) {
                assertEquals(lwtaAbbreviationRepository.abbreviateJournalName(JOURNAL_NAMES[i]), ABBREVIATED_NAMES[i]);
            }
    }
}
