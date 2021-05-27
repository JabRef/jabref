package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.uno.UnoRuntime;

public class UnoCast {

    private UnoCast() { }

    /**
     * unoQI : short for UnoRuntime.queryInterface
     *
     * @return A reference to the requested UNO interface type if available,
     *         otherwise null
     */
    public static <T> T unoQI(Class<T> zInterface, Object object) {
        return UnoRuntime.queryInterface(zInterface, object);
    }

    public static <T> Optional<T> optUnoQI(Class<T> zInterface, Object object) {
        return Optional.ofNullable(UnoRuntime.queryInterface(zInterface, object));
    }
}
