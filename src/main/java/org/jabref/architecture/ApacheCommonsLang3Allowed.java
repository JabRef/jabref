package org.jabref.architecture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that usage of ApacheCommonsLang3 is explicitly allowed.
 * The intention is to fully switch to Google Guava and only use Apache Commons Lang3 if there is no other possibility
 */
@Target(ElementType.TYPE)
public @interface ApacheCommonsLang3Allowed {

    // The rationale
    String value();

}
