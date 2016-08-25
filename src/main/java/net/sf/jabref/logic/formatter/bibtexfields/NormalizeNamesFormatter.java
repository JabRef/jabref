package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.AuthorList;

/**
 * Formatter normalizing a list of person names to the BibTeX format.
 */
public class NormalizeNamesFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Normalize names of persons");
    }

    @Override
    public String getKey() {
        return "normalize_names";
    }

    @Override
    public String format(String value) {
        AuthorList authorList = AuthorList.parse(value);
        return authorList.getAsLastFirstNamesWithAnd(false);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes lists of persons to the BibTeX standard.");
    }

    @Override
    public String getExampleInput() {
        return "Albert Einstein and Alan Turing";
    }

}
