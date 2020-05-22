package org.jabref.model.entry;

import java.text.NumberFormat;

/**
 * IDs are at least 8 digit long. The lowest ID is 00000000, the next would be 00000001.
 * <p/>
 * The generator is thread safe!
 */
public class IdGenerator {

    private static final NumberFormat ID_FORMAT;

    static {
        ID_FORMAT = NumberFormat.getInstance();
        IdGenerator.ID_FORMAT.setMinimumIntegerDigits(8);
        IdGenerator.ID_FORMAT.setGroupingUsed(false);
    }

    private static int idCounter;

    private IdGenerator() {
    }

    public static synchronized String next() {
        String result = ID_FORMAT.format(idCounter);
        idCounter++;
        return result;
    }
}
