package net.sf.jabref.bibtex;

import java.util.EnumSet;

public enum FieldProperties {
    YES_NO,
    URL,
    DATE,
    JOURNAL_NAME,
    EXTERNAL,
    BROWSE,
    OWNER,
    MONTH,
    FILE_EDITOR,
    NUMERIC,
    PERSON_NAMES,
    INTEGER,
    GENDER,
    LANGUAGE,
    LANG_ID,
    DOI;

    public static final EnumSet<FieldProperties> ALL_OPTS = EnumSet
            .allOf(FieldProperties.class);

}
