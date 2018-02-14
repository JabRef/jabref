package org.jabref.logic.formatter;

import java.util.Collections;

import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

public class FormatterConfiguration {

    /**
     * Default instance that can be customized on program startup
     */
    private static ProtectedTermsLoader protectedTermsLoader = new ProtectedTermsLoader();

    public static void setProtectedTermsLoader(ProtectedTermsLoader protectedTermsLoader) {
        FormatterConfiguration.protectedTermsLoader = protectedTermsLoader;
    }

    public static ProtectedTermsLoader getProtectedTermsLoader() {
        return protectedTermsLoader;
    }
}
