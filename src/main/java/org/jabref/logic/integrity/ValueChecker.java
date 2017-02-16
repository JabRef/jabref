package org.jabref.logic.integrity;

import java.util.Optional;

public interface ValueChecker {
    /**
     * Validates the specified value.
     * Returns a error massage if the validation failed. Otherwise an empty optional is returned.
     */
    Optional<String> checkValue(String value);
}
