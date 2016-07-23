/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.logic.protectterms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProtectTermsLoader {

    private static final Log LOGGER = LogFactory.getLog(ProtectTermsLoader.class);

    private final List<ProtectTermsList> mainList = new ArrayList<>();


    public ProtectTermsLoader(List<String> enabledExternalTermLists, List<String> disabledExternalTermLists) {
        update(enabledExternalTermLists, disabledExternalTermLists);
    }

    public void update(List<String> enabledExternalTermLists, List<String> disabledExternalTermLists) {
        mainList.clear();

        // Read builtin list
        mainList.addAll(ProtectTermsLists.getAllLists());

        // Read external lists
        for (String filename : enabledExternalTermLists) {
            try {
                mainList.add(readTermsFromFile(new File(filename), true));
            } catch (FileNotFoundException e) {
                // The file couldn't be found... should we tell anyone?
                LOGGER.info("Cannot find protected terms file " + filename, e);
            }
        }
        for (String filename : disabledExternalTermLists) {
            try {
                mainList.add(readTermsFromFile(new File(filename), false));
            } catch (FileNotFoundException e) {
                // The file couldn't be found... should we tell anyone?
                LOGGER.info("Cannot find protected terms file " + filename, e);
            }
        }
    }

    public void reloadList(ProtectTermsList list) {
        try {
            ProtectTermsList newList = readTermsFromFile(new File(list.getLocation()), list.isEnabled());
            int index = mainList.indexOf(list);
            if (index >= 0) {
                mainList.set(index, newList);
            } else {
                LOGGER.warn("Problem reloading protected terms file");
            }
        } catch (IOException e) {
            LOGGER.warn("Problem with protected terms file '" + list.getLocation() + "'", e);
        }

    }
    public List<ProtectTermsList> getTermsLists() {
        return mainList;
    }

    public List<String> getProtectedTerms() {
        List<String> result = new ArrayList<>();
        for (ProtectTermsList list : mainList) {
            if (list.isEnabled()) {
                result.addAll(list.getTermList());
            }
        }
        result.sort(null);

        return result;
    }

    public void addFromFile(String fileName, boolean enabled) {
        try {
            mainList.add(readTermsFromFile(new File(fileName), enabled));
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            LOGGER.info("Cannot find protected terms file " + fileName, e);
        }
    }

    public static ProtectTermsList readTermsFromFile(File file, boolean enabled) throws FileNotFoundException {
        LOGGER.debug("Reading term list from file " + file);
        ProtectTermsParser parser = new ProtectTermsParser();
        parser.readTermsFromFile(Objects.requireNonNull(file));
        return parser.getProtectTermsList(enabled);
    }

    public static ProtectTermsList readTermsFromFile(File file, Charset encoding, boolean enabled)
            throws FileNotFoundException {
        LOGGER.debug("Reading term list from file " + file);
        ProtectTermsParser parser = new ProtectTermsParser();
        parser.readTermsFromFile(Objects.requireNonNull(file), Objects.requireNonNull(encoding));
        return parser.getProtectTermsList(enabled);
    }

    public boolean removeTermList(ProtectTermsList termList) {
        return mainList.remove(termList);
    }
}
