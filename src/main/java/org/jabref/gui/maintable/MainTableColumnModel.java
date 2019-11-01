package org.jabref.gui.maintable;

import java.util.Objects;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;

public class MainTableColumnModel {

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

        public static Type parse(String text) {
            switch (text) {
                case "groups": return GROUPS;
                case "files": return FILES;
                case "extrafile": return EXTRAFILE;
                case "linked_id": return LINKED_IDENTIFIER;
                case "special": return SPECIALFIELD;
                case "field":
                default: return NORMALFIELD;
            }
            /* try {
                return Type.valueOf(text);
            } catch (IllegalArgumentException iae) {
                return NORMALFIELD;
            } */
        }
    }

    private final Type type;
    private final String name;

    public MainTableColumnModel(String rawColumnName) {
        Objects.requireNonNull(rawColumnName);

        String[] splitname = rawColumnName.split(ColumnPreferences.QUALIFIER_SEPARATOR);
        type = Type.parse(splitname[0]);
        if (type == Type.GROUPS || type == Type.FILES || type == Type.LINKED_IDENTIFIER) {
            name = "";
        } else {
            if (splitname.length == 1) {
                name = splitname[0]; // If default type is parsed as NORMALFIELD
            } else {
                name = splitname[1];
            }
        }
    }

    public MainTableColumnModel(Type type, String name) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(name);
        this.type = type;
        this.name = name;
    }

    public MainTableColumnModel(Type type) {
        Objects.requireNonNull(type);
        this.type = type;
        this.name = "";
    }

    public Type getType() { return type; }

    public String getName() { return name; }

    public String getDisplayName() {
        if ((type == Type.GROUPS || type == Type.FILES || type == Type.LINKED_IDENTIFIER) && name.isBlank()) {
            return type.getDisplayName();
        } else {
            return FieldsUtil.getNameWithType(FieldFactory.parseField(name));
        }
    }

    public String toString() {
        if (name.isBlank()) {
            return type.getName();
        } else {
            return type.getName() + ":" + name;
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
        return Objects.equals(name, that.name);
    }

    public int hashCode() {
        return Objects.hash(type, name);
    }
}
