package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/// 
/// LayoutFormatter that removes the space between abbreviated First names
/// 
/// 
/// What out that this regular expression might also remove other spaces that fit the pattern.
/// 
/// 
/// Example: J. R. R. Tolkien becomes J.R.R. Tolkien.
/// 
/// 
/// See Testcase for more examples.
/// 
public class NoSpaceBetweenAbbreviations implements LayoutFormatter {

    /*
     * Match '.' followed by spaces followed by uppercase char followed by '.'
     * but don't include the last dot into the capturing group.
     *
     * Replace the match by removing the spaces.
     *
     * @see org.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        return fieldText.replaceAll("\\.\\s+(\\p{Lu})(?=\\.)", "\\.$1");
    }
}
