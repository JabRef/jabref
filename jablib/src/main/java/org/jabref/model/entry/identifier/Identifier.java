package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

import org.jabref.model.entry.field.Field;

/**
 * All implementing classes should additionally offer
 *
 * <ul>
 *     <li><code>public static Optional&lt;Class> parse(String value)</code></li>
 *     <li><code>public static boolean isValid(String value)</code></li>
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
