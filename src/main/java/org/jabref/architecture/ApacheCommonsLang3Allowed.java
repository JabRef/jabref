package org.jabref.architecture;

/**
 * Annotation to indicate that usage of ApacheCommonsLang3 is explicitly allowed.
 * The intention is to fully switch to Google Guava and only use Apache Commons Lang3 if there is no other possibility
 */
public @interface ApacheCommonsLang3Allowed {

    // The rationale
    String value();

}
