package org.jabref.model.metadata;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;

import org.jspecify.annotations.NullMarked;

/// What one entry of a library's formatting configuration is about: a rule, on one field or on
/// every field the rule looks at.
///
/// Naming a field narrows a mode to that field, which is how a library says "lower-case everything,
/// but only report it on the title". Which of two selectors that both cover a field wins is
/// [LintSettings]'s business, not this record's.
///
/// @param field  the field the mode is about, or empty for every field the rule looks at
/// @param ruleId the [org.jabref.logic.lint.rule.Rule#id] the mode is about
@NullMarked
public record RuleSelector(Optional<Field> field, String ruleId) {
    /// What a selector may not contain yet -- see [#parse].
    private static final Pattern NOT_READ_YET = Pattern.compile("[,/\\s]");

    /// The rule wherever it looks.
    public static RuleSelector of(String ruleId) {
        return new RuleSelector(Optional.empty(), ruleId);
    }

    /// The rule on that field alone.
    public static RuleSelector on(Field field, String ruleId) {
        return new RuleSelector(Optional.of(field), ruleId);
    }

    /// Whether this is about every field the rule looks at, rather than about one of them.
    ///
    /// Naming no field says so outright. So does naming `all` or `all-text-fields`, which a Save
    /// Action may be configured with: the rule has already decided which fields of that class it
    /// visits, so such a selector is about all of them and not about a field called `all`. Without
    /// this, a Save Action configured for every text field would be named by a selector that no
    /// finding of it ever matches, and it would quietly stop running.
    public boolean coversEveryField() {
        return field.map(named -> namesEveryField(InternalField.INTERNAL_ALL_FIELD, named)
                                || namesEveryField(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, named))
                    .orElse(true);
    }

    /// By name rather than by identity, since a selector read from a file carries whatever
    /// [FieldFactory#parseField] made of the name, which need not be the field it stands for.
    private static boolean namesEveryField(InternalField marker, Field named) {
        return marker.getName().equalsIgnoreCase(named.getName());
    }

    /// How this selector is spelled in a library's formatting configuration.
    public String asKey() {
        return field.map(named -> named.getName() + ":" + ruleId).orElse(ruleId);
    }

    /// The selector spelled that way, or nothing for a spelling this JabFix does not read.
    ///
    /// `rule-id` names the rule wherever it looks; `field:rule-id` narrows it to that field. The
    /// rule id is what stands after the *last* colon: a rule id is kebab-case and never contains
    /// one, while a BibTeX field name may, so `note:de:lower-case` is `lower-case` on `note:de`.
    /// The magic comments above an entry tell the two apart by the fields that entry carries, which
    /// is a thing a library-wide key has no access to.
    ///
    /// A comma-separated list and a regex between slashes are what those comments accept, and they
    /// may be accepted here too one day. Until they are, a key that looks like one is not read at
    /// all, rather than read as a field name that happens to contain a comma -- so that it cannot
    /// quietly mean one thing now and another later.
    public static Optional<RuleSelector> parse(String key) {
        if (key.isBlank() || NOT_READ_YET.matcher(key).find()) {
            return Optional.empty();
        }
        int lastColon = key.lastIndexOf(':');
        if (lastColon < 0) {
            return Optional.of(of(key));
        }
        String fieldName = key.substring(0, lastColon);
        String ruleId = key.substring(lastColon + 1);
        if (fieldName.isEmpty() || ruleId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(on(FieldFactory.parseField(fieldName), ruleId));
    }
}
