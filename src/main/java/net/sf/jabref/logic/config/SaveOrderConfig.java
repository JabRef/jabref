package net.sf.jabref.logic.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.JabRefPreferences;

/**
 * Stores the save order config from MetaData
 * <p>
 * Format: <choice>, pair of field + ascending (boolean)
 */
public class SaveOrderConfig {

    public boolean saveInOriginalOrder;

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

        public SortCriterion(String field, boolean descending) {
            this.field = field;
            this.descending = descending;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SortCriterion{");
            sb.append("field='").append(field).append('\'');
            sb.append(", descending=").append(descending);
            sb.append('}');
            return sb.toString();
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

    public SaveOrderConfig(boolean saveInOriginalOrder, SortCriterion first, SortCriterion second,
            SortCriterion third) {
        this.saveInOriginalOrder = saveInOriginalOrder;
        sortCriteria[0] = first;
        sortCriteria[1] = second;
        sortCriteria[2] = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SaveOrderConfig) {
            SaveOrderConfig that = (SaveOrderConfig) o;
            boolean sortCriteriaEquals = sortCriteria[0].equals(that.sortCriteria[0])
                    && sortCriteria[1].equals(that.sortCriteria[1]) && sortCriteria[2].equals(that.sortCriteria[2]);

            return Objects.equals(saveInOriginalOrder, that.saveInOriginalOrder) && sortCriteriaEquals;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveInOriginalOrder, Arrays.hashCode(sortCriteria));
    }

    public SaveOrderConfig() {
        // fill default values
        setSaveInOriginalOrder();
        sortCriteria[0] = new SortCriterion();
        sortCriteria[1] = new SortCriterion();
        sortCriteria[2] = new SortCriterion();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SaveOrderConfig{");
        sb.append("saveInOriginalOrder=").append(saveInOriginalOrder);
        sb.append(", sortCriteria=").append(Arrays.toString(sortCriteria));
        sb.append('}');
        return sb.toString();
    }

    public SaveOrderConfig(List<String> data) {
        Objects.requireNonNull(data);

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
    }

    public void setSaveInSpecifiedOrder() {
        this.saveInOriginalOrder = false;
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
    public List<String> getConfigurationList() {
        List<String> res = new ArrayList<>(7);
        if (saveInOriginalOrder) {
            res.add("original");
        } else {
            res.add("specified");
        }

        res.add(sortCriteria[0].field);
        res.add(Boolean.toString(sortCriteria[0].descending));
        res.add(sortCriteria[1].field);
        res.add(Boolean.toString(sortCriteria[1].descending));
        res.add(sortCriteria[2].field);
        res.add(Boolean.toString(sortCriteria[2].descending));

        return res;
    }

}
