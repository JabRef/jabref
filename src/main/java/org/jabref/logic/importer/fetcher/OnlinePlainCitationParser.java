package org.jabref.logic.importer.fetcher;

import org.jabref.logic.l10n.Localization;

public enum OnlinePlainCitationParser {
    GROBID,
    LLM;

    public String getLocalizedName() {
        return switch (this) {
            case GROBID ->
                    Localization.lang("GROBID");
            case LLM ->
                    Localization.lang("LLM");
        };
    }
}
