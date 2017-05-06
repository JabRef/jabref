package org.jabref.gui.entryeditor;

import java.util.Optional;
import java.util.Set;

import javax.swing.JComponent;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.contentselector.FieldContentSelector;
import org.jabref.gui.entryeditor.EntryEditor.StoreFieldAction;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

public class FieldExtraComponents {

    private FieldExtraComponents() {
    }


    /**
     * Return a button opening a content selector for fields where one exists
     *
     * @param frame
     * @param panel
     * @param editor
     * @param contentSelectors
     * @param storeFieldAction
     * @return
     */
    public static Optional<JComponent> getSelectorExtraComponent(JabRefFrame frame, BasePanel panel, FieldEditor editor,
                                                                 Set<FieldContentSelector> contentSelectors, StoreFieldAction storeFieldAction) {
        FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, storeFieldAction, false,
                InternalBibtexFields.getFieldProperties(editor.getFieldName())
                        .contains(FieldProperty.PERSON_NAMES) ? " and " : ", ");
        contentSelectors.add(ws);
        return Optional.of(ws);
    }
}
