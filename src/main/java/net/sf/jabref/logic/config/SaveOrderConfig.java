package net.sf.jabref.logic.config;

import net.sf.jabref.JabRefPreferences;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * Stores the save order config from MetaData
 * <p>
 * Format: <choice>, pair of field + ascending (boolean)
 */
public class SaveOrderConfig {

    public boolean saveInOriginalOrder;
    public boolean saveInSpecifiedOrder;

    // quick hack for outside modifications
    public final SortCriterion[] sortCriteria = new SortCriterion[3];

    public static class SortCriterion {

        public String field;
        public boolean descending;


        public SortCriterion() {
            this.field = "";
        }

        public SortCriterion(String field, String descending) {
            this.field = field;
            this.descending = Boolean.parseBoolean(descending);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SortCriterion that = (SortCriterion) o;
            return Objects.equals(descending, that.descending) &&
                    Objects.equals(field, that.field);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, descending);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaveOrderConfig that = (SaveOrderConfig) o;
        boolean sortCriteriaEquals = sortCriteria[0].equals(that.sortCriteria[0]) &&
                sortCriteria[1].equals(that.sortCriteria[1]) &&
                sortCriteria[2].equals(that.sortCriteria[2]);

        return Objects.equals(saveInOriginalOrder, that.saveInOriginalOrder) &&
                Objects.equals(saveInSpecifiedOrder, that.saveInSpecifiedOrder) &&
                sortCriteriaEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveInOriginalOrder, saveInSpecifiedOrder, Arrays.hashCode(sortCriteria));
    }

    public SaveOrderConfig() {
        // fill default values
        setSaveInOriginalOrder();
        sortCriteria[0] = new SortCriterion();
        sortCriteria[1] = new SortCriterion();
        sortCriteria[2] = new SortCriterion();
    }

    public SaveOrderConfig(List<String> data) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (data.isEmpty()) {
            throw new IllegalArgumentException();
        }

        String choice = data.get(0);
        if ("original".equals(choice)) {
            setSaveInOriginalOrder();
        } else {
            setSaveInSpecifiedOrder();
        }

        if (data.size() >= 3) {
            sortCriteria[0] = new SortCriterion(data.get(1), data.get(2));
        } else {
            sortCriteria[0] = new SortCriterion();
        }
        if (data.size() >= 5) {
            sortCriteria[1] = new SortCriterion(data.get(3), data.get(4));
        } else {
            sortCriteria[1] = new SortCriterion();
        }
        if (data.size() >= 7) {
            sortCriteria[2] = new SortCriterion(data.get(5), data.get(6));
        } else {
            sortCriteria[2] = new SortCriterion();
        }
    }

    public void setSaveInOriginalOrder() {
        this.saveInOriginalOrder = true;
        this.saveInSpecifiedOrder = false;
    }

    public void setSaveInSpecifiedOrder() {
        this.saveInOriginalOrder = false;
        this.saveInSpecifiedOrder = true;
    }

    public SortCriterion[] getSortCriteria() {
        return sortCriteria;
    }

    public static SaveOrderConfig loadExportSaveOrderFromPreferences(JabRefPreferences preferences) {
        SaveOrderConfig config = new SaveOrderConfig();
        config.sortCriteria[0].field = preferences.get(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD);
        config.sortCriteria[0].descending = preferences.getBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING);
        config.sortCriteria[1].field = preferences.get(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD);
        config.sortCriteria[1].descending = preferences.getBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING);
        config.sortCriteria[2].field = preferences.get(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD);
        config.sortCriteria[2].descending = preferences.getBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING);

        return config;
    }

    public static SaveOrderConfig loadTableSaveOrderFromPreferences(JabRefPreferences preferences) {
        SaveOrderConfig config = new SaveOrderConfig();
        config.sortCriteria[0].field = preferences.get(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD);
        config.sortCriteria[0].descending = preferences.getBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING);
        config.sortCriteria[1].field = preferences.get(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD);
        config.sortCriteria[1].descending = preferences.getBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING);
        config.sortCriteria[2].field = preferences.get(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD);
        config.sortCriteria[2].descending = preferences.getBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING);

        return config;
    }

    public void storeAsExportSaveOrderInPreferences(JabRefPreferences preferences) {
        preferences.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, sortCriteria[0].descending);
        preferences.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, sortCriteria[1].descending);
        preferences.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, sortCriteria[2].descending);

        preferences.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, sortCriteria[0].field);
        preferences.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, sortCriteria[1].field);
        preferences.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, sortCriteria[2].field);
    }

    /**
     * Outputs the current configuration to be consumed later by the constructor
     */
    public Vector<String> getVector() {
        Vector<String> res = new Vector<>(7);
        if (saveInOriginalOrder) {
            res.insertElementAt("original", 0);
        } else {
            assert saveInSpecifiedOrder;
            res.insertElementAt("specified", 0);
        }

        res.insertElementAt(sortCriteria[0].field, 1);
        res.insertElementAt(Boolean.toString(sortCriteria[0].descending), 2);
        res.insertElementAt(sortCriteria[1].field, 3);
        res.insertElementAt(Boolean.toString(sortCriteria[1].descending), 4);
        res.insertElementAt(sortCriteria[2].field, 5);
        res.insertElementAt(Boolean.toString(sortCriteria[2].descending), 6);

        return res;
    }

}
