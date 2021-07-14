package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JournalStatistics  {
    private Path pathToCSV;

    public JournalStatistics(Path path) {
        this.pathToCSV = path;
    }

    public boolean getTitleByISSN(int ISSN) throws IOException {
        String readString = Files.readString(pathToCSV);
        String[] strings = readString.split(";");

        for (String s:
             strings) {
            if (s.contains(String.valueOf(ISSN))) {
                return true;
            }
        }
        return false;
    }
}
