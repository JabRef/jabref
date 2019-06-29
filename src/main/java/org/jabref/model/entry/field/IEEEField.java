package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Optional;

/**
 * IEEE BSTctl fields
 */
public enum IEEEField implements Field {
    CTLALT_STRETCH_FACTOR("ctlalt_stretch_factor"),
    CTLDASH_REPEATED_NAMES("ctldash_repeated_names"),
    CTLMAX_NAMES_FORCED_ETAL("ctlmax_names_forced_etal"),
    CTLNAME_FORMAT_STRING("ctlname_format_string"),
    CTLNAME_LATEX_CMD("ctlname_latex_cmd"),
    CTLNAME_URL_PREFIX("ctlname_url_prefix"),
    CTLNAMES_SHOW_ETAL("ctlnames_show_etal"),
    CTLUSE_ALT_SPACING("ctluse_alt_spacing"),
    CTLUSE_ARTICLE_NUMBER("ctluse_article_number"),
    CTLUSE_FORCED_ETAL("ctluse_forced_etal"),
    CTLUSE_PAPER("ctluse_paper"),
    CTLUSE_URL("ctluse_url");

    private final String name;

    IEEEField(String name) {
        this.name = name;
    }

    public static Optional<IEEEField> fromName(String name) {
        return Arrays.stream(IEEEField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }

    @Override
    public String getName() {
        return name;
    }
}
