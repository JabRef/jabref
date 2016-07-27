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
package net.sf.jabref.logic.protectedterms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProtectedTermsLoader {

    private static final Log LOGGER = LogFactory.getLog(ProtectedTermsLoader.class);

    private final List<ProtectedTermsList> mainList = new ArrayList<>();

    private static final Map<String, String> internalLists = new HashMap<>();

    static {
        internalLists.put("/protectedterms/months_weekdays.terms", Localization.lang("Months and weekdays in English"));
        internalLists.put("/protectedterms/countries_territories.terms",
                Localization.lang("Countries and territories in English"));
        internalLists.put("/protectedterms/electrical_engineering.terms",
                Localization.lang("Electrical engineering terms"));
    }


    public static List<String> getInternalLists() {
        return new ArrayList<>(internalLists.keySet());
    }

    public ProtectedTermsLoader(ProtectedTermsPreferences preferences) {
        update(preferences);
    }

    public void update(ProtectedTermsPreferences preferences) {
        mainList.clear();

        // Read internal lists
        for (String filename : preferences.getEnabledInternalTermLists()) {
            if (internalLists.containsKey(filename)) {
                mainList.add(readProtectedTermsListFromResource(filename, internalLists.get(filename), true));
            } else {
                LOGGER.warn("Protected terms resource '" + filename + "' is no longer available.");
            }
        }
        for (String filename : preferences.getDisabledInternalTermLists()) {
            if (internalLists.containsKey(filename)) {
                mainList.add(readProtectedTermsListFromResource(filename, internalLists.get(filename), false));
            } else {
                LOGGER.warn("Protected terms resource '" + filename + "' is no longer available.");
            }
        }

        // Check if any new internal lists have emerged
        for (String filename : internalLists.keySet()) {
            if (!preferences.getEnabledInternalTermLists().contains(filename)
                    && !preferences.getDisabledInternalTermLists().contains(filename)) {
                // New internal list, add it
                mainList.add(readProtectedTermsListFromResource(filename, internalLists.get(filename), true));
                LOGGER.warn("New protected terms resource '" + filename + "' is available and enabled by default.");
            }
        }

        // Read external lists
        for (String filename : preferences.getEnabledExternalTermLists()) {
            try {
                mainList.add(readProtectedTermsListFromFile(new File(filename), true));
            } catch (FileNotFoundException e) {
                // The file couldn't be found...
                LOGGER.warn("Cannot find protected terms file " + filename, e);
            }
        }
        for (String filename : preferences.getDisabledExternalTermLists()) {
            try {
                mainList.add(readProtectedTermsListFromFile(new File(filename), false));
            } catch (FileNotFoundException e) {
                // The file couldn't be found...
                LOGGER.warn("Cannot find protected terms file " + filename, e);
            }
        }
    }

    public void reloadProtectedTermsList(ProtectedTermsList list) {
        try {
            ProtectedTermsList newList = readProtectedTermsListFromFile(new File(list.getLocation()), list.isEnabled());
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
    public List<ProtectedTermsList> getProtectedTermsLists() {
        return mainList;
    }

    public List<String> getProtectedTerms() {
        Set<String> result = new HashSet<>();
        for (ProtectedTermsList list : mainList) {
            if (list.isEnabled()) {
                result.addAll(list.getTermList());
            }
        }

        return new ArrayList<>(result);
    }

    public void addProtectedTermsListFromFile(String fileName, boolean enabled) {
        try {
            mainList.add(readProtectedTermsListFromFile(new File(fileName), enabled));
        } catch (FileNotFoundException e) {
            // The file couldn't be found...
            LOGGER.warn("Cannot find protected terms file " + fileName, e);
        }
    }

    public static ProtectedTermsList readProtectedTermsListFromResource(String resource, String description, boolean enabled) {
        ProtectedTermsParser parser = new ProtectedTermsParser();
        parser.readTermsFromResource(Objects.requireNonNull(resource), Objects.requireNonNull(description));
        return parser.getProtectTermsList(enabled, true);
    }

    public static ProtectedTermsList readProtectedTermsListFromFile(File file, boolean enabled) throws FileNotFoundException {
        LOGGER.debug("Reading term list from file " + file);
        ProtectedTermsParser parser = new ProtectedTermsParser();
        parser.readTermsFromFile(Objects.requireNonNull(file));
        return parser.getProtectTermsList(enabled, false);
    }

    public static ProtectedTermsList readProtectedTermsListFromFile(File file, Charset encoding, boolean enabled)
            throws FileNotFoundException {
        LOGGER.debug("Reading term list from file " + file);
        ProtectedTermsParser parser = new ProtectedTermsParser();
        parser.readTermsFromFile(Objects.requireNonNull(file), Objects.requireNonNull(encoding));
        return parser.getProtectTermsList(enabled, false);
    }

    public boolean removeProtectedTermsList(ProtectedTermsList termList) {
        return mainList.remove(termList);
    }
}
