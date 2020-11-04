package org.jabref.gui.citationkeypattern;

import javafx.scene.control.ButtonType;

import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.metadata.MetaData;

public class CitationKeyPatternDialog extends BaseDialog<Void> {

    private final MetaData metaData;
    private final LibraryTab libraryTab;
    private final CitationKeyPatternPanel citationKeyPatternPanel;

    public CitationKeyPatternDialog(LibraryTab libraryTab) {
        this.citationKeyPatternPanel = new CitationKeyPatternPanel(libraryTab.getBibDatabaseContext());
        this.libraryTab = libraryTab;
        this.metaData = libraryTab.getBibDatabaseContext().getMetaData();
        AbstractCitationKeyPattern keyPattern = metaData.getCiteKeyPattern(Globals.prefs.getGlobalCitationKeyPattern());
        citationKeyPatternPanel.setValues(keyPattern);
        init();
    }

    private void init() {

        this.setTitle(Localization.lang("Citation key patterns"));

        this.getDialogPane().setContent(citationKeyPatternPanel.getPanel());
        this.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        this.setResultConverter(button -> {
            if (button == ButtonType.APPLY) {
                metaData.setCiteKeyPattern(citationKeyPatternPanel.getKeyPatternAsDatabaseKeyPattern());
                libraryTab.markNonUndoableBaseChanged();
            }

            return null;
        });

        // Keep this for later conversion of the library-properties
/* void storeSettings() {
        DataBaseKeyPattern newKeyPattern = new DatabaseBibtexKeyPattern(preferences.getKeyPattern());

        bibtexKeyPatternTableView.patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (!item.getEntryType().getName().equals("default")) {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addBibtexKeyPattern(item.getEntryType(), patternString);
                }
            }
        });

        if (!defaultItem.getPattern().trim().isEmpty()) {
            // we do not trim the value at the assignment to enable users to have spaces at the beginning and
            // at the end of the pattern
            newKeyPattern.setDefaultValue(defaultItemProperty.getPattern());
        }
     } */

    }
}
