package net.sf.jabref.logic.formatter.bibtexfields;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.AuthorList;

/**
 * Formatter normalizing a list of person names to the BibTeX format.
 */
public class NormalizeNamesFormatter implements Formatter {

    // Avoid partition where these values are contained
    private final Collection<String> avoidTermsInLowerCase = Arrays.asList("jr", "sr", "jnr", "snr", "von", "zu", "van", "der");

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
        Objects.requireNonNull(value);
        // Handle case names in order lastname, firstname and separated by ","
        // E.g., Ali Babar, M., Dingsøyr, T., Lago, P., van der Vliet, H.
        if (!value.contains(" and ") && !value.contains("{") && !value.contains(";")) {
            String[] valueParts = value.split(",");
            // Delete spaces for correct case identification
            for (int i=0; i < valueParts.length; i++) {
                valueParts[i] = valueParts[i].trim();
            }
            // Looking for space between pre- and lastname
            boolean spaceInAllParts = false;
            for (int i=0; i<valueParts.length; i++) {
                if (valueParts[i].contains(" ") ) {
                    spaceInAllParts = true;
                } else {
                    spaceInAllParts = false;
                    break;
                }
            }

            // We hit the comma name separator case
            // Usually the getAsLastFirstNamesWithAnd method would separate them if pre- and lastname are separated with "and"
            // If not, we check if spaces separate pre- and lastname
            if (spaceInAllParts) {
                value = value.replaceAll(",", " and");
            } else {
                // Looking for name affixes to avoid
                // partCount need to reduce by the count off avoiding terms
                //valuePartsCount holds the count of name parts without the avoided terms

                int valuePartsCount = valueParts.length;
                // Holds the index of each term which needs to be avoided
                Collection<Integer> avoidIndex = new HashSet<>();

                for (int i = 0; i < valueParts.length; i++) {
                    if (avoidTermsInLowerCase.contains(valueParts[i].toLowerCase())) {
                        avoidIndex.add(i);
                        valuePartsCount--;
                    }
                }

                if ((valuePartsCount % 2) == 0) {
                    // We hit the described special case with name affix like Jr
                    StringBuilder stringBuilder = new StringBuilder();
                    // avoidedTimes needs to be increased b< the count of avoided terms for correct odd/even calculation
                    int avoidedTimes = 0;
                    for (int i = 0; i < valueParts.length; i++) {
                        if (avoidIndex.contains(i)) {
                            // We hit a name affix
                            stringBuilder.append(valueParts[i]);
                            stringBuilder.append(',');
                            avoidedTimes++;
                        } else {
                            stringBuilder.append(valueParts[i]);
                            if (((i + avoidedTimes) % 2) == 0) {
                                // Hit separation between last name and firstname --> comma has to be kept
                                stringBuilder.append(',');
                            } else {
                                // Hit separation between full names (e.g., Ali Babar, M. and Dingsøyr, T.) --> semicolon has to be used
                                // Will be treated correctly by AuthorList.parse(value);
                                stringBuilder.append(';');
                            }
                        }
                    }
                    value = stringBuilder.toString();
                }
            }
        }

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
