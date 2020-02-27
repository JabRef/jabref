package org.jabref.gui.util;

import javafx.util.StringConverter;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldsUtil.class);

    public static String getNameWithType(Field field) {
        if (field instanceof SpecialField) {

            String fieldName;
            SpecialField specialField = (SpecialField) field;

            if (specialField == SpecialField.PRINTED) {
                fieldName = Localization.lang("Printed");
            } else if (specialField == SpecialField.QUALITY) {
                fieldName = Localization.lang("Quality");
            } else if (specialField == SpecialField.RANKING) {
                fieldName = Localization.lang("Ranking");
            } else if (specialField == SpecialField.PRIORITY) {
                fieldName = Localization.lang("Priority");
            } else if (specialField == SpecialField.RELEVANCE) {
                fieldName = Localization.lang("Relevance");
            } else if (specialField == SpecialField.READ_STATUS) {
                fieldName = Localization.lang("Read status");
            } else {
                fieldName = Localization.lang("Unknown");
                LOGGER.warn("Unknown special field '" + field.getName() + "'.");
            }

            return fieldName + " (" + Localization.lang("Special") + ")";

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
