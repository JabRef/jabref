package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * IEEE BSTctl fields
 */
public enum IEEEField implements Field {
    CTLALT_STRETCH_FACTOR("ctlalt_stretch_factor", FieldProperty.NUMERIC),
    CTLDASH_REPEATED_NAMES("ctldash_repeated_names", FieldProperty.YES_NO),
    CTLMAX_NAMES_FORCED_ETAL("ctlmax_names_forced_etal", FieldProperty.NUMERIC),
    CTLNAME_FORMAT_STRING("ctlname_format_string", FieldProperty.VERBATIM),
    CTLNAME_LATEX_CMD("ctlname_latex_cmd", FieldProperty.VERBATIM),
    CTLNAME_URL_PREFIX("ctlname_url_prefix", FieldProperty.VERBATIM),
    CTLNAMES_SHOW_ETAL("ctlnames_show_etal", FieldProperty.NUMERIC),
    CTLUSE_ALT_SPACING("ctluse_alt_spacing", FieldProperty.YES_NO),
    CTLUSE_ARTICLE_NUMBER("ctluse_article_number", FieldProperty.YES_NO),
    CTLUSE_FORCED_ETAL("ctluse_forced_etal", FieldProperty.YES_NO),
    CTLUSE_PAPER("ctluse_paper", FieldProperty.YES_NO),
    CTLUSE_URL("ctluse_url", FieldProperty.YES_NO);

    private final String name;
    private final Set<FieldProperty> properties;

    IEEEField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.properties = EnumSet.of(first, rest);
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

    @Override
    public boolean isStandardField() {
        return false;
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return Collections.unmodifiableSet(properties);
    }
}
