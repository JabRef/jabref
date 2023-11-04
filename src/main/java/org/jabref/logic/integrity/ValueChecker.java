package org.jabref.logic.integrity;

import java.util.Optional;

public interface ValueChecker {
    /**
     * Validates the specified value.
     * Returns an error message if the validation failed. Otherwise, an empty optional is returned.
     *
     * @return Validation error message
     */
    Optional<String> checkValue(String value);
}
