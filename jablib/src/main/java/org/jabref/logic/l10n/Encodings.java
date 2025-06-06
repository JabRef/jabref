package org.jabref.logic.l10n;

import java.nio.charset.Charset;
import java.util.List;

public class Encodings {

    public static final Charset[] ENCODINGS;
    public static final String[] ENCODINGS_DISPLAYNAMES;
    private static final List<Charset> ENCODINGS_LIST = Charset.availableCharsets().values().stream().distinct().toList();

    private Encodings() {
    }

    static {
        ENCODINGS_DISPLAYNAMES = ENCODINGS_LIST.stream().map(Charset::displayName).distinct().toArray(String[]::new);
        ENCODINGS = ENCODINGS_LIST.toArray(new Charset[0]);

    }

    public static List<Charset> getCharsets() {
        return ENCODINGS_LIST;
    }
}
