package org.jabref.architecture;

/**
 * Annotation to indicate that this logic class can use class.getResource().
 * Mostly, because {@link java.nio.file.Path} is not used.
 * See <a href="graal#7682">https://github.com/oracle/graal/issues/7682</a> for a longer discussion.
 */
public @interface AllowedToUseClassGetResource {
    // The rationale
    String value();
}
