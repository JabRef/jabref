package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class NormalizeAmpersandEmDashesFormatter extends Formatter {
    @Override
    public String getName() {
        return Localization.lang("Normalize an ampersand and em dash");
    }

    @Override
    public String getKey() {
        return "normalize_ampersand_em_dash";
    }

    @Override
    public String format(String value) {
        return value.replaceAll("&amp;#x2014;", "—");
    }

    @Override
    public String getDescription() {
        return "Convert a sequence of ampersand and em dash (written in HTML) to a single unicode em dash";
    }

    @Override
    public String getExampleInput() {
        return "Towards situation-aware adaptive workflows: SitOPT &amp;#x2014; A general purpose situation-aware workflow management system";
    }
}
