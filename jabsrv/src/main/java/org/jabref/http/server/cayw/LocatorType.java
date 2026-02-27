package org.jabref.http.server.cayw;

public enum LocatorType {
    PAGE("Page", "p."),
    ACT("Act", "act"),
    APPENDIX("Appendix", "app."),
    ARTICLE("Article", "art."),
    BOOK("Book", "bk."),
    CANON("Canon", "can."),
    CHAPTER("Chapter", "ch."),
    COLUMN("Column", "col."),
    EQUATION("Equation", "eq."),
    FIGURE("Figure", "fig."),
    FOLIO("Folio", "fol."),
    LINE("Line", "l."),
    LOCATION("Location", "loc."),
    NOTE("Note", "n."),
    NUMBER("Number", "no."),
    OPUS("Opus", "op."),
    PARAGRAPH("Paragraph", "par."),
    PART("Part", "pt."),
    RULE("Rule", "r."),
    SCENE("Scene", "sc."),
    SECTION("Section", "sec."),
    SUB_VERBO("Sub verbo", "s.v."),
    TABLE("Table", "tbl."),
    TITLE("Title", "tit."),
    VERSE("Verse", "v."),
    VOLUME("Volume", "vol.");

    private final String displayName;
    private final String abbreviation;

    LocatorType(String displayName, String abbreviation) {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
