package org.jabref.gui.entryeditor;

import java.util.List;

import javafx.scene.control.Tooltip;

import org.jabref.gui.IconTheme;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OptionalFields2Tab extends FieldsEditorTab {
    public OptionalFields2Tab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders) {
        super(true, databaseContext, suggestionProviders);

        setText(Localization.lang("Optional fields 2"));
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }

    @Override
    protected List<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        return entryType.getSecondaryOptionalNotDeprecatedFields();
    }
}
