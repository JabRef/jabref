package org.jabref.logic.formatter.bibtexfields;

import java.util.StringJoiner;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NullMarked;

/**
 * Removes start and end brace both at the complete string and at beginning/end of a word
 * <p>
 * E.g.,
 * <ul>
 *     <li>{Vall{\'e}e Poussin} -> Vall{\'e}e Poussin</li>
 *     <li>{Vall{\'e}e} {Poussin} -> Vall{\'e}e Poussin</li>
 *     <li>Vall{\'e}e Poussin -> Vall{\'e}e Poussin</li>
 * </ul>
 */
@NullMarked
public class RemoveWordEnclosingAndOuterEnclosingBracesFormatter extends Formatter {

    private static final RemoveEnclosingBracesFormatter REMOVE_ENCLOSING_BRACES_FORMATTER = new RemoveEnclosingBracesFormatter();

    @Override
    public String getName() {
        return Localization.lang("Remove word enclosing braces");
    }

    @Override
    public String getKey() {
        return "remove_enclosing_and_outer_enclosing_braces";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes braces encapsulating a complete word and the complete field content.");
    }

    @Override
    public String getExampleInput() {
        return "{In {CDMA}}";
    }

    @Override
    public String format(String input) {
        if (StringUtil.isBlank(input)) {
            return input;
        }

        if (!input.contains("{")) {
            return input;
        }

        // We need to first remove the outer braces to have double braces at the last word working (e.g., {In {CDMA}})
        input = REMOVE_ENCLOSING_BRACES_FORMATTER.format(input);

        String[] split = input.split(" ");
        StringJoiner result = new StringJoiner(" ");
        for (String s : split) {
            if ((s.length() > 2) && s.startsWith("{") && s.endsWith("}")) {
                // quick solution (which we don't do): just remove first "{" and last "}"
                // however, it might be that s is like {A}bbb{c}, where braces may not be removed

                String inner = s.substring(1, s.length() - 1);

                if (inner.contains("}")) {
                    if (properBrackets(inner)) {
                        s = inner;
                    }
                } else {
                    //  no inner curly brackets found, no check needed, inner can just be used as s
                    s = inner;
                }
            }
            result.add(s);
        }
        return result.toString();
    }

    /**
     * @return true iff the brackets in s are properly paired
     */
    private boolean properBrackets(String s) {
        // nested construct is there, check for "proper" nesting
        int i = 0;
        int level = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            switch (c) {
                case '{':
                    level++;
                    break;
                case '}':
                    level--;
                    if (level == -1) { // improper nesting
                        return false;
                    }
                    break;
                default:
                    break;
            }
            i++;
        }
        return level == 0;
    }
}
