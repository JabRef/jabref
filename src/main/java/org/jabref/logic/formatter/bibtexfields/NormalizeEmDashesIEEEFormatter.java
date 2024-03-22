package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class NormalizeEmDashesIEEEFormatter extends Formatter {
    @Override
    public String getName() {
        return Localization.lang("Normalize em dashes from IEEE");
    }

    @Override
    public String getKey() {
        return "normalize_em_dash_ieee";
    }

    @Override
    public String format(String value) {
        return value.replaceAll("&amp;#x2014;", "—");
    }

    @Override
    public String getDescription() {
        return "Convert em dashes from IEEE Xplore";
    }

    @Override
    public String getExampleInput() {
        return "Towards situation-aware adaptive workflows: SitOPT &amp;#x2014; A general purpose situation-aware workflow management system";
    }
}
