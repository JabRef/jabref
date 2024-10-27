package org.jabref.model.entry;

import java.util.Optional;

import org.jabref.logic.bibtex.FieldWriter;
/**
 * Language identifiers based on BibLaTeX manual specifications.
 * See the BibLaTeX documentation for full details:
 * <a href="http://mirrors.ctan.org/macros/latex/contrib/biblatex/doc/biblatex.pdfhangelo">BibLaTeX manual</a>
 */

public enum Langid {
    BASQUE("Basque", "basque"),
    BULGARIAN("Bulgarian", "bulgarian"),
    CATALAN("Catalan", "catalan"),
    CROATIAN("Croatian", "croatian"),
    CZECH("Czech", "czech"),
    DANISH("Danish", "danish"),
    AMERICAN("American", "american"),
    USENGLISH("US English", "USenglish"),
    ENGLISH("English", "english"),
    BRITISH("British", "british"),
    UKENGLISH("UK English", "UKenglish"),
    CANADIAN("Canadian", "canadian"),
    AUSTRALIAN("Australian", "australian"),
    NEWZEALAND("New Zealand", "newzealand"),
    ESTONIAN("Estonian", "estonian"),
    FINNISH("Finnish", "finnish"),
    FRENCH("French", "french"),
    GERMAN("German", "german"),
    AUSTRIAN("Austrian", "austrian"),
    SWISSGERMAN("Swiss German", "swissgerman"),
    NGERMAN("German (New)", "ngerman"),
    NAUSTRIAN("Austrian (New)", "naustrian"),
    NSWISSGERMAN("Swiss German (New)", "nswissgerman"),
    GREEK("Greek", "greek"),
    MAGYAR("Hungarian", "hungarian"),
    HUNGARIAN("Hungarian", "hungarian"),
    ICELANDIC("Icelandic", "icelandic"),
    ITALIAN("Italian", "italian"),
    LATVIAN("Latvian", "latvian"),
    LITHUANIAN("Lithuanian", "lithuanian"),
    MARATHI("Marathi", "marathi"),
    NORSK("Norwegian (Bokmål)", "norsk"),
    NYNORSK("Norwegian (Nynorsk)", "nynorsk"),
    POLISH("Polish", "polish"),
    BRAZIL("Portuguese (Brazilian)", "brazil"),
    PORTUGUESE("Portuguese", "portuguese"),
    PORTUGES("Portuguese (alt)", "portuges"),
    ROMANIAN("Romanian", "romanian"),
    RUSSIAN("Russian", "russian"),
    SERBIAN("Serbian (Latin)", "serbian"),
    SERBIANC("Serbian (Cyrillic)", "serbianc"),
    SLOVAK("Slovak", "slovak"),
    SLOVENE("Slovene", "slovene"),
    SLOVENIAN("Slovenian", "slovenian"),
    SPANISH("Spanish", "spanish"),
    SWEDISH("Swedish", "swedish"),
    TURKISH("Turkish", "turkish"),
    UKRAINIAN("Ukrainian", "ukrainian");


    private final String name;
    private final String langid;

    Langid(String name, String langid) {
        this.name = name;
        this.langid = langid;
    }

    public String getLangid() {
        return langid;
    }

    public String getName() {
        return name;
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
