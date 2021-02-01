package org.jabref.logic.l10n;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class Encodings {

    public static final Charset[] ENCODINGS;
    public static final String[] ENCODINGS_DISPLAYNAMES;
    private static List<Charset> encodingsList = Charset.availableCharsets().values().stream().distinct()
                                                        .collect(Collectors.toList());

    private Encodings() {
    }

    static {
        List<String> encodingsStringList = encodingsList.stream().map(Charset::displayName).distinct()
                                                        .collect(Collectors.toList());
        ENCODINGS = encodingsList.toArray(new Charset[encodingsList.size()]);
        ENCODINGS_DISPLAYNAMES = encodingsStringList.toArray(new String[encodingsStringList.size()]);
    }

    public static List<Charset> getCharsets() {
        return encodingsList;
    }
}
