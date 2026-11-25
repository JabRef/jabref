package org.jabref.logic.openoffice.uno;

import java.util.Optional;

import com.sun.star.uno.UnoRuntime;

/// Queries the interfaces supported by UNO objects without exposing nullable references.
public class UnoInterfaces {

    private UnoInterfaces() {
    }

    /// A UNO interface query can expose capabilities that are not available through a Java type cast.
    ///
    /// @return A reference to the requested UNO interface type if available, otherwise Optional.empty()
    public static <T> Optional<T> query(Class<T> interfaceType, Object object) {
        return Optional.ofNullable(UnoRuntime.queryInterface(interfaceType, object));
    }
}
