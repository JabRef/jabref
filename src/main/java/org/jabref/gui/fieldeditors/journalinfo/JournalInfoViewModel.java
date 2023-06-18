package org.jabref.gui.fieldeditors.journalinfo;

import javafx.beans.property.ReadOnlyStringWrapper;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.l10n.Localization;

public class JournalInfoViewModel extends AbstractViewModel {
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();

    public JournalInfoViewModel() {
        heading.set(Localization.lang("Journal Information"));
    }

    public String getHeading() {
        return heading.get();
    }

    public ReadOnlyStringWrapper headingProperty() {
        return heading;
    }
}
