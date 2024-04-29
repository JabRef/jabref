package org.jabref.logic.formatter.bibtexfields;

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
    @Override
    public String getName() {
        return Localization.lang("Remove word enclosing braces");
    }

    @Override
    public String getKey() {
        return "remove_braces";
    }

    @Override
    public String format(String name) {
        if (StringUtil.isBlank(name)) {
            return null;
        }

        if (!name.contains("{")) {
            return name;
        }

        String[] split = name.split(" ");
        StringBuilder b = new StringBuilder();
        for (String s : split) {
            if ((s.length() > 2) && s.startsWith("{") && s.endsWith("}")) {
                // quick solution (which we don't do: just remove first "{" and last "}"
                // however, it might be that s is like {A}bbb{c}, where braces may not be removed

                // inner
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
            b.append(s).append(' ');
        }
        // delete last
        b.deleteCharAt(b.length() - 1);

        // now, all inner words are cleared
        // case {word word word} remains
        // as above, we have to be aware of {w}ord word wor{d} and {{w}ord word word}

        String newName = b.toString();

        if (newName.startsWith("{") && newName.endsWith("}")) {
            String inner = newName.substring(1, newName.length() - 1);
            if (properBrackets(inner)) {
                return inner;
            } else {
                return newName;
            }
        } else {
            return newName;
        }
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes braces encapsulating a complete word and the complete field content.");
    }

    @Override
    public String getExampleInput() {
        return "{In {CDMA}}";
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
