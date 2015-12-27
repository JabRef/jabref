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
package net.sf.jabref.openoffice;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.exporter.layout.LayoutFormatter;
import net.sf.jabref.exporter.layout.format.FormatChars;
import net.sf.jabref.bst.VM;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Wrapper for using JabRef's bst engine for formatting OO bibliography.
 */
class BstWrapper {

    private final LayoutFormatter formatter = new FormatChars();
    private VM vm;

    private static final Log LOGGER = LogFactory.getLog(BstWrapper.class);

    private static final Pattern BIB_ITEM_TAG = Pattern.compile("\\\\[a-zA-Z]*item\\{.*\\}");


    public BstWrapper() {

    }

    /**
     * Set the bst file to be used for processing. This method will initiate parsing
     * of the bst file.
     * @param f The bst file to load.
     * @throws IOException On IO errors.
     * @throws RecognitionException On parsing errors.
     */
    public void loadBstFile(File f) throws IOException, RecognitionException {
        vm = new VM(f);
    }

    /**
     * Use the instructions of the loaded bst file for processing a collection of entries.
     * @param entries The entries to process.
     * @param database The database the entries belong to.
     * @return A Map of the entries' bibtex keys linking to their processed strings.
     */
    public Map<String, String> processEntries(Collection<BibEntry> entries, BibDatabase database) {
        // TODO: how to handle uniquefiers?

        // TODO: need handling of crossrefs?
        String result = vm.run(entries);
        return parseResult(result);
    }



    private Map<String, String> parseResult(String result) {
        Map<String, String> map = new HashMap<>();
        // Look through for instances of \bibitem :
        Matcher m = BstWrapper.BIB_ITEM_TAG.matcher(result);
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Integer> endIndices = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        while (m.find()) {
            if (!indices.isEmpty()) {
                endIndices.add(m.start());
            }
            LOGGER.debug(m.start() + "  " + m.end());
            String tag = m.group();
            String key = tag.substring(9, tag.length() - 1);
            indices.add(m.end());
            keys.add(key);
        }
        int lastI = result.lastIndexOf("\\end{thebibliography}");
        if ((lastI > 0) && (lastI > indices.get(indices.size() - 1))) {
            endIndices.add(lastI);
        }
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            int index = indices.get(i);
            int endIndex = endIndices.get(i);
            String part = result.substring(index, endIndex);
            map.put(key, formatter.format(part.trim().replaceAll("\\\\newblock ", " ")));
        }

        return map;
    }
}
