package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;

/**
 * All implementing classes should additionally offer
 *
 * <ul>
 *     <li><code>public static Optional&lt;Class> parse(String value)</code></li>
 *     <li><code>public static boolean isValid(String value)</code></li>
 * </ul>
 */
@AllowedToUseLogic("Uses StringUtil temporarily")
public interface Identifier {

    /**
     * Returns the identifier as String
     */
    String asString();

    Field getDefaultField();

    Optional<URI> getExternalURI();

    public static Optional<Identifier> from(String identifier) {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        return Stream.<Supplier<Optional<? extends Identifier>>>of(
                             () -> DOI.findInText(identifier),
                             () -> ArXivIdentifier.findInText(identifier),
                             () -> ISBN.parse(identifier),
                             () -> SSRN.parse(identifier),
                             () -> RFC.parse(identifier))
                     .map(Supplier::get)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .map(id -> (Identifier) id)
                     .findFirst();
    }
}
