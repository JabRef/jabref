package org.jabref.logic.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConferenceUtil {
    public static Optional<String> extractAcronym(String title) {
        Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(title);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();

    }
}
