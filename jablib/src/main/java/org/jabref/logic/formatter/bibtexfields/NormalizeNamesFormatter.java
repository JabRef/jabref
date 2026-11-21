package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.AuthorList;

import org.jspecify.annotations.NonNull;

/// Formatter normalizing a list of person names to the BibTeX format.
public class NormalizeNamesFormatter extends Formatter {

    public static final String KEY = "normalize_names";

    @Override
    public String getName() {
        return Localization.lang("Normalize names of persons");
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String format(@NonNull String nameList) {
        AuthorList authorList = AuthorList.parse(nameList);
        return authorList.getAsBibtex();
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
