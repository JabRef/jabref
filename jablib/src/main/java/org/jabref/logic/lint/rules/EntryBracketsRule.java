package org.jabref.logic.lint.rules;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bibtex.EntryBracket;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.Rule;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.LintSettings;

import org.jspecify.annotations.NullMarked;

/// Encloses an entry in the [EntryBracket] the configuration names: braces, unless it names
/// parentheses, `@Article(key, ...)`.
///
/// BibTeX accepts both, and a save writes the configured one, but only for an entry it writes anew:
/// one nothing changed is written back the way it was read. A library therefore keeps the other
/// bracket entry by entry until each one happens to be edited, and a formatter run, which writes
/// every entry anew, replaces them without saying why. This rule reports them, and repairs them in
/// the entries a save would otherwise leave alone.
///
/// Unlike the other rules, it is not about a field but about how the entry was read: the repair
/// replaces the two delimiters in the entry's parsed serialization and keeps everything between
/// them, so that a save writing the entry back as it was read changes those two characters and
/// nothing else. No field changes, so there is nothing to undo.
@NullMarked
public class EntryBracketsRule implements Rule {
    public static final String ID = "entry-brackets";

    /// The name of the value this rule takes, an [EntryBracket#asKey].
    public static final String BRACKET = "bracket";

    /// An entry as the parser read it, past the comments above it: its type, the delimiter opening
    /// it, what it holds, the delimiter closing it, and the line break after it. The parser takes
    /// either delimiter at either end, so a mismatched pair is enclosed in the configured bracket as
    /// well.
    private static final Pattern DELIMITED_ENTRY = Pattern.compile("(?s)(\\s*@[^{(]*)([{(])(.*)([})])(\\s*)");

    private final EntryBracket bracket;

    public EntryBracketsRule() {
        this(EntryBracket.BRACES);
    }

    public EntryBracketsRule(EntryBracket bracket) {
        this.bracket = bracket;
    }

    /// What a save configured by `settings` encloses an entry in that it writes anew: the bracket
    /// this rule goes by where it runs, and braces, as ever, where it does not.
    public static EntryBracket bracketOf(LintSettings settings) {
        if (!settings.runs(ID)) {
            return EntryBracket.BRACES;
        }
        return bracketIn(settings.valuesOf(ID)).orElse(EntryBracket.BRACES);
    }

    private static Optional<EntryBracket> bracketIn(Map<String, String> values) {
        if (values.isEmpty()) {
            return Optional.of(EntryBracket.BRACES);
        }
        if (values.size() > 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(BRACKET)).flatMap(EntryBracket::fromKey);
    }

    @Override
    public Optional<Rule> configuredWith(Map<String, String> values) {
        return bracketIn(values).map(EntryBracketsRule::new);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return Localization.lang("Encloses an entry in braces, or in parentheses where it is configured to.");
    }

    @Override
    public List<Finding> scan(BibEntry entry) {
        return enclosed(entry)
                .map(_ -> List.of(new Finding(this, entry, Optional.empty(), message(), Optional.of(this::enclose))))
                .orElse(List.of());
    }

    private String message() {
        return switch (bracket) {
            case BRACES ->
                    Localization.lang("entry is enclosed in parentheses instead of braces");
            case PARENTHESES ->
                    Localization.lang("entry is enclosed in braces instead of parentheses");
        };
    }

    /// Setting the serialization marks the entry unchanged, so whether it had changed is put back:
    /// an entry edited before the save has to be written anew, or the edit would be lost.
    private Optional<FieldChange> enclose(BibEntry entry) {
        enclosed(entry).ifPresent(enclosed -> {
            boolean changed = entry.hasChanged();
            entry.setParsedSerialization(enclosed);
            entry.setChanged(changed);
        });
        return Optional.empty();
    }

    /// The serialization `entry` was read from, enclosed in the bracket; nothing where it is enclosed
    /// in it already, or where it was not read from a file at all.
    ///
    /// The comments above the entry are skipped by their length rather than searched for an `@`,
    /// since they may hold one -- and a parenthesis after it.
    private Optional<String> enclosed(BibEntry entry) {
        String serialization = entry.getParsedSerialization();
        String comments = entry.getUserComments();
        if (!serialization.startsWith(comments)) {
            return Optional.empty();
        }
        Matcher matcher = DELIMITED_ENTRY.matcher(serialization).region(comments.length(), serialization.length());
        if (!matcher.matches() || (bracket.getOpening().equals(matcher.group(2)) && bracket.getClosing().equals(matcher.group(4)))) {
            return Optional.empty();
        }
        return Optional.of(comments + matcher.group(1) + bracket.getOpening() + matcher.group(3) + bracket.getClosing() + matcher.group(5));
    }
}
