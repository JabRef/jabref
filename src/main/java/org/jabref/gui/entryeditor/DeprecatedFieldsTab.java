package org.jabref.gui.entryeditor;

import java.util.List;

import javafx.scene.control.Tooltip;

import org.jabref.gui.IconTheme;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class DeprecatedFieldsTab extends FieldsEditorTab {
    public DeprecatedFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders) {
        super(false, databaseContext, suggestionProviders);

        setText(Localization.lang("Deprecated fields"));
        setTooltip(new Tooltip(Localization.lang("Show deprecated BibTeX fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }

    @Override
    protected List<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        return entryType.getDeprecatedFields();
    }
}
