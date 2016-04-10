/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.layout;

import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Main class for formatting DOCUMENT ME!
 */
public class Layout {

    private final List<LayoutEntry> layoutEntries;

    private final List<String> missingFormatters = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(Layout.class);


    public Layout(List<StringInt> parsedEntries, JournalAbbreviationRepository repository) {
        List<LayoutEntry> tmpEntries = new ArrayList<>(parsedEntries.size());

        List<StringInt> blockEntries = null;
        LayoutEntry le;
        String blockStart = null;

        for (StringInt parsedEntry : parsedEntries) {
            switch (parsedEntry.i) {
            case LayoutHelper.IS_LAYOUT_TEXT:
            case LayoutHelper.IS_SIMPLE_FIELD:
            case LayoutHelper.IS_OPTION_FIELD:
                // Do nothing
                break;
            case LayoutHelper.IS_FIELD_START:
            case LayoutHelper.IS_GROUP_START:
                blockEntries = new ArrayList<>();
                blockStart = parsedEntry.s;
                break;
            case LayoutHelper.IS_FIELD_END:
            case LayoutHelper.IS_GROUP_END:
                if ((blockStart != null) && (blockEntries != null)) {
                    if (blockStart.equals(parsedEntry.s)) {
                        blockEntries.add(parsedEntry);
                        le = new LayoutEntry(blockEntries,
                                parsedEntry.i == LayoutHelper.IS_FIELD_END ? LayoutHelper.IS_FIELD_START : LayoutHelper.IS_GROUP_START,
                                repository);
                        tmpEntries.add(le);
                        blockEntries = null;
                    } else {
                        LOGGER.debug(blockStart + '\n' + parsedEntry.s);
                        LOGGER.warn("Nested field/group entries are not implemented!");
                        Thread.dumpStack();
                    }
                }
                break;
            default:
                break;
            }

            if (blockEntries == null) {
                tmpEntries.add(new LayoutEntry(parsedEntry, repository));
            } else {
                blockEntries.add(parsedEntry);
            }
        }

        layoutEntries = new ArrayList<>(tmpEntries);

        for (LayoutEntry layoutEntry : layoutEntries) {
            missingFormatters.addAll(layoutEntry.getInvalidFormatters());
        }
    }

    public void setPostFormatter(LayoutFormatter formatter) {
        for (LayoutEntry layoutEntry : layoutEntries) {
            layoutEntry.setPostFormatter(formatter);
        }
    }

    public String doLayout(BibEntry bibtex, BibDatabase database) {
        return doLayout(bibtex, database, Optional.empty());
    }

    /**
     * Returns the processed bibtex entry. If the database argument is
     * null, no string references will be resolved. Otherwise all valid
     * string references will be replaced by the strings' contents. Even
     * recursive string references are resolved.
     */
    public String doLayout(BibEntry bibtex, BibDatabase database, Optional<Pattern> highlightPattern) {
        StringBuilder sb = new StringBuilder(100);

        for (LayoutEntry layoutEntry : layoutEntries) {
            String fieldText = layoutEntry.doLayout(bibtex, database, highlightPattern);

            // 2005.05.05 M. Alver
            // The following change means we treat null fields as "". This is to fix the
            // problem of whitespace disappearing after missing fields. Hoping there are
            // no side effects.
            if (fieldText == null) {
                fieldText = "";
            }

            sb.append(fieldText);
        }

        return sb.toString();
    }

    /**
     * Returns the processed text. If the database argument is
     * null, no string references will be resolved. Otherwise all valid
     * string references will be replaced by the strings' contents. Even
     * recursive string references are resolved.
     */
    public String doLayout(BibDatabaseContext databaseContext, Charset encoding) {
        StringBuilder sb = new StringBuilder(100);
        String fieldText;

        for (LayoutEntry layoutEntry : layoutEntries) {
            fieldText = layoutEntry.doLayout(databaseContext, encoding);

            if (fieldText == null) {
                fieldText = "";
            }

            sb.append(fieldText);
        }

        return sb.toString();
    }

    // added section - end (arudert)

    public List<String> getMissingFormatters() {
        return new ArrayList<>(missingFormatters);
    }
}
