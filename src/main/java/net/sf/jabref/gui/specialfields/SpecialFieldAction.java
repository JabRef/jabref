package net.sf.jabref.gui.specialfields;

import java.util.List;
import java.util.Objects;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.specialfields.SpecialFieldsUtils;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.specialfields.SpecialField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SpecialFieldAction implements BaseAction {

    private final JabRefFrame frame;
    private final SpecialField specialField;
    private final String value;
    private final boolean nullFieldIfValueIsTheSame;
    private final String undoText;

    private static final Log LOGGER = LogFactory.getLog(SpecialFieldAction.class);


    /**
     * @param nullFieldIfValueIsTheSame - false also causes that doneTextPattern has two place holders %0 for the value and %1 for the sum of entries
     */
    public SpecialFieldAction(
            JabRefFrame frame,
            SpecialField specialField,
            String value,
            boolean nullFieldIfValueIsTheSame,
            String undoText) {
        this.frame = frame;
        this.specialField = specialField;
        this.value = value;
        this.nullFieldIfValueIsTheSame = nullFieldIfValueIsTheSame;
        this.undoText = undoText;
    }

    @Override
    public void action() {
        try {
            List<BibEntry> bes = frame.getCurrentBasePanel().getSelectedEntries();
            if ((bes == null) || bes.isEmpty()) {
                return;
            }
            NamedCompound ce = new NamedCompound(undoText);
            for (BibEntry be : bes) {
                // if (value==null) and then call nullField has been omitted as updatefield also handles value==null
                List<FieldChange> changes = SpecialFieldsUtils.updateField(specialField, value, be, nullFieldIfValueIsTheSame, Globals.prefs.isKeywordSyncEnabled(), Globals.prefs.getKeywordDelimiter());
                for(FieldChange change: changes) {
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
                frame.output(outText);
            } else {
                // if user does not change anything with his action, we do not do anything either
                // even no output message
            }
        } catch (Throwable ex) {
            LOGGER.error("Problem setting special fields", ex);
        }
    }

    private String getTextDone(SpecialField field, String... params) {
        Objects.requireNonNull(params);

        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field);

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
