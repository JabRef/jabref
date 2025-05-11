package org.jabref.logic.util.strings;

import java.util.HashMap;

public class UnicodeLigaturesMap extends HashMap<String, String> {

    /**
     * Ligature mapping taken from https://en.wikipedia.org/wiki/Typographic_ligature#Ligatures_in_Unicode_(Latin_alphabets)
     *
     * The mapping is bijective. In case it is ever needed to turn the extended version back to unicode ligatures, the
     * map can easily be reversed.
     */
    public UnicodeLigaturesMap() {
        put("\uA732", "AA");
        put("\uA733", "aa");
        put("\u00C6", "AE");
        put("\u00E6", "ae");
        put("\uA734", "AO");
        put("\uA735", "ao");
        put("\uA736", "AU");
        put("\uA737", "au");
        put("\uA738", "AV");
        put("\uA739", "av");
        // AV, av with bar
        put("\uA73A", "AV");
        put("\uA73B", "av");
        put("\uA73C", "AY");
        put("\uA73D", "ay");
        put("\uD83D\uDE70", "et");
        put("\uFB00", "ff");
        put("\uFB01", "fi");
        put("\uFB02", "fl");
        put("\uFB03", "ffi");
        put("\uFB04", "ffl");
        put("\uFB05", "ſt");
        put("\uFB06", "st");
        put("\u0152", "OE");
        put("\u0153", "oe");
        put("\uA74E", "OO");
        put("\uA74F", "oo");
        // we explicitly decided to exclude the conversion of ß or ẞ
        // put("\u1E9E", "ſs");
        // put("\u00DF", "ſz");
        put("\uA728", "TZ");
        put("\uA729", "tz");
        put("\u1D6B", "ue");
        put("\uA760", "VY");
        put("\uA761", "vy");

        // ligatures for phonetic transcription
        put("\u0238", "db");
        put("\u02A3", "dz");
        put("\u02A5", "dʑ");
        put("\u02A4", "dʒ");
        put("\u02A9", "fŋ");
        put("\u0132", "IJ");
        put("\u0133", "ij");
        put("\u02AA", "ls");
        put("\u02AB", "lz");
        put("\u026E", "lʒ");
        put("\u0239", "qp");
        put("\u02A6", "ts");
        put("\u02A7", "tʃ");
        put("\u02A8", "tɕ");
        put("\uAB50", "ui");
        put("\uAB51", "turned ui");
    }
}
