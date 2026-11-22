package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;

/// Reads the former JStyle encoding without making it part of canonical mark storage.
// [impl->req~openoffice.legacy-jstyle-migration~1]
@NullMarked
record LegacyJStyleMark(List<String> citationKeys, CitationType citationType) {
    private static final Pattern MARK = Pattern.compile("JR_cite[0-9]*_([1-6])_(.*)", Pattern.DOTALL);

    static boolean isLegacyMarkName(String name) {
        return MARK.matcher(name).matches();
    }

    static LegacyJStyleMark decode(String name) throws JabRefMarkCodecException {
        Matcher matcher = MARK.matcher(name);
        if (!matcher.matches()) {
            throw new JabRefMarkCodecException(JabRefMarkCodecException.Reason.INVALID_MARK_NAME, "Not a legacy JStyle mark name");
        }
        List<String> keys = List.of(matcher.group(2).split(",", -1));
        JabRefMarkCodec.validate(keys, "legacy");
        CitationType type = switch (matcher.group(1)) {
            case "1" -> CitationType.AUTHORYEAR_PAR;
            case "2" -> CitationType.AUTHORYEAR_INTEXT;
            case "3" -> CitationType.INVISIBLE_CIT;
            case "4" -> CitationType.AUTHORYEAR_NOPAR;
            case "5" -> CitationType.AUTHOR_ONLY;
            case "6" -> CitationType.YEAR_ONLY;
            default -> throw new JabRefMarkCodecException(JabRefMarkCodecException.Reason.INVALID_MARK_NAME, "Unknown legacy JStyle citation type");
        };
        return new LegacyJStyleMark(keys, type);
    }
}
