package net.sf.jabref.bibtex;

import java.util.EnumSet;

public enum BibtexSingleFieldProperties {
    YES_NO,
    URL,
    DATEPICKER,
    JOURNAL_NAMES,
    EXTERNAL,
    DOI,
    SET_OWNER,
    MONTH,
    FILE_EDITOR,
    NUMERIC,
    PERSON_NAMES;

    public static final EnumSet<BibtexSingleFieldProperties> ALL_OPTS = EnumSet.allOf(BibtexSingleFieldProperties.class);
}
