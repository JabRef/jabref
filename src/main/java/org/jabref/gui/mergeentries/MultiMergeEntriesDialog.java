package org.jabref.gui.mergeentries;

import java.util.Optional;
import java.util.function.Supplier;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class MultiMergeEntriesDialog extends BaseDialog<BibEntry> {

    private final MultiMergeEntries multiMergeEntries;

    public MultiMergeEntriesDialog(FieldContentFormatterPreferences fieldContentFormatterPreferences) {
        multiMergeEntries = new MultiMergeEntries(fieldContentFormatterPreferences);

        init();
    }

    private void init() {
        this.getDialogPane().setContent(multiMergeEntries);

        // Create buttons
        ButtonType replaceEntries = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, replaceEntries);
        this.setResultConverter(buttonType -> {
            if (buttonType.equals(replaceEntries)) {
                return multiMergeEntries.getMergeEntry();
            } else {
                return null;
            }
        });
    }

    public void addEntry(String title, BibEntry entry) {
        multiMergeEntries.addEntry(title, entry);
    }

    public void addEntry(String title, Supplier<Optional<BibEntry>> entrySupplier) {
        multiMergeEntries.addEntry(title, entrySupplier);
    }
}
