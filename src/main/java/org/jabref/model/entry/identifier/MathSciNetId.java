package org.jabref.model.entry.identifier;

import java.util.Objects;

import org.jabref.model.strings.StringUtil;

/**
 * Article identifier for MathSciNet (also sometimes called "MRNumber")
 */
public class MathSciNetId {

    private String identifier;

    public MathSciNetId(String identifier) {
        this.identifier = Objects.requireNonNull(identifier);
    }

    public static MathSciNetId fromString(String mrNumberRaw) {
        // Take everything before whitespace or open bracket, so something like `619693 (82j:58046)` gets parsed correctly
        return new MathSciNetId(StringUtil.tokenizeToList(mrNumberRaw, " (").get(0));
    }

    @Override
    public String toString() {
        return identifier;
    }

    /**
     * Get URL in online database.
     */
    public String getItemUrl() {
        return "http://www.ams.org/mathscinet-getitem?mr=" + identifier;
    }
}
