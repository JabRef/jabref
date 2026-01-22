package org.jabref.logic.importer.plaincitation;

import org.jabref.logic.l10n.Localization;

public enum PlainCitationParserChoice {
    RULE_BASED_GENERAL,
    RULE_BASED_IEEE,
    GROBID,
    LLM;

    public String getLocalizedName() {
        return switch (this) {
            case RULE_BASED_GENERAL ->
                    Localization.lang("Rule-based (general)");
            case RULE_BASED_IEEE ->
                    Localization.lang("Rule-based (IEEE)");
            case GROBID ->
                    Localization.lang("Grobid");
            case LLM ->
                    Localization.lang("LLM");
        };
    }
}
