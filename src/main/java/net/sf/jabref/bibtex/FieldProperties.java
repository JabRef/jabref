package net.sf.jabref.bibtex;

import java.util.EnumSet;
import java.util.Set;

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
    DOI,
    EDITOR_TYPE,
    PAGINATION,
    TYPE;

    public static final Set<FieldProperties> ALL_OPTS = EnumSet.allOf(FieldProperties.class);

}
