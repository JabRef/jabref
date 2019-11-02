package org.jabref.gui.maintable;

import java.util.Objects;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the full internal name of a column in the main table. Consists of two parts:
 * The type of the column and a qualifier, like the field name to be displayed in the column.
 */
public class MainTableColumnModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableColumnModel.class);

    public enum Type {
        EXTRAFILE("extrafile", Localization.lang("File type")),
        FILES("files", Localization.lang("Linked files")),
        GROUPS("groups", Localization.lang("Groups")),
        LINKED_IDENTIFIER("linked_id", Localization.lang("Linked identifiers")),
        NORMALFIELD("field"),
        SPECIALFIELD("special", Localization.lang("Special"));

        private String name;
        private String displayName;

        Type(String name) {
            this.name = name;
            this.displayName = name;
        }

        Type(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Type fromString(String text) {
            for (Type type : Type.values()) {
                if (type.getName().equals(text)) {
                    return type;
                }
            }
            LOGGER.warn(Localization.lang("Column type %0 is unknown.", text));
            return NORMALFIELD;
        }
    }

    private final Type type;
    private final String qualifier;

    public MainTableColumnModel(String rawColumnName) {
        Objects.requireNonNull(rawColumnName);

        String[] splittedName = rawColumnName.split(JabRefPreferences.COLUMNS_QUALIFIER_DELIMITER.toString());

        type = Type.fromString(splittedName[0]);

        if (type == Type.NORMALFIELD || type == Type.SPECIALFIELD || type == Type.EXTRAFILE) {
            if (splittedName.length == 1) {
                qualifier = splittedName[0]; // On default the rawColumnName is parsed as NORMALFIELD
            } else {
                qualifier = splittedName[1];
            }
        } else {
            qualifier = "";
        }
    }

    public MainTableColumnModel(Type type, String qualifier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(qualifier);
        this.type = type;
        this.qualifier = qualifier;
    }

    public MainTableColumnModel(Type type) {
        Objects.requireNonNull(type);
        this.type = type;
        this.qualifier = "";
    }

    public Type getType() { return type; }

    public String getQualifier() { return qualifier; }

    public String getDisplayName() {
        if ((type == Type.GROUPS || type == Type.FILES || type == Type.LINKED_IDENTIFIER) && qualifier.isBlank()) {
            return type.getDisplayName();
        } else {
            return FieldsUtil.getNameWithType(FieldFactory.parseField(qualifier));
        }
    }

    public String toString() {
        if (qualifier.isBlank()) {
            return type.getName();
        } else {
            return type.getName() + ":" + qualifier;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MainTableColumnModel that = (MainTableColumnModel) o;

        if (type != that.type) {
            return false;
        }
        return Objects.equals(qualifier, that.qualifier);
    }

    public int hashCode() {
        return Objects.hash(type, qualifier);
    }
}
