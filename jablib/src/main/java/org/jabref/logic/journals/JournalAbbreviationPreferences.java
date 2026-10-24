/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.journals;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class JournalAbbreviationPreferences extends AbbreviationPreferences {

    private final BooleanProperty useFJournalField;
    private final BooleanProperty shouldEnableMscKeywordDescriptions;

    public JournalAbbreviationPreferences(List<String> externalLists,
                                          boolean useFJournalField,
                                          boolean shouldEnableMscKeywordDescriptions) {
        super(externalLists);
        this.useFJournalField = new SimpleBooleanProperty(useFJournalField);
        this.shouldEnableMscKeywordDescriptions = new SimpleBooleanProperty(shouldEnableMscKeywordDescriptions);
    }

    private JournalAbbreviationPreferences() {
        this(
                List.of(), // externalLists
                true,      // useFJournalField
                false      // Enable MSC codes as Keyword
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
