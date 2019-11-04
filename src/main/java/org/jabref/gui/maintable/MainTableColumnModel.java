package org.jabref.gui.maintable;

import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the full internal name of a column in the main table. Consists of two parts:
 * The type of the column and a qualifier, like the field name to be displayed in the column.
 */
public class MainTableColumnModel {

    public static final Character COLUMNS_QUALIFIER_DELIMITER = ':';

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

    private final ObjectProperty<Type> typeProperty = new SimpleObjectProperty<>();
    private final StringProperty qualifierProperty = new SimpleStringProperty("");
    private final DoubleProperty widthProperty = new SimpleDoubleProperty(ColumnPreferences.DEFAULT_WIDTH);

    /**
     * This is used by the preferences dialog, to initialize available columns the user can add to the table.
     *
     * @param type the {@code MainTableColumnModel.Type} of the column, e.g. "NORMALFIELD" or "GROUPS"
     * @param qualifier the stored qualifier of the column, e.g. "author/editor"
     */
    public MainTableColumnModel(Type type, String qualifier) {
        Objects.requireNonNull(type);
        this.typeProperty.setValue(type);
        this.qualifierProperty.setValue(qualifier);
    }

    public MainTableColumnModel(Type type) {
        this(type, "");
    }

    public Type getType() { return typeProperty.getValue(); }

    public String getQualifier() { return qualifierProperty.getValue(); }

    public String getName() {
        if (qualifierProperty.getValue().isBlank()) {
            return typeProperty.getValue().getName();
        } else {
            return typeProperty.getValue().getName() + COLUMNS_QUALIFIER_DELIMITER + qualifierProperty.getValue();
        }
    }

    public String getDisplayName() {
        if ((typeProperty.getValue() == Type.GROUPS
                || typeProperty.getValue() == Type.FILES
                || typeProperty.getValue() == Type.LINKED_IDENTIFIER)
                && qualifierProperty.getValue().isBlank()) {
            return typeProperty.getValue().getDisplayName();
        } else {
            return FieldsUtil.getNameWithType(FieldFactory.parseField(qualifierProperty.getValue()));
        }
    }

    public StringProperty nameProperty() { return new ReadOnlyStringWrapper(getDisplayName()); }

    public DoubleProperty widthProperty() { return widthProperty; }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MainTableColumnModel that = (MainTableColumnModel) o;

        if (typeProperty != that.typeProperty) {
            return false;
        }
        return Objects.equals(qualifierProperty, that.qualifierProperty);
    }

    public int hashCode() {
        return Objects.hash(typeProperty.getValue(), qualifierProperty.getValue());
    }

    /**
     * This is used by JabRefPreferences, to create a new ColumnModel out ouf the stored preferences.
     *
     * @param rawColumnName the stored name of the column, e.g. "field:author"
     * @param width the stored width of the column
     */
    public static MainTableColumnModel parse(String rawColumnName, Double width) {
        MainTableColumnModel columnModel = parse(rawColumnName);

        Objects.requireNonNull(width);
        columnModel.widthProperty().setValue(width);
        return columnModel;
    }

    /**
     * This is used by the preferences dialog, to allow the user to type in a field he wants to add to the table.
     *
     * @param rawColumnName the stored name of the column, e.g. "field:author", or "author"
     */
    public static MainTableColumnModel parse(String rawColumnName) {
        Objects.requireNonNull(rawColumnName);
        String[] splittedName = rawColumnName.split(COLUMNS_QUALIFIER_DELIMITER.toString());

        Type type = Type.fromString(splittedName[0]);
        String qualifier = "";

        if (type == Type.NORMALFIELD || type == Type.SPECIALFIELD || type == Type.EXTRAFILE) {
            if (splittedName.length == 1) {
                qualifier = splittedName[0]; // By default the rawColumnName is parsed as NORMALFIELD
            } else {
                qualifier = splittedName[1];
            }
        }

        return new MainTableColumnModel(type, qualifier);
    }
}
