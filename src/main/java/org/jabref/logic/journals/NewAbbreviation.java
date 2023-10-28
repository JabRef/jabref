package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class NewAbbreviation {
    public static void main(String[] args) throws IOException {
        Path csvFile = Paths.get("resources", "abbreviation", "ltwa_20210702.csv");
        try {
            Collection<Abbreviation> abbreviationList = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(csvFile);
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }
}
