package org.jabref.logic.l10n.parsing;

import org.jabref.logic.l10n.Localization;

/**
 * This class is used to test LocalizationParser
 */
public class ParseTestClass {

    public void simpleLangCall() {
        Localization.lang("simple");
    }

    public void callToVariable() {
        String stringVariable = "";
        Localization.lang(stringVariable);
    }

    public void callToVariableWithADR23() {
        String stringVariable = "";
        // @ADR(23)
        Localization.lang(stringVariable);
    }

}
