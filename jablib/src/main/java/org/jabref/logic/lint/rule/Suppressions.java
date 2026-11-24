package org.jabref.logic.lint.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The rules the magic comments of one entry switch off.
///
/// A magic comment is a BibTeX comment directly above the entry, which JabRef keeps with it as its
/// user comments:
///
/// ```bibtex
/// % jabref-format-ignore surrounding-whitespace author,title:page-ranges /comment-.*/:/normalize-.*/
/// @Article{key,
///   author = {Doe, Jane},
/// }
/// ```
///
/// A token without a colon switches its rules off for the whole entry, `fields:rules` only for
/// those fields. Both sides are comma-separated lists, so `author,title:rule1,rule2` switches both
/// rules off for both fields. An item between slashes is a regex, which has to match a rule id or a
/// field name as a whole -- a field name ignoring case, as BibTeX does. Commas and colons between
/// the slashes belong to the regex, and `\/` is a slash that does not end it. Whitespace still ends
/// the token; a regex spells it `\s`.
///
/// A field name may itself contain a colon, as in `note:de`. Where the entry has such a field, its
/// name is read as a whole, so `note:de:surrounding-whitespace` switches the rule off for that
/// field. A field name cannot contain a comma: BibTeX does not allow one.
///
/// A comment with no token at all switches every rule off for the entry. Several such comments
/// above one entry add up, and every other comment line is left to whoever wrote it.
///
/// Rule ids are not checked here against the rules that exist, because the parser does not know
/// them -- [RuleSet#without] does that for the ids given on the command line. A token that cannot
/// be taken apart -- an empty item as in `author:` or `author,,title:page-ranges`, a colon too many,
/// or a regex that is not closed or does not compile -- is therefore kept whole as one rule id, so
/// that it surfaces as an unknown rule once ids are checked, rather than being dropped here.
///
/// @param wholeEntry whether every rule is switched off for the entry
/// @param tokens     what each token switches off, in the order they were written
@NullMarked
public record Suppressions(boolean wholeEntry, List<Token> tokens) {
    /// Nothing switched off, the state of an entry without magic comments.
    public static final Suppressions NONE = new Suppressions(false, List.of());

    private static final Logger LOGGER = LoggerFactory.getLogger(Suppressions.class);

    private static final Pattern DISABLE_COMMENT = Pattern.compile("^\\s*%+\\s*jabref-format-ignore(?:\\s+(?<rules>.*?))?\\s*$");
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("\\s+");

    /// What stands between the slashes of a regex; a backslash escapes the character after it.
    private static final String REGEX = "(?:\\\\.|[^\\\\/])*";
    private static final String NAME = "[^,:/][^,:]*";
    private static final Pattern RULE_ITEM = Pattern.compile(item(NAME));

    public Suppressions {
        tokens = List.copyOf(tokens);
    }

    /// Reads the magic comments above `entry`.
    public static Suppressions in(BibEntry entry) {
        return parse(entry.getUserComments(), entry.getFields());
    }

    /// Reads the magic comments out of `comments`, the text above an entry that has no field whose
    /// name contains a colon.
    public static Suppressions parse(String comments) {
        return parse(comments, Set.of());
    }

    /// Reads the magic comments out of `comments`, the text above an entry.
    ///
    /// @param fieldsOfEntry the fields of that entry; a name among them that contains a colon is read
    ///                      as a whole
    public static Suppressions parse(String comments, Set<Field> fieldsOfEntry) {
        Syntax syntax = Syntax.forFields(fieldsOfEntry);
        boolean wholeEntry = false;
        List<Token> tokens = new ArrayList<>();

        for (String line : comments.lines().toList()) {
            Matcher comment = DISABLE_COMMENT.matcher(line);
            if (!comment.matches()) {
                continue;
            }
            List<String> written = tokensOf(comment);
            if (written.isEmpty()) {
                wholeEntry = true;
                continue;
            }
            written.stream().map(token -> parseToken(token, syntax)).forEach(tokens::add);
        }
        return new Suppressions(wholeEntry, tokens);
    }

    /// Whether `ruleId` is switched off for the finding at hand.
    ///
    /// @param field the field the finding concerns, empty for a finding about the entry as a whole
    public boolean suppresses(String ruleId, Optional<Field> field) {
        return wholeEntry || tokens.stream().anyMatch(token -> token.suppresses(ruleId, field));
    }

    private static List<String> tokensOf(Matcher comment) {
        return Optional.ofNullable(comment.group("rules"))
                       .map(String::strip)
                       .filter(rules -> !rules.isEmpty())
                       .map(rules -> List.of(TOKEN_SEPARATOR.split(rules)))
                       .orElse(List.of());
    }

    private static Token parseToken(String token, Syntax syntax) {
        return takeApart(token, syntax).orElseGet(() -> new Token(List.of(), List.of(), List.of(token), List.of()));
    }

