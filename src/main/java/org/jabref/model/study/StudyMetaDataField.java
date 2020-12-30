package org.jabref.model.study;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

/**
 * This enum represents the different fields in the study entry
 */
public enum StudyMetaDataField {
    STUDY_NAME(new UnknownField("name")), STUDY_RESEARCH_QUESTIONS(new UnknownField("researchQuestions")),
    STUDY_AUTHORS(StandardField.AUTHOR), STUDY_GIT_REPOSITORY(new UnknownField("gitRepositoryURL")),
    STUDY_LAST_SEARCH(new UnknownField("lastSearchDate"));

    private final Field field;

    StudyMetaDataField(Field field) {
        this.field = field;
    }

    public Field toField() {
        return this.field;
    }
}
