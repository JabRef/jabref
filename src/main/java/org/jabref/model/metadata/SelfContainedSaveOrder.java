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
}
