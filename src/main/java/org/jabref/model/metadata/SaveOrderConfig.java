package org.jabref.model.metadata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

/**
 * Stores the save order config from MetaData
 * <p>
 * Format: &lt;choice>, pair of field + ascending (boolean)
 */
public class SaveOrderConfig {

    private static final String ORIGINAL = "original";
    private static final String SPECIFIED = "specified";
    private final LinkedList<SortCriterion> sortCriteria = new LinkedList<>();
    private boolean saveInOriginalOrder;
    private boolean saveInSpecifiedOrder;

    public SaveOrderConfig() {
        setSaveInOriginalOrder();
    }

    public SaveOrderConfig(boolean saveInOriginalOrder, boolean saveInSpecifiedOrder, SortCriterion first, SortCriterion second, SortCriterion third) {
        this.saveInOriginalOrder = saveInOriginalOrder;
        this.saveInSpecifiedOrder = saveInSpecifiedOrder;
        sortCriteria.add(first);
        sortCriteria.add(second);
        sortCriteria.add(third);
    }

    private SaveOrderConfig(List<String> data) {
        Objects.requireNonNull(data);

        if (data.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String choice = data.get(0);
        if (ORIGINAL.equals(choice)) {
            setSaveInOriginalOrder();
        } else {
            setSaveInSpecifiedOrder();
        }

        for (int index = 1; index < data.size(); index = index + 2) {
            sortCriteria.addLast(new SortCriterion(FieldFactory.parseField(data.get(index)), data.get(index + 1)));
        }
    }

    public static SaveOrderConfig parse(List<String> orderedData) {
        return new SaveOrderConfig(orderedData);
    }

    public static SaveOrderConfig getDefaultSaveOrder() {
        SaveOrderConfig standard = new SaveOrderConfig();
        standard.setSaveInOriginalOrder();
        return standard;
    }

    public boolean saveInOriginalOrder() {
        return saveInOriginalOrder;
    }

    public boolean saveInSpecifiedOrder() {
        return saveInSpecifiedOrder;
    }

    public LinkedList<SortCriterion> getSortCriteria() {
        return sortCriteria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SaveOrderConfig) {
            SaveOrderConfig that = (SaveOrderConfig) o;
            return Objects.equals(sortCriteria, that.sortCriteria) &&
                    Objects.equals(saveInOriginalOrder, that.saveInOriginalOrder) &&
                    Objects.equals(saveInSpecifiedOrder, that.saveInSpecifiedOrder);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveInOriginalOrder, saveInSpecifiedOrder, sortCriteria);
    }

    @Override
    public String toString() {
        return "SaveOrderConfig{" + "saveInOriginalOrder=" + saveInOriginalOrder
                + "saveInSpecifiedOrder =" + saveInSpecifiedOrder
                + ", sortCriteria=" + sortCriteria +
                '}';
    }

    public void setSaveInOriginalOrder() {
        this.saveInOriginalOrder = true;
    }

    public void setSaveInSpecifiedOrder() {
        this.saveInOriginalOrder = false;
        this.saveInSpecifiedOrder = true;
    }

    /**
     * Outputs the current configuration to be consumed later by the constructor
     */
    public List<String> getAsStringList() {
        List<String> res = new ArrayList<>(7);
        if (saveInOriginalOrder) {
            res.add(ORIGINAL);
        } else {
            res.add(SPECIFIED);
        }

        for (SortCriterion sortCriterion : sortCriteria) {
            res.add(sortCriterion.field.getName());
            res.add(Boolean.toString(sortCriterion.descending));
        }

        return res;
    }

    public static class SortCriterion {

        public Field field;

        public boolean descending;

        public SortCriterion(Field field, String descending) {
            this.field = field;
            this.descending = Boolean.parseBoolean(descending);
        }

        public SortCriterion(Field field, boolean descending) {
            this.field = field;
            this.descending = descending;
        }

        public SortCriterion() {

        }

        @Override
        public String toString() {
            return "SortCriterion{" + "field='" + field + '\'' +
                    ", descending=" + descending +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }
            SortCriterion that = (SortCriterion) o;
            return Objects.equals(descending, that.descending) &&
                    Objects.equals(field, that.field);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, descending);
        }
    }
}
