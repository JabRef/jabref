package org.jabref.model.metadata;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * With this class, the user of an instance can directly sort things. Without looking up anything in the preferences or in the UI.
 *
 * To avoid confusion at the caller, we offer ORIGINAL and SPECIFIED only. Not TABLE.
 */
public class SelfContainedSaveOrder extends SaveOrder {

    public static final Logger LOGGER = LoggerFactory.getLogger(SelfContainedSaveOrder.class);

    public SelfContainedSaveOrder(OrderType orderType, List<SortCriterion> sortCriteria) {
        super(orderType, sortCriteria);
        if (orderType == OrderType.TABLE) {
            LOGGER.debug("TABLE with sort criteria {}", sortCriteria);
            throw new IllegalArgumentException("TABLE might require external lookup.");
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
        if ((saveOrder.getOrderType() == OrderType.TABLE) && (!saveOrder.getSortCriteria().isEmpty())) {
            // We map from TABLE to SPECIFIED to have the users of this class just to `switch` between
            //   ORIGINAL and SPECIFIED
            return new SelfContainedSaveOrder(OrderType.SPECIFIED, saveOrder.getSortCriteria());
        }
        return new SelfContainedSaveOrder(saveOrder.getOrderType(), saveOrder.getSortCriteria());
    }
}
