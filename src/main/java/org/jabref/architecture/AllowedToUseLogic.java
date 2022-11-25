package org.jabref.architecture;

/**
 * Annotation to indicate that this logic class can be used in the model package.
 */
public @interface AllowedToUseLogic {

    // The rationale
    String value();
}
