package org.jabref.gui.edit;

import java.util.Optional;

import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.DOI;

/**
 * Copies the doi url to the clipboard
 */
public class CopyDoiUrlAction extends SimpleCommand {

    private final TextArea component;
    private final StandardActions action;
    private final DialogService dialogService;

    public CopyDoiUrlAction(TextArea component, StandardActions action, DialogService dialogService) {
        this.component = component;
        this.action = action;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        String identifier = component.getText();

        if (action == StandardActions.COPY_DOI_URL) {
            copy(DOI.parse(identifier).map(DOI::getURIAsASCIIString), identifier);
        } else {
            copy(DOI.parse(identifier).map(DOI::getDOI), identifier);
        }
    }

    private void copy(Optional<String> urlOptional, String identifier) {
        if (urlOptional.isPresent()) {
            Globals.getClipboardManager().setContent(urlOptional.get());
            dialogService.notify(Localization.lang("The link has been copied to the clipboard."));
        } else {
            dialogService.notify(Localization.lang("Invalid DOI: '%0'.", identifier));
        }
    }
}
