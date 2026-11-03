package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;

/// Pure codec for `JABREF_<key>[, JABREF_<key2> ...] <TYPE> <uid>`.
/// It never reads legacy `JR_cite` or `CID_n` formats or Zotero marks.
/// Keys exclude Unicode whitespace and commas. Uids are opaque identifiers containing ASCII letters,
/// digits, `_` and `-`, without conflicting with page-info property separators.
@NullMarked
public final class MarkCodec {

    private static final String PREFIX = "JABREF_";
    private static final Pattern KEY = Pattern.compile("[^\\s,]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern KEY_SEPARATOR = Pattern.compile(", ");
    private static final Pattern UNIQUE_ID = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern MARK = Pattern.compile(
            "(JABREF_[^\\s,]+(?:, JABREF_[^\\s,]+)*) (NORMAL|IN_TEXT|EMPTY|NOPAR|AUTHOR_ONLY|YEAR_ONLY) ([A-Za-z0-9_-]+)",
            Pattern.UNICODE_CHARACTER_CLASS);

    private MarkCodec() {
    }

    public static String encode(JabRefMarkData mark) {
        StringJoiner keys = new StringJoiner(", ");
        mark.citationKeys().forEach(key -> keys.add(PREFIX + key));
        return keys + " " + encodeType(mark.citationType()) + " " + mark.uniqueId();
    }

    public static JabRefMarkData decode(String name) throws MarkCodecException {
        Matcher matcher = MARK.matcher(name);
        if (!matcher.matches()) {
            throw new MarkCodecException(MarkCodecException.Reason.INVALID_MARK_NAME, "Not a canonical JabRef mark name");
        }

        List<String> keys = KEY_SEPARATOR.splitAsStream(matcher.group(1))
                                         .map(key -> key.substring(PREFIX.length()))
                                         .toList();
        return new JabRefMarkData(keys, matcher.group(3), decodeType(matcher.group(2)));
    }

    /// Recognition is independent of the active adapters; using this codec does not enable document reads.
    public static boolean isCanonicalMarkName(String name) {
        return MARK.matcher(name).matches();
    }

    static void validate(List<String> citationKeys, String uniqueId) throws MarkCodecException {
        if (citationKeys.isEmpty()) {
            throw new MarkCodecException(MarkCodecException.Reason.EMPTY_KEYS, "A mark must contain at least one citation key");
        }
        for (String key : citationKeys) {
            if (!KEY.matcher(key).matches()) {
                throw new MarkCodecException(MarkCodecException.Reason.INVALID_KEY, "A citation key must be nonempty and contain neither whitespace nor commas");
            }
        }
        if (!UNIQUE_ID.matcher(uniqueId).matches()) {
            throw new MarkCodecException(MarkCodecException.Reason.INVALID_UNIQUE_ID, "A mark uid must contain only ASCII letters, digits, underscores or hyphens");
        }
    }

    private static String encodeType(CitationType type) {
        return switch (type) {
            case AUTHORYEAR_PAR ->
                    "NORMAL";
            case AUTHORYEAR_INTEXT ->
                    "IN_TEXT";
            case INVISIBLE_CIT ->
                    "EMPTY";
            case AUTHORYEAR_NOPAR ->
                    "NOPAR";
            case AUTHOR_ONLY ->
                    "AUTHOR_ONLY";
            case YEAR_ONLY ->
                    "YEAR_ONLY";
        };
    }

    private static CitationType decodeType(String type) throws MarkCodecException {
        return switch (type) {
            case "NORMAL" ->
                    CitationType.AUTHORYEAR_PAR;
            case "IN_TEXT" ->
                    CitationType.AUTHORYEAR_INTEXT;
            case "EMPTY" ->
                    CitationType.INVISIBLE_CIT;
            case "NOPAR" ->
                    CitationType.AUTHORYEAR_NOPAR;
            case "AUTHOR_ONLY" ->
                    CitationType.AUTHOR_ONLY;
            case "YEAR_ONLY" ->
                    CitationType.YEAR_ONLY;
            default ->
                    throw new MarkCodecException(MarkCodecException.Reason.INVALID_MARK_NAME, "Unknown canonical citation type");
        };
    }
}
