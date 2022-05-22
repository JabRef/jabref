package org.jabref.architecture;

/**
 * Annotation to indicate that this class can use System.Out.* instead of using the logging framework
 */
public @interface AllowedToUseStandardStreams {

    // The rationale
    String value();
}
