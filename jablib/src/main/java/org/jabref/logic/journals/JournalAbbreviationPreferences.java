package org.jabref.logic.journals;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class JournalAbbreviationPreferences extends AbbreviationPreferences {

    private final BooleanProperty shouldEnableMscKeywordDescriptions;
    private final BooleanProperty useFJournalField;

    public JournalAbbreviationPreferences(List<String> externalLists,
                                          boolean useFJournalField,
                                          boolean shouldEnableMscKeywordDescriptions) {
        super(externalLists);
        this.shouldEnableMscKeywordDescriptions = new SimpleBooleanProperty(shouldEnableMscKeywordDescriptions);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
    }

    private JournalAbbreviationPreferences() {
        this(
                List.of(),
                true,
                false
        );
    }

    public static JournalAbbreviationPreferences getDefault() {
        return new JournalAbbreviationPreferences();
    }

    public boolean shouldUseFJournalField() {
        return useFJournalField.get();
    }

    public BooleanProperty useFJournalFieldProperty() {
        return useFJournalField;
    }

    public void setUseFJournalField(boolean useFJournalField) {
        this.useFJournalField.set(useFJournalField);
    }

    public boolean shouldEnableMscKeywordDescriptions() {
        return shouldEnableMscKeywordDescriptions.get();
    }

    public void setShouldEnableMscKeywordDescriptions(boolean value) {
        this.shouldEnableMscKeywordDescriptions.set(value);
    }

    public BooleanProperty shouldEnableMscKeywordDescriptionsProperty() {
        return shouldEnableMscKeywordDescriptions;
    }
}
