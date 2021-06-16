package org.jabref.gui.util;

import javafx.util.StringConverter;

import org.jabref.gui.Globals;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;

public class FieldsUtil {

    public static StringConverter<Field> fieldStringConverter = new StringConverter<>() {
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

    public static String getNameWithType(Field field) {
        if (field instanceof SpecialField) {
            return new SpecialFieldViewModel((SpecialField) field, Globals.prefs, Globals.undoManager).getLocalization()
                    + " (" + Localization.lang("Special") + ")";
        } else if (field instanceof IEEEField) {
            return field.getDisplayName() + " (" + Localization.lang("IEEE") + ")";
        } else if (field instanceof InternalField) {
            return field.getDisplayName() + " (" + Localization.lang("Internal") + ")";
        } else if (field instanceof UnknownField) {
            return field.getDisplayName() + " (" + Localization.lang("Custom") + ")";
        } else {
            return field.getDisplayName();
        }
    }
}
