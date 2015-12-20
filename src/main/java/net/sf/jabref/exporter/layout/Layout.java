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
package net.sf.jabref.exporter.layout;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Main class for formatting DOCUMENT ME!
 */
public class Layout {

    private final LayoutEntry[] layoutEntries;

    private final ArrayList<String> missingFormatters = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(Layout.class);

    public Layout(Vector<StringInt> parsedEntries, String classPrefix) {
        StringInt si;
        Vector<LayoutEntry> tmpEntries = new Vector<>(parsedEntries.size());

        Vector<StringInt> blockEntries = null;
        LayoutEntry le;
        String blockStart = null;

        for (StringInt parsedEntry : parsedEntries) {
            si = parsedEntry;
            // TODO: Rewrite using switch
            if ((si.i == LayoutHelper.IS_LAYOUT_TEXT) || (si.i == LayoutHelper.IS_SIMPLE_FIELD)) {
                // Do nothing
            } else if (si.i == LayoutHelper.IS_FIELD_START) {
                blockEntries = new Vector<>();
                blockStart = si.s;
            } else if (si.i == LayoutHelper.IS_FIELD_END) {
                if ((blockStart != null) && (blockEntries != null)) {
                    if (blockStart.equals(si.s)) {
                        blockEntries.add(si);
                        le = new LayoutEntry(blockEntries, classPrefix, LayoutHelper.IS_FIELD_START);
                        tmpEntries.add(le);
                        blockEntries = null;
                    } else {
                        LOGGER.debug(blockStart + '\n' + si.s);
                        LOGGER.warn("Nested field entries are not implemented!");
                        Thread.dumpStack();
                    }
                }
            } else if (si.i == LayoutHelper.IS_GROUP_START) {
                blockEntries = new Vector<>();
                blockStart = si.s;
            } else if (si.i == LayoutHelper.IS_GROUP_END) {
                if ((blockStart != null) && (blockEntries != null)) {
                    if (blockStart.equals(si.s)) {
                        blockEntries.add(si);
                        le = new LayoutEntry(blockEntries, classPrefix, LayoutHelper.IS_GROUP_START);
                        tmpEntries.add(le);
                        blockEntries = null;
                    } else {
                        LOGGER.warn("Nested field entries are not implemented!");
                        Thread.dumpStack();
                    }
                }
            } else if (si.i == LayoutHelper.IS_OPTION_FIELD) {
                // Do nothing
            }

            if (blockEntries == null) {
                tmpEntries.add(new LayoutEntry(si, classPrefix));
            } else {
                blockEntries.add(si);
            }
        }

        layoutEntries = new LayoutEntry[tmpEntries.size()];

        for (int i = 0; i < tmpEntries.size(); i++)
        {
            layoutEntries[i] = tmpEntries.get(i);
            // Note if one of the entries has an invalid formatter:
            if (layoutEntries[i].isInvalidFormatter()) {
                missingFormatters.addAll(layoutEntries[i].getInvalidFormatters());
            }

            //System.out.println(layoutEntries[i].text);
        }
    }

    public void setPostFormatter(LayoutFormatter formatter) {
        for (LayoutEntry layoutEntry : layoutEntries) {
            layoutEntry.setPostFormatter(formatter);
        }
    }

    public String doLayout(BibEntry bibtex, BibDatabase database) {
        return doLayout(bibtex, database, null);
    }

    /**
     * Returns the processed bibtex entry. If the database argument is
     * null, no string references will be resolved. Otherwise all valid
     * string references will be replaced by the strings' contents. Even
     * recursive string references are resolved.
     */
    public String doLayout(BibEntry bibtex, BibDatabase database, List<String> wordsToHighlight) {
        StringBuilder sb = new StringBuilder(100);

        for (LayoutEntry layoutEntry : layoutEntries) {
            String fieldText = layoutEntry.doLayout(bibtex, database, wordsToHighlight);

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
    public String doLayout(BibDatabase database, Charset encoding)
    {
        //System.out.println("LAYOUT: " + bibtex.getId());
        StringBuilder sb = new StringBuilder(100);
        String fieldText;
        boolean previousSkipped = false;

        for (LayoutEntry layoutEntry : layoutEntries) {
            fieldText = layoutEntry.doLayout(database, encoding);

            if (fieldText == null) {
                fieldText = "";
                if (previousSkipped) {
                    int eol = 0;

                    while ((eol < fieldText.length()) &&
                            ((fieldText.charAt(eol) == '\n') ||
                                    (fieldText.charAt(eol) == '\r'))) {
                        eol++;
                    }

                    if (eol < fieldText.length()) {
                        sb.append(fieldText.substring(eol));
                    }
                }
            } else {
                sb.append(fieldText);
            }

            previousSkipped = false;
        }

        return sb.toString();
    }

    // added section - end (arudert)

    public ArrayList<String> getMissingFormatters() {
        return missingFormatters;
    }
}
