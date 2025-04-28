package org.jabref.gui.edit;

import java.util.Optional;

import javafx.scene.control.TextField;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.DOI;

/**
 * Copies the doi url to the clipboard
 */
public class CopyDoiUrlAction extends SimpleCommand {

    private final TextField component;
    private final StandardActions action;
    private final DialogService dialogService;
    private final ClipBoardManager clipBoardManager;

    public CopyDoiUrlAction(TextField component, StandardActions action, DialogService dialogService, ClipBoardManager clipBoardManager) {
        this.component = component;
        this.action = action;
        this.dialogService = dialogService;
        this.clipBoardManager = clipBoardManager;
    }

    @Override
    public void execute() {
        String identifier = component.getText();

        if (action == StandardActions.COPY_DOI_URL) {
            copy(DOI.parse(identifier).map(DOI::getURIAsASCIIString), identifier);
        } else {
            copy(DOI.parse(identifier).map(DOI::asString), identifier);
        }
    }

    private void copy(Optional<String> urlOptional, String identifier) {
        if (urlOptional.isPresent()) {
            clipBoardManager.setContent(urlOptional.get());
            dialogService.notify(Localization.lang("The link has been copied to the clipboard."));
        } else {
            dialogService.notify(Localization.lang("Invalid DOI: '%0'.", identifier));
        }
    }
}
