package org.jabref.model.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the save order config for a library
 * <p>
 * Format: &lt;choice> ({@link OrderType}, a pair of {@link Field} + descending (boolean)
 * </p>
 * <p>
 * Note that {@link OrderType#TABLE} can only be used as "intermediate" setting. When passing <code>SaveOrder</code>
 * to {@link org.jabref.logic.exporter.BibDatabaseWriter}, the orderType must be different. Reason: The writer
 * does not have access to the UI.
 * </p>
 */
public class SaveOrder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveOrder.class);

    private final OrderType orderType;
    private final List<SortCriterion> sortCriteria;

    public SaveOrder(OrderType orderType, List<SortCriterion> sortCriteria) {
        this.orderType = orderType;
        this.sortCriteria = sortCriteria;
    }

    private SaveOrder(List<String> data) {
        Objects.requireNonNull(data);

        if (data.isEmpty()) {
            throw new IllegalArgumentException();
        }

        OrderType orderType;
        try {
            orderType = OrderType.valueOf(data.getFirst().toUpperCase());
        } catch (IllegalArgumentException ex) {
            if (data.size() > 1 && data.size() % 2 == 1) {
                LOGGER.warn("Could not parse sort order: {} - trying to parse the sort criteria", data.getFirst());
                orderType = OrderType.SPECIFIED;
            } else {
                LOGGER.warn("Could not parse sort order: {}", data.getFirst());
                this.sortCriteria = List.of();
                this.orderType = OrderType.ORIGINAL;
                return;
            }
        }
        this.orderType = orderType;

        List<SortCriterion> sortCriteria = new ArrayList<>(data.size() / 2);
        for (int index = 1; index < data.size(); index = index + 2) {
            sortCriteria.add(new SortCriterion(FieldFactory.parseField(data.get(index)), data.get(index + 1)));
        }
        this.sortCriteria = sortCriteria;
    }

    public static SaveOrder parse(List<String> orderedData) {
        return new SaveOrder(orderedData);
    }

    public static SelfContainedSaveOrder getDefaultSaveOrder() {
        return new SelfContainedSaveOrder(OrderType.ORIGINAL, List.of());
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
        if (o instanceof SaveOrder that) {
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
        res.add(orderType.toString());

        for (SortCriterion sortCriterion : sortCriteria) {
            res.add(sortCriterion.field.getName());
            res.add(Boolean.toString(sortCriterion.descending));
        }

        return res;
    }

    public static class SortCriterion {

        public final Field field;

        public final boolean descending;

        /**
         * Given field sorted ascending
         */
        public SortCriterion(Field field) {
            this(field, false);
        }

        /**
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

        public static SaveOrder.OrderType fromBooleans(boolean saveInSpecifiedOrder, boolean saveInOriginalOrder) {
            SaveOrder.OrderType orderType = SaveOrder.OrderType.TABLE;
            if (saveInSpecifiedOrder) {
                orderType = SaveOrder.OrderType.SPECIFIED;
            } else if (saveInOriginalOrder) {
                orderType = SaveOrder.OrderType.ORIGINAL;
            }

            return orderType;
        }
    }
}
