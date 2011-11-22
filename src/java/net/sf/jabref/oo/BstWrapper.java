/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.oo;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.FormatChars;
import net.sf.jabref.bst.VM;
import org.antlr.runtime.RecognitionException;

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
public class BstWrapper {

    LayoutFormatter formatter = new FormatChars();
    VM vm = null;

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
    public Map<String,String> processEntries(Collection<BibtexEntry> entries, BibtexDatabase database) {
        // TODO: how to handle uniquefiers?

        // TODO: need handling of crossrefs?
        String result = vm.run(entries);
        return parseResult(result);
    }

    static Pattern bibitemTag = Pattern.compile("\\\\[a-zA-Z]*item\\{.*\\}");

    private Map<String,String> parseResult(String result) {
        Map<String,String> map = new HashMap<String,String>();
        // Look through for instances of \bibitem :
        Matcher m =  bibitemTag.matcher(result);
        ArrayList<Integer> indices = new ArrayList<Integer>();
        ArrayList<Integer> endIndices = new ArrayList<Integer>();
        ArrayList<String> keys = new ArrayList<String>();
        while (m.find()) {
            if (indices.size() > 0)
                endIndices.add(m.start());
            System.out.println(m.start()+"  "+m.end());
            String tag = m.group();
            String key = tag.substring(9, tag.length()-1);
            indices.add(m.end());
            keys.add(key);
        }
        int lastI = result.lastIndexOf("\\end{thebibliography}");
        if ((lastI > 0) && (lastI > indices.get(indices.size()-1)))
            endIndices.add(lastI);
        for (int i=0; i<keys.size(); i++) {
            String key = keys.get(i);
            int index = indices.get(i);
            int endIndex = endIndices.get(i);
            String part = result.substring(index, endIndex);
            map.put(key, formatter.format(part.trim().replaceAll("\\\\newblock ", " ")));
        }

        return map;
    }
}
