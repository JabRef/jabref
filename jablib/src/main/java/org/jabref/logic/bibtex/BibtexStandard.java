package org.jabref.logic.bibtex;

import java.util.Arrays;
import java.util.List;

/// Contains various information or constants about the BibTeX standard.
public class BibtexStandard {
    /// Source of disallowed characters: <https://tex.stackexchange.com/a/408548/9075>
    /// These characters are disallowed in BibTeX keys.
    public static final List<Character> DISALLOWED_CHARACTERS = Arrays.asList('{', '}', '(', ')', ',', '=', '\\', '"', '#', '%', '~', '\'');
}
