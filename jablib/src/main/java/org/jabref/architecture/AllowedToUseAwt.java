package org.jabref.architecture;

/**
 * Annotation to indicate that this logic class can access AWT
 */
public @interface AllowedToUseAwt {

    // The rationale
    String value();
}
