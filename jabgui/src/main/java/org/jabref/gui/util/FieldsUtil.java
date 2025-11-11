package org.jabref.gui.util;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;

public class FieldsUtil {

    public static final StringConverter<Field> FIELD_STRING_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(Field object) {
            if (object != null) {
                return object.getDisplayName();
            } else {
                return "";
            }
        }

        @Override
        public Field fromString(String string) {
            return FieldFactory.parseField(string);
        }
    };

    public static String getNameWithType(Field field, CliPreferences preferences, UndoManager undoManager) {
        return switch (field) {
            case SpecialField specialField ->
                    new SpecialFieldViewModel(specialField, preferences, undoManager).getLocalization()
                            + " (" + Localization.lang("Special") + ")";
            case IEEEField _ -> field.getDisplayName() + " (" + Localization.lang("IEEE") + ")";
            case InternalField _ -> field.getDisplayName() + " (" + Localization.lang("Internal") + ")";
            case UnknownField _ -> field.getDisplayName() + " (" + Localization.lang("Custom") + ")";
            case null -> throw new IllegalArgumentException("Field must not be null");
            default -> field.getDisplayName();
        };
    }
}
