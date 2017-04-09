package org.jabref.gui.fieldeditors;

import java.util.Set;

import org.jabref.Globals;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

public class FieldEditors {

    public static FieldEditorFX getForField(String fieldName) {
        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        if (Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD).equals(fieldName) || fieldExtras.contains(FieldProperty.DATE)) {
            // timestamp or a other field with datepicker command
            // double click AND datefield => insert the current date (today)
            //return FieldExtraComponents.getDateTimeExtraComponent(editor,
            //        fieldExtras.contains(FieldProperty.DATE), fieldExtras.contains(FieldProperty.ISO_DATE));
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            //return FieldExtraComponents.getExternalExtraComponent(panel, editor);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            // Add controls for switching between abbreviated and full journal names.
            // If this field also has a FieldContentSelector, we need to combine these.
            //return FieldExtraComponents.getJournalExtraComponent(frame, panel, editor, entry, contentSelectors, storeFieldAction);
            //} else if (!panel.getBibDatabaseContext().getMetaData().getContentSelectorValuesForField(fieldName).isEmpty()) {
            //return FieldExtraComponents.getSelectorExtraComponent(frame, panel, editor, contentSelectors, storeFieldAction);
        } else if (fieldExtras.contains(FieldProperty.DOI)) {
            return new DoiEditor(fieldName);
        } else if (fieldExtras.contains(FieldProperty.EPRINT)) {
            //return FieldExtraComponents.getEprintExtraComponent(panel, this, editor);
        } else if (fieldExtras.contains(FieldProperty.ISBN)) {
            //return FieldExtraComponents.getIsbnExtraComponent(panel, this, editor);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            //return FieldExtraComponents.getSetOwnerExtraComponent(editor, storeFieldAction);
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            //return FieldExtraComponents.getYesNoExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            //return FieldExtraComponents.getMonthExtraComponent(editor, this, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            //return FieldExtraComponents.getGenderExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            //return FieldExtraComponents.getEditorTypeExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            //return FieldExtraComponents.getPaginationExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            //return FieldExtraComponents.getTypeExtraComponent(editor, this, "patent".equalsIgnoreCase(entry.getType()));
        }

        // default
        return new SimpleEditor(fieldName);
    }
}
