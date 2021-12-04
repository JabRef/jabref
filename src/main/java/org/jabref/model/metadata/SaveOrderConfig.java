package org.jabref.model.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the save order config from MetaData
 * <p>
 * Format: &lt;choice>, pair of field + ascending (boolean)
 */
public class SaveOrderConfig {

    public enum OrderType {
        SPECIFIED("specified"),
        ORIGINAL("original"),
        TABLE("table");

        private final String name;

        OrderType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static SaveOrderConfig.OrderType fromBooleans(boolean saveInSpecifiedOrder, boolean saveInOriginalOrder) {
            SaveOrderConfig.OrderType orderType = SaveOrderConfig.OrderType.TABLE;
            if (saveInSpecifiedOrder) {
                orderType = SaveOrderConfig.OrderType.SPECIFIED;
            } else if (saveInOriginalOrder) {
                orderType = SaveOrderConfig.OrderType.ORIGINAL;
            }

            return orderType;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveOrderConfig.class);

    private final List<SortCriterion> sortCriteria = new ArrayList<>();
    private OrderType orderType;

    public SaveOrderConfig() {
        this.orderType = OrderType.ORIGINAL;
    }

    public SaveOrderConfig(OrderType orderType, List<SortCriterion> sortCriteria) {
        this.orderType = orderType;
        this.sortCriteria.addAll(sortCriteria);
    }

    private SaveOrderConfig(List<String> data) {
        Objects.requireNonNull(data);

        if (data.isEmpty()) {
            throw new IllegalArgumentException();
        }

        try {
            this.orderType = OrderType.valueOf(data.get(0).toUpperCase());
        } catch (IllegalArgumentException ex) {
            if (data.size() > 1 && data.size() % 2 == 1) {
                LOGGER.warn("Could not parse sort order: {} - trying to parse the sort criteria", data.get(0));
                this.orderType = OrderType.SPECIFIED;
            } else {
                LOGGER.warn("Could not parse sort order: {}", data.get(0));
                this.orderType = OrderType.ORIGINAL;
                return;
            }
        }

        for (int index = 1; index < data.size(); index = index + 2) {
            sortCriteria.add(new SortCriterion(FieldFactory.parseField(data.get(index)), data.get(index + 1)));
        }
    }

    public static SaveOrderConfig parse(List<String> orderedData) {
        return new SaveOrderConfig(orderedData);
    }

    public static SaveOrderConfig getDefaultSaveOrder() {
        SaveOrderConfig standard = new SaveOrderConfig();
        standard.orderType = OrderType.ORIGINAL;
        return standard;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public List<SortCriterion> getSortCriteria() {
        return sortCriteria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SaveOrderConfig that) {
            return Objects.equals(sortCriteria, that.sortCriteria) &&
                    Objects.equals(orderType, that.orderType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderType, sortCriteria);
    }

    @Override
    public String toString() {
        return "SaveOrderConfig{" + "orderType=" + orderType.toString() +
                ", sortCriteria=" + sortCriteria +
                '}';
    }

    /**
     * Outputs the current configuration to be consumed later by the constructor
     */
    public List<String> getAsStringList() {
        List<String> res = new ArrayList<>(7);
        if (orderType == OrderType.ORIGINAL) {
            res.add(OrderType.ORIGINAL.toString());
        } else {
            res.add(OrderType.SPECIFIED.toString());
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

        /**
         *
         * @param field The field
         * @param descending Must be a boolean value as string, e.g. "true", "false"
         */
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
