package org.jabref.architecture;

/**
 * Annotation to indicate that this logic class can access swing
 */
public @interface AllowedToUseSwing {

    // The rationale
    String value();
}
