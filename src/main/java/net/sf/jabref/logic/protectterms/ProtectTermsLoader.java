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
package net.sf.jabref.logic.protectterms;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProtectTermsLoader {

    private static final Log LOGGER = LogFactory.getLog(ProtectTermsLoader.class);

    private final List<ProtectTermsList> mainList = new ArrayList<>();


    public ProtectTermsLoader(List<String> externalTermLists) {
        update(externalTermLists);
    }

    public void update(List<String> externalTermLists) {
        // Read builtin list
        mainList.addAll(ProtectTermsLists.getAllLists());

        // Read external lists
        if (!(externalTermLists.isEmpty())) {
            for (String filename : externalTermLists) {
                try {
                    mainList.add(readTermsFromFile(new File(filename)));
                } catch (FileNotFoundException e) {
                    // The file couldn't be found... should we tell anyone?
                    LOGGER.info("Cannot find term list file " + filename, e);
                }
            }
        }
    }

    public List<ProtectTermsList> getTermsLists() {
        return mainList;
    }

    public void addFromFile(String filename) {
        try {
            mainList.add(readTermsFromFile(new File(filename)));
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            LOGGER.info("Cannot find term list file " + filename, e);
        }
    }

    public static ProtectTermsList readTermsFromFile(File file) throws FileNotFoundException {
        LOGGER.debug("Reading term list from file " + file);
        ProtectTermsParser parser = new ProtectTermsParser();
        parser.readTermsFromFile(Objects.requireNonNull(file));
        return parser.getProtectTermsList();
    }

    public static ProtectTermsList readTermsFromFile(File file, Charset encoding) throws FileNotFoundException {
        LOGGER.debug("Reading term list from file " + file);
        ProtectTermsParser parser = new ProtectTermsParser();
        parser.readTermsFromFile(Objects.requireNonNull(file), Objects.requireNonNull(encoding));
        return parser.getProtectTermsList();
    }

    public boolean removeStyle(ProtectTermsList termList) {
        return mainList.remove(termList);
    }
}
