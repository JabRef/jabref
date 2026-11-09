package org.jabref.logic.bibtex;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// What encloses an entry in a `.bib` file. BibTeX takes `@Article{ ... }` as well as `@Article( ... )`.
@NullMarked
public enum EntryBracket {
    BRACES("{", "}"),
    PARENTHESES("(", ")");

    private final String opening;
    private final String closing;

    EntryBracket(String opening, String closing) {
        this.opening = opening;
        this.closing = closing;
    }

    public String getOpening() {
        return opening;
    }

    public String getClosing() {
        return closing;
    }

    /// How this bracket is spelled in a library's formatting configuration.
    public String asKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /// The bracket spelled that way, or nothing for a spelling this JabRef does not know.
    public static Optional<EntryBracket> fromKey(String key) {
        return Arrays.stream(values())
                     .filter(bracket -> bracket.asKey().equals(key))
                     .findFirst();
    }
}
