package org.jabref.gui.maintable;

import java.util.EnumSet;
import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TableColumn;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the full internal name of a column in the main table. Consists of two parts: The type of the column and a qualifier, like the
 * field name to be displayed in the column.
 */
public class MainTableColumnModel {

    public static final Character COLUMNS_QUALIFIER_DELIMITER = ':';

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableColumnModel.class);

    public enum Type {
        INDEX("index", Localization.lang("Index")),
        EXTRAFILE("extrafile", Localization.lang("File type")),
        FILES("files", Localization.lang("Linked files")),
        GROUPS("groups", Localization.lang("Groups")),
        LINKED_IDENTIFIER("linked_id", Localization.lang("Linked identifiers")),
        NORMALFIELD("field"),
        SPECIALFIELD("special", Localization.lang("Special")),
        LIBRARY_NAME("library", Localization.lang("Library"));

        public static final EnumSet<Type> ICON_COLUMNS = EnumSet.of(EXTRAFILE, FILES, GROUPS, LINKED_IDENTIFIER);

        private final String name;
        private final String displayName;

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
            LOGGER.warn("Column type '{}' is unknown.", text);
            return NORMALFIELD;
        }
    }

    private final ObjectProperty<Type> typeProperty = new SimpleObjectProperty<>();
    private final StringProperty qualifierProperty = new SimpleStringProperty();
    private final DoubleProperty widthProperty = new SimpleDoubleProperty();
    private final ObjectProperty<TableColumn.SortType> sortTypeProperty = new SimpleObjectProperty<>();

    /**
     * This is used by the preferences dialog, to initialize available columns the user can add to the table.
     *
     * @param type      the {@code MainTableColumnModel.Type} of the column, e.g. "NORMALFIELD" or "EXTRAFILE"
     * @param qualifier the stored qualifier of the column, e.g. "author/editor"
     */
    public MainTableColumnModel(Type type, String qualifier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(qualifier);

        this.typeProperty.setValue(type);
        this.qualifierProperty.setValue(qualifier);
        this.sortTypeProperty.setValue(TableColumn.SortType.ASCENDING);

        if (Type.ICON_COLUMNS.contains(type)) {
            this.widthProperty.setValue(ColumnPreferences.ICON_COLUMN_WIDTH);
        } else {
            this.widthProperty.setValue(ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        }
    }

    /**
     * This is used by the preferences dialog, to initialize available basic icon columns, the user can add to the table.
     *
     * @param type the {@code MainTableColumnModel.Type} of the column, e.g. "GROUPS" or "LINKED_IDENTIFIER"
     */
    public MainTableColumnModel(Type type) {
        this(type, "");
    }

    /**
     * This is used by the preference migrations.
     *
     * @param type      the {@code MainTableColumnModel.Type} of the column, e.g. "NORMALFIELD" or "GROUPS"
     * @param qualifier the stored qualifier of the column, e.g. "author/editor"
     * @param width     the stored width of the column
     */
    public MainTableColumnModel(Type type, String qualifier, double width) {
        this(type, qualifier);

        this.widthProperty.setValue(width);
    }

    public Type getType() {
        return typeProperty.getValue();
    }

    public String getQualifier() {
        return qualifierProperty.getValue();
    }

    public String getName() {
        if (qualifierProperty.getValue().isBlank()) {
            return typeProperty.getValue().getName();
        } else {
            return typeProperty.getValue().getName() + COLUMNS_QUALIFIER_DELIMITER + qualifierProperty.getValue();
        }
    }

    public String getDisplayName() {
        if ((Type.ICON_COLUMNS.contains(typeProperty.getValue()) && qualifierProperty.getValue().isBlank())
                || (typeProperty.getValue() == Type.INDEX)) {
            return typeProperty.getValue().getDisplayName();
        } else {
            return FieldsUtil.getNameWithType(FieldFactory.parseField(qualifierProperty.getValue()));
        }
    }

    public StringProperty nameProperty() {
        return new ReadOnlyStringWrapper(getDisplayName());
    }

    public double getWidth() {
        return widthProperty.getValue();
    }

    public DoubleProperty widthProperty() {
        return widthProperty;
    }

    public TableColumn.SortType getSortType() {
        return sortTypeProperty.getValue();
    }

    public ObjectProperty<TableColumn.SortType> sortTypeProperty() {
        return sortTypeProperty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        MainTableColumnModel that = (MainTableColumnModel) o;

        if (typeProperty.getValue() != that.typeProperty.getValue()) {
            return false;
        }
        return Objects.equals(qualifierProperty.getValue(), that.qualifierProperty.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeProperty.getValue(), qualifierProperty.getValue());
    }

    /**
     * This creates a new {@code MainTableColumnModel} out of a given string
     *
     * @param rawColumnName the name of the column, e.g. "field:author", or "author"
     * @return A new {@code MainTableColumnModel}
     */
    public static MainTableColumnModel parse(String rawColumnName) {
        Objects.requireNonNull(rawColumnName);
        String[] splittedName = rawColumnName.split(COLUMNS_QUALIFIER_DELIMITER.toString());

        Type type = Type.fromString(splittedName[0]);
        String qualifier = "";

        if ((type == Type.NORMALFIELD)
                || (type == Type.SPECIALFIELD)
                || (type == Type.EXTRAFILE)) {
            if (splittedName.length == 1) {
                qualifier = splittedName[0]; // By default the rawColumnName is parsed as NORMALFIELD
            } else {
                qualifier = splittedName[1];
            }
        }

        return new MainTableColumnModel(type, qualifier);
    }
}
