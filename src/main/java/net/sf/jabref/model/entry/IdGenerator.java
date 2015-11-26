package net.sf.jabref.model.entry;

import java.text.NumberFormat;

/**
 * IDs are at least 8 digit long. The lowest ID is 00000000, the next would be 00000001.
 * <p/>
 * The generator is thread safe!
 */
public class IdGenerator {

    private static final NumberFormat idFormat;

    static {
        idFormat = NumberFormat.getInstance();
        IdGenerator.idFormat.setMinimumIntegerDigits(8);
        IdGenerator.idFormat.setGroupingUsed(false);
    }

    private static int idCounter;


    public static synchronized String next() {
        String result = idFormat.format(idCounter);
        idCounter++;
        return result;
    }

    public static int getMinimumIntegerDigits() {
        return idFormat.getMinimumIntegerDigits();
    }
}
