package org.jabref.model.openoffice.uno;

import com.sun.star.uno.UnoRuntime;
import java.util.Optional;

public class UnoCast {

    private UnoCast() {}

    /**
     * cast : short for Optional.ofNullable(UnoRuntime.queryInterface(...))
     *
     * @return A reference to the requested UNO interface type if available, otherwise Optional.empty()
     */
    public static <T> Optional<T> cast(Class<T> zInterface, Object object) {
        return Optional.ofNullable(
            UnoRuntime.queryInterface(zInterface, object)
        );
    }
}
