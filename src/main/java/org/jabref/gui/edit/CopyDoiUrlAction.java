package org.jabref.gui.edit;

import java.util.Optional;

import javafx.scene.control.TextArea;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.DOI;

/**
 * Copies the doi url to the clipboard
 */
public class CopyDoiUrlAction extends SimpleCommand {

    private TextArea component;

    public CopyDoiUrlAction(TextArea component) {
        this.component = component;
    }

    @Override
    public void execute() {
        String identifier = component.getText();

        Optional<String> urlOptional = DOI.parse(identifier).map(DOI::getURIAsASCIIString);
        if (urlOptional.isPresent()) {
            Globals.getClipboardManager().setContent(urlOptional.get());
            JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("The link has been copied to the clipboard."));
        } else {
            JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Invalid DOI: '%0'.", identifier));
        }
    }
}
