package org.jabref.gui.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.EntryTypeDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.model.entry.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryAction.class);

    private final JabRefFrame jabRefFrame;
    /**
     * The type of the entry to create.
     */
    private final Optional<EntryType> type;


    public NewEntryAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
        this.type = Optional.empty();
    }

    public NewEntryAction(JabRefFrame jabRefFrame, EntryType type) {
        this.jabRefFrame = jabRefFrame;
        this.type = Optional.of(type);
    }

    @Override
    public void execute() {
        if (jabRefFrame.getBasePanelCount() <= 0) {
            LOGGER.error("Action 'New entry' must be disabled when no database is open.");
            return;
        }

        if (type.isPresent()) {
            jabRefFrame.getCurrentBasePanel().newEntry(type.get());
        } else {
            SwingUtilities.invokeLater(() -> {
                EntryTypeDialog typeChoiceDialog = new EntryTypeDialog(jabRefFrame);
                typeChoiceDialog.setVisible(true);
                EntryType selectedType = typeChoiceDialog.getChoice();
                if (selectedType == null) {
                    return;
                }

                trackNewEntry(selectedType);
                jabRefFrame.getCurrentBasePanel().newEntry(selectedType);
            });
        }
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());
        Map<String, Double> measurements = new HashMap<>();

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, measurements));
    }
}
