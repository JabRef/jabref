package org.jabref.gui.specialfields;

import java.util.List;
import java.util.Objects;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.specialfields.SpecialFieldsUtils;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecialFieldAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialFieldAction.class);
    private final JabRefFrame frame;
    private final SpecialField specialField;
    private final String value;
    private final boolean nullFieldIfValueIsTheSame;
    private final String undoText;
    private final DialogService dialogService;
    private final StateManager stateManager;

    /**
     * @param nullFieldIfValueIsTheSame - false also causes that doneTextPattern has two place holders %0 for the value and %1 for the sum of entries
     */
    public SpecialFieldAction(JabRefFrame frame,
                              SpecialField specialField,
                              String value,
                              boolean nullFieldIfValueIsTheSame,
                              String undoText,
                              DialogService dialogService,
                              StateManager stateManager) {
        this.frame = frame;
        this.specialField = specialField;
        this.value = value;
        this.nullFieldIfValueIsTheSame = nullFieldIfValueIsTheSame;
        this.undoText = undoText;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        try {
            List<BibEntry> bes = stateManager.getSelectedEntries();
            if ((bes == null) || bes.isEmpty()) {
                return;
            }
            NamedCompound ce = new NamedCompound(undoText);
            for (BibEntry bibEntry : bes) {
                // if (value==null) and then call nullField has been omitted as updatefield also handles value==null
                List<FieldChange> changes = SpecialFieldsUtils.updateField(
                        specialField,
                        value,
                        bibEntry,
                        nullFieldIfValueIsTheSame,
                        Globals.prefs.getSpecialFieldsPreferences().isKeywordSyncEnabled(),
                        Globals.prefs.getKeywordDelimiter());

                for (FieldChange change : changes) {
                    ce.addEdit(new UndoableFieldChange(change));
                }
            }
            ce.end();
            if (ce.hasEdits()) {
                frame.getCurrentBasePanel().getUndoManager().addEdit(ce);
                frame.getCurrentBasePanel().markBaseChanged();
                frame.getCurrentBasePanel().updateEntryEditorIfShowing();
                String outText;
                if (nullFieldIfValueIsTheSame || value == null) {
                    outText = getTextDone(specialField, Integer.toString(bes.size()));
                } else {
                    outText = getTextDone(specialField, value, Integer.toString(bes.size()));
                }
                dialogService.notify(outText);
            }

            // if user does not change anything with his action, we do not do anything either, even no output message
        } catch (Throwable ex) {
            LOGGER.error("Problem setting special fields", ex);
        }
    }

    private String getTextDone(SpecialField field, String... params) {
        Objects.requireNonNull(params);

        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field, frame.getUndoManager());

        if (field.isSingleValueField() && (params.length == 1) && (params[0] != null)) {
            // Single value fields can be toggled only
            return Localization.lang("Toggled '%0' for %1 entries", viewModel.getLocalization(), params[0]);
        } else if (!field.isSingleValueField() && (params.length == 2) && (params[0] != null) && (params[1] != null)) {
            // setting a multi value special field - the setted value is displayed, too
            String[] allParams = {viewModel.getLocalization(), params[0], params[1]};
            return Localization.lang("Set '%0' to '%1' for %2 entries", allParams);
        } else if (!field.isSingleValueField() && (params.length == 1) && (params[0] != null)) {
            // clearing a multi value specialfield
            return Localization.lang("Cleared '%0' for %1 entries", viewModel.getLocalization(), params[0]);
        } else {
            // invalid usage
            LOGGER.info("Creation of special field status change message failed: illegal argument combination.");
            return "";
        }
    }
}
