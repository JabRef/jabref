package org.jabref.gui.externalfiles;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameParser {

    private static final Pattern FIELD_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    public static Set<String> parseFields(String pattern) {
        Set<String> fields = new HashSet<>();
        Matcher matcher = FIELD_PATTERN.matcher(pattern);
        while (matcher.find()) {
            String fieldExpression = matcher.group(1).trim();
            fields.add(fieldExpression);
        }
        return fields;
    }
}

