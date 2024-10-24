package org.jabref.model.entry;

import java.util.Optional;

import org.jabref.logic.bibtex.FieldWriter;

public enum Langid {
    BASQUE("basque"),
    BULGARIAN("bulgarian"),
    CATALAN("catalan"),
    CROATIAN("croatian"),
    CZECH("czech"),
    DANISH("danish"),
    AMERICAN("american"),
    USENGLISH("american"),
    ENGLISH("english"),
    BRITISH("british"),
    UKENGLISH("british"),
    CANADIAN("canadian"),
    AUSTRALIAN("australian"),
    NEWZEALAND("newzealand"),
    ESTONIAN("estonian"),
    FINNISH("finnish"),
    FRENCH("french"),
    GERMAN("german"),
    AUSTRIAN("austrian"),
    SWISSGERMAN("swissgerman"),
    NGERMAN("ngerman"),
    NAUSTRIAN("naustrian"),
    NSWISSGERMAN("nswissgerman"),
    GREEK("greek"),
    MAGYAR("hungarian"),
    HUNGARIAN("hungarian"),
    ICELANDIC("icelandic"),
    ITALIAN("italian"),
    LATVIAN("latvian"),
    LITHUANIAN("lithuanian"),
    MARATHI("marathi"),
    NORSK("norsk"),
    NYNORSK("nynorsk"),
    POLISH("polish"),
    BRAZIL("brazil"),
    PORTUGUESE("portuguese"),
    PORTUGES("portuguese"),
    ROMANIAN("romanian"),
    RUSSIAN("russian"),
    SERBIAN("serbian"),
    SERBIANC("serbianc"),
    SLOVAK("slovak"),
    SLOVENE("slovenian"),
    SLOVENIAN("slovenian"),
    SPANISH("spanish"),
    SWEDISH("swedish"),
    TURKISH("turkish"),
    UKRAINIAN("ukrainian");

    private final String langid;

    Langid(String langid) {
        this.langid = langid;
    }

    public String getLangid() {
        return langid;
    }

    public static Optional<Langid> getByLangid(String id) {
        for (Langid lang : Langid.values()) {
            if (lang.langid.equalsIgnoreCase(id)) {
                return Optional.of(lang);
            }
        }
        return Optional.empty();
    }

    public static Optional<Langid> parse(String value) {
        return Langid.getByLangid(value.trim().toLowerCase());
    }

    public String getJabRefFormat() {
        return (FieldWriter.BIBTEX_STRING_START_END_SYMBOL + "%s" + FieldWriter.BIBTEX_STRING_START_END_SYMBOL).formatted(langid);
    }
}