    private static Optional<Token> takeApart(String token, Syntax syntax) {
        Matcher parts = syntax.token().matcher(token);
        if (!parts.matches()) {
            return Optional.empty();
        }
        String fields = Optional.ofNullable(parts.group("fields")).orElse("");
        String rules = parts.group("rules");

        List<String> fieldRegexes = itemsOf(fields, "regex", syntax.fieldItem());
        List<String> ruleRegexes = itemsOf(rules, "regex", RULE_ITEM);
        if (!allCompile(Stream.concat(fieldRegexes.stream(), ruleRegexes.stream()).toList())) {
            return Optional.empty();
        }
        return Optional.of(new Token(
                itemsOf(fields, "name", syntax.fieldItem()).stream().map(FieldFactory::parseField).toList(),
                fieldRegexes,
                itemsOf(rules, "name", RULE_ITEM),
                ruleRegexes));
    }

    /// @param kind `regex` or `name`, the kind of item to collect from `list`
    private static List<String> itemsOf(String list, String kind, Pattern item) {
        return item.matcher(list).results()
                   .filter(found -> found.start(kind) >= 0)
                   .map(found -> found.group(kind))
                   .toList();
    }

    /// One list item: a regex between slashes, or a name as `name` describes it.
    private static String item(String name) {
        return "/(?<regex>%s)/|(?<name>%s)".formatted(REGEX, name);
    }

    /// Items as `name` describes them, separated by commas.
    private static String list(String name) {
        return "(?:/%1$s/|%2$s)(?:,(?:/%1$s/|%2$s))*".formatted(REGEX, name);
    }

    /// The patterns a token is taken apart with. They depend on the entry, since the name of a
    /// field of it that contains a colon is read as a whole rather than split there.
    ///
    /// @param token     a whole token, with the `fields` and the `rules` side as groups
    /// @param fieldItem one item of the `fields` side
    private record Syntax(Pattern token, Pattern fieldItem) {
        private static final Syntax WITHOUT_COLON_FIELDS = of(NAME);

        static Syntax forFields(Set<Field> fieldsOfEntry) {
            List<String> colonNames = fieldsOfEntry.stream()
                                                   .map(Field::getName)
                                                   .filter(name -> name.contains(":"))
                                                   // longest first, so that `a:b:c` is not read as `a:b`
                                                   .sorted(Comparator.comparingInt(String::length).reversed())
                                                   .map(name -> "(?i:%s)".formatted(Pattern.quote(name)))
                                                   .toList();
            if (colonNames.isEmpty()) {
                return WITHOUT_COLON_FIELDS;
            }
            return of("(?:%s|%s)".formatted(String.join("|", colonNames), NAME));
        }

        private static Syntax of(String fieldName) {
            return new Syntax(
                    Pattern.compile("(?:(?<fields>%s):)?(?<rules>%s)".formatted(list(fieldName), list(NAME))),
                    Pattern.compile(item(fieldName)));
        }
    }

    private static boolean allCompile(List<String> regexes) {
        try {
            regexes.forEach(Pattern::compile);
            return true;
        } catch (PatternSyntaxException e) {
            LOGGER.debug("Keeping a magic comment token with an invalid regex as one rule id", e);
            return false;
        }
    }

    /// What one token switches off: the rules it names, on the fields it names -- or on the whole
    /// entry when it names no field. Regexes are kept as written between the slashes.
    ///
    /// @param fields       the fields named
    /// @param fieldRegexes regexes a field name has to match
    /// @param rules        the rule ids named
    /// @param ruleRegexes  regexes a rule id has to match
    public record Token(List<Field> fields, List<String> fieldRegexes, List<String> rules, List<String> ruleRegexes) {
        public Token {
            fields = List.copyOf(fields);
            fieldRegexes = List.copyOf(fieldRegexes);
            rules = List.copyOf(rules);
            ruleRegexes = List.copyOf(ruleRegexes);
        }

        /// Whether the token names no field, and so covers the whole entry.
        public boolean coversWholeEntry() {
            return fields.isEmpty() && fieldRegexes.isEmpty();
        }

        /// Whether `regex` matches `ruleId` as a whole.
        static boolean ruleMatches(String regex, String ruleId) {
            return Pattern.compile(regex).matcher(ruleId).matches();
        }

        /// Whether `regex` matches the name of `field` as a whole, ignoring case as BibTeX does.
        static boolean fieldMatches(String regex, Field field) {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(field.getName()).matches();
        }

        private boolean suppresses(String ruleId, Optional<Field> field) {
            return namesRule(ruleId) && (coversWholeEntry() || field.filter(this::namesField).isPresent());
        }

        private boolean namesRule(String ruleId) {
            return rules.contains(ruleId) || ruleRegexes.stream().anyMatch(regex -> ruleMatches(regex, ruleId));
        }

        private boolean namesField(Field field) {
            return fields.contains(field) || fieldRegexes.stream().anyMatch(regex -> fieldMatches(regex, field));
        }
    }
}
