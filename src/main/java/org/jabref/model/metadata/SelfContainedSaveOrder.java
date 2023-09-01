package org.jabref.model.metadata;

import java.util.List;

/**
 * With this class, the user of an instance can directly sort things. Without looking up anything in the preferences or in the UI.
 */
public class SelfContainedSaveOrder extends SaveOrder {
    public SelfContainedSaveOrder(OrderType orderType, List<SortCriterion> sortCriteria) {
        super(orderType, sortCriteria);
        if (orderType == OrderType.TABLE) {
            throw new IllegalArgumentException("TABLE requires external lookup.");
        }
    }

    /**
     * Converts a SaveOrder to a SelfContainedSaveOrder
     *
     * @throws IllegalArgumentException if {@code saveOrder} has {@link OrderType#TABLE}
     */
    public static SelfContainedSaveOrder of(SaveOrder saveOrder) {
        if (saveOrder instanceof SelfContainedSaveOrder order) {
            return order;
        }
        return new SelfContainedSaveOrder(saveOrder.getOrderType(), saveOrder.getSortCriteria());
    }
}
