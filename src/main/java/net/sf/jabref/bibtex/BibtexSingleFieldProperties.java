package net.sf.jabref.bibtex;

import java.util.EnumSet;

public enum BibtexSingleFieldProperties {
    YES_NO,
    URL,
    DATE,
    JOURNAL_NAME,
    EXTERNAL,
    DOI,
    OWNER,
    MONTH,
    FILE_EDITOR,
    NUMERIC,
    PERSON_NAMES;

    public static final EnumSet<BibtexSingleFieldProperties> ALL_OPTS = EnumSet.allOf(BibtexSingleFieldProperties.class);
}
