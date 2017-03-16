package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.text.JTextComponent;

import org.jabref.JabRefGUI;
import org.jabref.gui.ClipBoardManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.DOI;

/**
 * Copies the doi url to the clipboard
 */
public class CopyDoiUrlAction extends AbstractAction {

    private JTextComponent component = null;
    private String identifier;

    public CopyDoiUrlAction(String identifier) {
        super(Localization.menuTitle("Copy DOI url"));
        this.identifier = identifier;
    }

    public CopyDoiUrlAction(JTextComponent component) {
        this(component.getText());
        this.component = component;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        identifier = component == null ? identifier : component.getText();

        Optional<String> urlOptional = DOI.build(identifier).map(DOI::getURIAsASCIIString);
        if (urlOptional.isPresent()) {
            ClipBoardManager clipBoard = new ClipBoardManager();
            clipBoard.setClipboardContents(urlOptional.get());
            JabRefGUI.getMainFrame().output(Localization.lang("The link has been copied to the clipboard."));
        } else {
            JabRefGUI.getMainFrame().output(Localization.lang("Invalid DOI: '%0'.", identifier));
        }
    }
}
