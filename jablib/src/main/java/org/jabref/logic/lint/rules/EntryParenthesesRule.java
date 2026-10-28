package org.jabref.logic.lint.rules;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.Rule;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Encloses an entry in braces where it is enclosed in parentheses, `@Article(key, ...)`.
///
/// BibTeX accepts both, and JabRef writes braces, but only for an entry it writes anew: one nothing
/// changed is written back the way it was read. A library therefore keeps its parentheses entry by
/// entry until each one happens to be edited, and a formatter run, which writes every entry anew,
/// replaces them without saying why. This rule reports them, and repairs them in the entries a save
/// would otherwise leave alone.
///
/// Unlike the other rules, it is not about a field but about how the entry was read: the repair
/// replaces the two delimiters in the entry's parsed serialization and keeps everything between
/// them, so that a save writing the entry back as it was read changes those two characters and
/// nothing else. No field changes, so there is nothing to undo.
@NullMarked
public class EntryParenthesesRule implements Rule {
    public static final String ID = "entry-parentheses";

    /// An entry as the parser read it, past the comments above it: its type, the delimiter opening
    /// it, what it holds, the delimiter closing it, and the line break after it. The parser takes
    /// either delimiter at either end, so a mismatched pair is enclosed in braces as well.
    private static final Pattern DELIMITED_ENTRY = Pattern.compile("(?s)(\\s*@[^{(]*)([{(])(.*)([})])(\\s*)");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Encloses an entry in braces instead of parentheses.";
    }

    @Override
    public List<Finding> scan(BibEntry entry) {
        return braced(entry)
                .map(_ -> List.of(new Finding(this, entry, Optional.empty(),
                        "entry is enclosed in parentheses instead of braces", Optional.of(EntryParenthesesRule::encloseInBraces))))
                .orElse(List.of());
    }

    /// Setting the serialization marks the entry unchanged, so whether it had changed is put back:
    /// an entry edited before the save has to be written anew, or the edit would be lost.
    private static Optional<FieldChange> encloseInBraces(BibEntry entry) {
        braced(entry).ifPresent(braced -> {
            boolean changed = entry.hasChanged();
            entry.setParsedSerialization(braced);
            entry.setChanged(changed);
        });
        return Optional.empty();
    }

    /// The serialization `entry` was read from, enclosed in braces; nothing where it is enclosed in
    /// them already, or where it was not read from a file at all.
    ///
    /// The comments above the entry are skipped by their length rather than searched for an `@`,
    /// since they may hold one -- and a parenthesis after it.
    private static Optional<String> braced(BibEntry entry) {
        String serialization = entry.getParsedSerialization();
        String comments = entry.getUserComments();
        if (!serialization.startsWith(comments)) {
            return Optional.empty();
        }
        Matcher matcher = DELIMITED_ENTRY.matcher(serialization).region(comments.length(), serialization.length());
        if (!matcher.matches() || ("{".equals(matcher.group(2)) && "}".equals(matcher.group(4)))) {
            return Optional.empty();
        }
        return Optional.of(comments + matcher.group(1) + "{" + matcher.group(3) + "}" + matcher.group(5));
    }
}
