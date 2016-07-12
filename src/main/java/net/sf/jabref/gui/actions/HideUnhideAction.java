package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.l10n.Localization;


public class HideUnhideAction extends AbstractAction {

    private FieldEditor fieldEditor;


    public HideUnhideAction(FieldEditor fieldEditor) {
        this.fieldEditor = fieldEditor;
        putValue(Action.NAME, Localization.lang("Hide/Unhide Field"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Hide/Unhide Field"));
    }

    public void setFieldEditor(FieldEditor fieldEditor) {
        this.fieldEditor = fieldEditor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fieldEditor.getEntryEditorTab().getBibEntry().toggleFieldConcealment(fieldEditor.getFieldName());
        System.out.println("! " + fieldEditor.getFieldName());
        System.out.println(fieldEditor.getEntryEditorTab().getBibEntry().toString());
    }

}
