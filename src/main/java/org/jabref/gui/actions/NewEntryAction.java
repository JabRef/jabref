package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.KeyStroke;

import org.jabref.Globals;
import org.jabref.gui.EntryTypeDialog;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryAction extends MnemonicAwareAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryAction.class);

    private final JabRefFrame jabRefFrame;
    private String type; // The type of item to create.

    public NewEntryAction(JabRefFrame jabRefFrame, KeyStroke key) {
        // This action leads to a dialog asking for entry type.
        super(IconTheme.JabRefIcon.ADD_ENTRY.getIcon());
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("New entry") + "...");
        putValue(Action.ACCELERATOR_KEY, key);
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New BibTeX entry"));
    }

    public NewEntryAction(JabRefFrame jabRefFrame, String type) {
        this.jabRefFrame = jabRefFrame;
        // This action leads to the creation of a specific entry.
        putValue(Action.NAME, StringUtil.capitalizeFirst(type));
        this.type = type;
    }

    public NewEntryAction(JabRefFrame jabRefFrame, String type, KeyStroke key) {
        this.jabRefFrame = jabRefFrame;
        // This action leads to the creation of a specific entry.
        putValue(Action.NAME, StringUtil.capitalizeFirst(type));
        putValue(Action.ACCELERATOR_KEY, key);
        this.type = type;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String thisType = type;
        if (thisType == null) {
            EntryTypeDialog etd = new EntryTypeDialog(jabRefFrame);
            etd.setLocationRelativeTo(jabRefFrame);
            etd.setVisible(true);
            EntryType tp = etd.getChoice();
            if (tp == null) {
                return;
            }
            thisType = tp.getName();

            trackNewEntry(tp);
        }

        if (jabRefFrame.getBasePanelCount() > 0) {
            jabRefFrame.getCurrentBasePanel().newEntry(
                    EntryTypes.getType(thisType, jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().getMode())
                            .get());
        } else {
            LOGGER.info("Action 'New entry' must be disabled when no database is open.");
        }
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());
        Map<String, Double> measurements = new HashMap<>();

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, measurements));
    }
}
