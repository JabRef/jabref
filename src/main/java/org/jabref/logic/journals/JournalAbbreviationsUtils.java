package org.jabref.logic.journals;

import java.nio.file.Path;

public class JournalAbbreviationsUtils {

    public static boolean isCSVFile(Path path) {
        return path.toString().endsWith(".csv");
    }
}
