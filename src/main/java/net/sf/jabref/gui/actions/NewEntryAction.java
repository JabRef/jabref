package net.sf.jabref.gui.actions;

import net.sf.jabref.gui.*;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class NewEntryAction extends MnemonicAwareAction {
    private static final Log LOGGER = LogFactory.getLog(NewEntryAction.class);

    private final JabRefFrame jabRefFrame;
    String type; // The type of item to create.
    KeyStroke keyStroke; // Used for the specific instances.

    public NewEntryAction(JabRefFrame jabRefFrame, KeyStroke key) {
        // This action leads to a dialog asking for entry type.
        super(IconTheme.JabRefIcon.ADD_ENTRY.getIcon());
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("New entry"));
        putValue(Action.ACCELERATOR_KEY, key);
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New BibTeX entry"));
    }

    public NewEntryAction(JabRefFrame jabRefFrame, String type_) {
        this.jabRefFrame = jabRefFrame;
        // This action leads to the creation of a specific entry.
        putValue(Action.NAME, StringUtil.capitalizeFirst(type_));
        type = type_;
    }

    public NewEntryAction(JabRefFrame jabRefFrame, String type_, KeyStroke key) {
        this.jabRefFrame = jabRefFrame;
        // This action leads to the creation of a specific entry.
        putValue(Action.NAME, StringUtil.capitalizeFirst(type_));
        putValue(Action.ACCELERATOR_KEY, key);
        type = type_;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String thisType = type;
        if (thisType == null) {
            EntryTypeDialog etd = new EntryTypeDialog(jabRefFrame);
            Util.placeDialog(etd, jabRefFrame);
            etd.setVisible(true);
            BibtexEntryType tp = etd.getChoice();
            if (tp == null) {
                return;
            }
            thisType = tp.getName();
        }

        if (jabRefFrame.tabbedPane.getTabCount() > 0) {
            ((BasePanel) jabRefFrame.tabbedPane.getSelectedComponent())
                    .newEntry(BibtexEntryType.getType(thisType));
        } else {
            LOGGER.info("Action 'New entry' must be disabled when no " + "database is open.");
        }
    }
}
