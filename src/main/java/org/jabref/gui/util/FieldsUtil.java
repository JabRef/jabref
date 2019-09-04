package org.jabref.gui.util;

import java.util.Collections;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.UnknownField;

public class FieldsUtil {

    public static String getNameWithType(Field field) {
        if (field instanceof SpecialField) {
            return field.getDisplayName() + " (" + Localization.lang("Special") + ")";
        } else if (field instanceof IEEEField) {
            return field.getDisplayName() + " (" + Localization.lang("IEEE") + ")";
        } else if (field instanceof InternalField) {
            return field.getDisplayName() + " (" + Localization.lang("Internal") + ")";
        } else if (field instanceof UnknownField) {
            return field.getDisplayName() + " (" + Localization.lang("Custom") + ")";
        } else if (field instanceof ExtraFilePseudoField) {
            return field.getDisplayName() + " (" + Localization.lang("File type") + ")";
        } else {
            return field.getDisplayName();
        }
    }

    public static class ExtraFilePseudoField implements Field {

        String name;

        public ExtraFilePseudoField(String name) {
            this.name = name;
        }

        @Override
        public Set<FieldProperty> getProperties() {
            return Collections.emptySet();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isStandardField() {
            return false;
        }
    }
}
