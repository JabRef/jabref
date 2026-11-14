package org.jabref.logic.openoffice.style;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.openoffice.ootext.OOText;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class JStyleCitationKeyFormatter {

    private JStyleCitationKeyFormatter() {
    }

    static OOText format(List<String> keys, JStyle style) {
        String text = keys.stream().map(StringEscapeUtils::escapeHtml4).collect(Collectors.joining(","));
        return OOText.fromString(style.getCitationGroupMarkupBefore() + text + style.getCitationGroupMarkupAfter());
    }
}
