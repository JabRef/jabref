package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

import org.jabref.model.entry.field.Field;

/**
 * All implementing classes should additionally offer
 *
 * <ul>
 *     <li>public static Optional<Class> parse(String value)</li>
 *     <li>public static boolean isValid(String value)</li>
 * </ul>
 */
public interface Identifier {

    /**
     * Returns the identifier as String
     */
    String asString();

    Field getDefaultField();

    Optional<URI> getExternalURI();
}
