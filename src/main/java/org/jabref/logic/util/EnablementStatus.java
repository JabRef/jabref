package org.jabref.logic.util;

/**
 * Specifies the different possible enablement states for online services
 */
public enum EnablementStatus {
    FIRST_START, // The first time a user uses this service
    ENABLED,
    DISABLED;

    public static EnablementStatus fromString(String status) {
        for (EnablementStatus value : EnablementStatus.values()) {
            if (value.toString().equalsIgnoreCase(status)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum found with value: " + status);
    }
}
