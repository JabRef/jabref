package org.jabref.logic.importer.plaincitation;

import org.jabref.logic.l10n.Localization;

public enum PlainCitationParserChoice {
    RULE_BASED,
    GROBID,
    LLM;

    public String getLocalizedName() {
        return switch (this) {
            case RULE_BASED ->
                    Localization.lang("Rule-based");
            case GROBID ->
                    Localization.lang("Grobid");
            case LLM ->
                    Localization.lang("LLM");
        };
    }
}
