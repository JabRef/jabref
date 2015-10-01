package net.sf.jabref.logic.l10n;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Encodings {

    public static final String[] ALL_ENCODINGS =
            new String[] {"ISO8859-1", "UTF-8", "UTF-16", "ASCII", "CP1250", "CP1251", "CP1252",
                    "CP1253", "CP1254", "CP1257", "SJIS",
                    "KOI8-R", // Cyrillic
                    "EUC-JP", // Added Japanese encodings
                    "Big5", "Big5-HKSCS", "GBK", "ISO8859-2", "ISO8859-3", "ISO8859-4", "ISO8859-5",
                    "ISO8859-6", "ISO8859-7", "ISO8859-8", "ISO8859-9", "ISO8859-13", "ISO8859-15"};

    public static final String[] ENCODINGS;
    public static final Map<String, String> ENCODING_NAMES_LOOKUP;

    static {
        // Build list of encodings, by filtering out all that are not supported
        // on this system:
        List<String> encodings = new ArrayList<>();
        for (String ALL_ENCODING : ALL_ENCODINGS) {
            if (Charset.isSupported(ALL_ENCODING)) {
                encodings.add(ALL_ENCODING);
            }
        }
        ENCODINGS = encodings.toArray(new String[encodings.size()]);
        // Build a map for translating Java encoding names into common encoding names:
        ENCODING_NAMES_LOOKUP = new HashMap<>();

        // old mispelled encoding mappings, kept for compatibility with v2.10 downwards
        ENCODING_NAMES_LOOKUP.put("Cp1250", "windows-1250");
        ENCODING_NAMES_LOOKUP.put("Cp1251", "windows-1251");
        ENCODING_NAMES_LOOKUP.put("Cp1252", "windows-1252");
        ENCODING_NAMES_LOOKUP.put("Cp1253", "windows-1253");
        ENCODING_NAMES_LOOKUP.put("Cp1254", "windows-1254");
        ENCODING_NAMES_LOOKUP.put("Cp1257", "windows-1257");
        ENCODING_NAMES_LOOKUP.put("ISO8859_1", "ISO-8859-1");
        ENCODING_NAMES_LOOKUP.put("ISO8859_2", "ISO-8859-2");
        ENCODING_NAMES_LOOKUP.put("ISO8859_3", "ISO-8859-3");
        ENCODING_NAMES_LOOKUP.put("ISO8859_4", "ISO-8859-4");
        ENCODING_NAMES_LOOKUP.put("ISO8859_5", "ISO-8859-5");
        ENCODING_NAMES_LOOKUP.put("ISO8859_6", "ISO-8859-6");
        ENCODING_NAMES_LOOKUP.put("ISO8859_7", "ISO-8859-7");
        ENCODING_NAMES_LOOKUP.put("ISO8859_8", "ISO-8859-8");
        ENCODING_NAMES_LOOKUP.put("ISO8859_9", "ISO-8859-9");
        ENCODING_NAMES_LOOKUP.put("ISO8859_13", "ISO-8859-13");
        ENCODING_NAMES_LOOKUP.put("ISO8859_15", "ISO-8859-15");
        ENCODING_NAMES_LOOKUP.put("KOI8_R", "KOI8-R");
        ENCODING_NAMES_LOOKUP.put("UTF8", "UTF-8");
        ENCODING_NAMES_LOOKUP.put("Big5_HKSCS", "Big5-HKSCS");
        ENCODING_NAMES_LOOKUP.put("EUC_JP", "EUC-JP");

        // correct encoding name mappings
        ENCODING_NAMES_LOOKUP.put("CP1250", "windows-1250");
        ENCODING_NAMES_LOOKUP.put("CP1251", "windows-1251");
        ENCODING_NAMES_LOOKUP.put("CP1252", "windows-1252");
        ENCODING_NAMES_LOOKUP.put("CP1253", "windows-1253");
        ENCODING_NAMES_LOOKUP.put("CP1254", "windows-1254");
        ENCODING_NAMES_LOOKUP.put("CP1257", "windows-1257");
        ENCODING_NAMES_LOOKUP.put("ISO8859-1", "ISO-8859-1");
        ENCODING_NAMES_LOOKUP.put("ISO8859-2", "ISO-8859-2");
        ENCODING_NAMES_LOOKUP.put("ISO8859-3", "ISO-8859-3");
        ENCODING_NAMES_LOOKUP.put("ISO8859-4", "ISO-8859-4");
        ENCODING_NAMES_LOOKUP.put("ISO8859-5", "ISO-8859-5");
        ENCODING_NAMES_LOOKUP.put("ISO8859-6", "ISO-8859-6");
        ENCODING_NAMES_LOOKUP.put("ISO8859-7", "ISO-8859-7");
        ENCODING_NAMES_LOOKUP.put("ISO8859-8", "ISO-8859-8");
        ENCODING_NAMES_LOOKUP.put("ISO8859-9", "ISO-8859-9");
        ENCODING_NAMES_LOOKUP.put("ISO8859-13", "ISO-8859-13");
        ENCODING_NAMES_LOOKUP.put("ISO8859-15", "ISO-8859-15");
        ENCODING_NAMES_LOOKUP.put("KOI8-R", "KOI8-R");
        ENCODING_NAMES_LOOKUP.put("UTF-8", "UTF-8");
        ENCODING_NAMES_LOOKUP.put("Big5-HKSCS", "Big5-HKSCS");
        ENCODING_NAMES_LOOKUP.put("EUC-JP", "EUC-JP");

        ENCODING_NAMES_LOOKUP.put("UTF-16", "UTF-16");
        ENCODING_NAMES_LOOKUP.put("SJIS", "Shift_JIS");
        ENCODING_NAMES_LOOKUP.put("GBK", "GBK");
        ENCODING_NAMES_LOOKUP.put("Big5", "Big5");
        ENCODING_NAMES_LOOKUP.put("ASCII", "US-ASCII");
    }
}
