package org.jabref.logic.integrity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/// Provides the set of country names used by the integrity checker to detect
/// location information embedded in booktitle fields.
///
/// Country names are derived from {@link Locale#getISOCountries()} and
/// {@link Locale#getDisplayCountry(Locale)} so that no hard-coded list needs
/// to be maintained.
public final class Countries {

    /// Set of country names (in English, lower-case) derived from the JDK locale data.
    public static final Set<String> COUNTRY_NAMES = Arrays.stream(Locale.getISOCountries())
            .map(code -> new Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH))
            .filter(name -> !name.isEmpty())
            .map(name -> name.toLowerCase(Locale.ENGLISH))
            .collect(Collectors.toUnmodifiableSet());

    private Countries() {
        // utility class
    }
}
