package net.sf.jabref.specialfields;

import java.util.List;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.model.entry.BibEntry;

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
     *
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
                SpecialFieldsUtils.updateField(specialField, value, be, ce, nullFieldIfValueIsTheSame);
            }
            ce.end();
            if (ce.hasEdits()) {
                frame.getCurrentBasePanel().getUndoManager().addEdit(ce);
                frame.getCurrentBasePanel().markBaseChanged();
                frame.getCurrentBasePanel().updateEntryEditorIfShowing();
                String outText;
                if (nullFieldIfValueIsTheSame || value==null) {
                    outText = specialField.getTextDone(Integer.toString(bes.size()));
                } else {
                    outText = specialField.getTextDone(value, Integer.toString(bes.size()));
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

}
