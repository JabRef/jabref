package org.jabref.gui.specialfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecialFieldAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialFieldAction.class);
    private final Supplier<LibraryTab> tabSupplier;
    private final SpecialField specialField;
    private final String value;
    private final boolean nullFieldIfValueIsTheSame;
    private final String undoText;
    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final UndoManager undoManager;
    private final StateManager stateManager;

    /**
     * @param nullFieldIfValueIsTheSame - false also causes that doneTextPattern has two place holders %0 for the value and %1 for the sum of entries
     */
    public SpecialFieldAction(Supplier<LibraryTab> tabSupplier,
                              SpecialField specialField,
                              String value,
                              boolean nullFieldIfValueIsTheSame,
                              String undoText,
                              DialogService dialogService,
                              CliPreferences preferences,
                              UndoManager undoManager,
                              StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.specialField = specialField;
        this.value = value;
        this.nullFieldIfValueIsTheSame = nullFieldIfValueIsTheSame;
        this.undoText = undoText;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.undoManager = undoManager;
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
            NamedCompoundEdit compoundEdit = new NamedCompoundEdit(undoText);
            List<BibEntry> besCopy = new ArrayList<>(bes);
            for (BibEntry bibEntry : besCopy) {
                // if (value==null) and then call nullField has been omitted as updatefield also handles value==null
                Optional<FieldChange> change = UpdateField.updateField(bibEntry, specialField, value, nullFieldIfValueIsTheSame);

                change.ifPresent(fieldChange -> compoundEdit.addEdit(new UndoableFieldChange(fieldChange)));
            }
            compoundEdit.end();
            if (compoundEdit.hasEdits()) {
                undoManager.addEdit(compoundEdit);
                tabSupplier.get().markBaseChanged();
                String outText;
                if (nullFieldIfValueIsTheSame || value == null) {
                    outText = getTextDone(specialField, Integer.toString(bes.size()));
                    getTextDone(specialField);
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

    // @formatter:off
    private String getTextDone(SpecialField field, String @NonNull... params) {
    // @formatter:on

        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field, preferences, undoManager);

        if (field.isSingleValueField() && (params.length == 1)) {
            // Single value fields can be toggled only
            return Localization.lang("Toggled '%0' for %1 entries", viewModel.getLocalization(), params[0]);
        } else if (!field.isSingleValueField() && (params.length == 2)) {
            // setting a multi value special field - the set value is displayed, too
            return Localization.lang("Set '%0' to '%1' for %2 entries", viewModel.getLocalization(), params[0], params[1]);
        } else if (!field.isSingleValueField() && (params.length == 1)) {
            // clearing a multi value specialfield
            return Localization.lang("Cleared '%0' for %1 entries", viewModel.getLocalization(), params[0]);
        } else {
            // invalid usage
            LOGGER.info("Creation of special field status change message failed: illegal argument combination.");
            return "";
        }
    }
}
