package org.jabref.logic.lint.rule;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.UnknownField;

import org.jspecify.annotations.NullMarked;

/// Reports a magic comment that switches nothing off.
///
/// A misspelled id in `% jabref-format-ignore` switches nothing off, which is the one thing a user of it
/// never notices: the entry is repaired as if the comment were not there. The id is therefore held
/// against the rules of the run, and one that names none of them is reported like any other
/// finding -- with no fix, since only the person who wrote the comment knows what was meant.
///
/// A token the parser could not take apart, such as `author:` or `author:title:page-ranges`, is
/// kept as one rule id by [Suppressions] and so lands here as well. A regex is reported when it
/// matches no rule of the run. Either is reported on each field the token names, or on the entry
/// when it names none.
///
/// The field of `field:rule` is reported on the same grounds, but only when JabRef does not know it
/// *and* the entry does not carry it: any name is a valid BibTeX field, so a standard field the
/// entry happens to lack may well be meant for what is added to it later, while `autor:` on an
/// entry that has no such field is a typo in all but name. A field regex is reported when it
/// matches neither a field JabRef knows nor one the entry carries.
///
/// The rule is not one of [RuleSet#all]: it needs the ids of the run it belongs to, which
/// [org.jabref.logic.lint.JabFix] hands it. Its own id counts as known, so `% jabref-format-ignore
/// magic-comment` silences it where a comment is meant as it is written.
@NullMarked
public class MagicCommentRule implements Rule {
    private static final String ID = "magic-comment";

    private static final Set<Field> KNOWN_FIELDS = FieldFactory.getAllFieldsWithOutInternal();

    private final Set<String> knownRuleIds;

    /// @param knownRuleIds the ids of the rules the run applies
    public MagicCommentRule(Set<String> knownRuleIds) {
        this.knownRuleIds = Stream.concat(knownRuleIds.stream(), Stream.of(ID)).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Reports a magic comment that names no rule.";
    }

    @Override
    public List<Finding> scan(BibEntry entry) {
        return Suppressions.in(entry).tokens().stream()
                           .flatMap(token -> Stream.concat(forRules(entry, token), forFields(entry, token)))
                           .toList();
    }

    private Stream<Finding> forRules(BibEntry entry, Suppressions.Token token) {
        Stream<String> unknownIds = token.rules().stream()
                                         .filter(ruleId -> !knownRuleIds.contains(ruleId))
                                         .map("switches off \"%s\", which is no rule of this run"::formatted);
        Stream<String> unmatchedRegexes = token.ruleRegexes().stream()
                                               .filter(regex -> knownRuleIds.stream().noneMatch(ruleId -> Suppressions.Token.ruleMatches(regex, ruleId)))
                                               .map("switches off /%s/, which matches no rule of this run"::formatted);

        List<Optional<Field>> reportedOn = token.fields().isEmpty()
                                           ? List.of(Optional.empty())
                                           : token.fields().stream().map(Optional::of).toList();
        return Stream.concat(unknownIds, unmatchedRegexes)
                     .flatMap(what -> reportedOn.stream().map(field -> finding(entry, field, what)));
    }

    private Stream<Finding> forFields(BibEntry entry, Suppressions.Token token) {
        Stream<Finding> missingFields = token.fields().stream()
                                             .filter(field -> field instanceof UnknownField)
                                             .filter(field -> !entry.hasField(field))
                                             .map(field -> finding(entry, Optional.of(field),
                                                     "names the field \"%s\", which is neither a BibTeX field nor one this entry has".formatted(field.getName())));
        Stream<Finding> unmatchedRegexes = token.fieldRegexes().stream()
                                                .filter(regex -> Stream.concat(KNOWN_FIELDS.stream(), entry.getFields().stream())
                                                                       .noneMatch(field -> Suppressions.Token.fieldMatches(regex, field)))
                                                .map(regex -> finding(entry, Optional.empty(),
                                                        "names the fields /%s/, which match neither a BibTeX field nor one this entry has".formatted(regex)));
        return Stream.concat(missingFields, unmatchedRegexes);
    }

    private Finding finding(BibEntry entry, Optional<Field> field, String what) {
        return new Finding(this, entry, field, "the magic comment " + what, Optional.empty());
    }
}
