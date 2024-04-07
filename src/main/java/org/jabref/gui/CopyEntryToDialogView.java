package org.jabref.gui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

public class CopyEntryToDialogView extends FXDialog {
    ButtonType copySelectedEntryOnly = new ButtonType("Copy selected entry only", ButtonBar.ButtonData.OK_DONE);
    ButtonType copySelectedEntryWithCrossReferencedEntries = new ButtonType("Copy cross references", ButtonBar.ButtonData.OK_DONE);
    ButtonType copySelectedEntryWithCrossReferencedEntriesRecursively = new ButtonType("Copy cross references recursively", ButtonBar.ButtonData.OK_DONE);

    public CopyEntryToDialogView() {
        super(AlertType.CONFIRMATION, Localization.lang("Copy entry to library"), true);
        setHeaderText(null);
        getDialogPane().setMinHeight(170);
        setResizable(true);
        setContentText("""
                The selected entry cross references 4 other entries.

                Do you want to copy all of them or just the selected one?

                * Copying cross-references recursively means copying references of the selected entry, and references of its references, until all dependant entries are included.""");
        getDialogPane().getButtonTypes().setAll(copySelectedEntryOnly, copySelectedEntryWithCrossReferencedEntries, copySelectedEntryWithCrossReferencedEntriesRecursively, new ButtonType("Cancel", ButtonBar.ButtonData.LEFT));
        getDialogPane().getButtonTypes().stream()
                       .map(getDialogPane()::lookupButton)
                       .forEach(btn-> {
                           ButtonBar.setButtonUniformSize(btn, false);
                           ((Region) btn).setMinWidth(120);
                       });
    }
}
