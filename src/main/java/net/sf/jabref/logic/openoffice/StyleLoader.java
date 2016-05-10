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
package net.sf.jabref.logic.openoffice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StyleLoader {

    private static final Log LOGGER = LogFactory.getLog(StyleLoader.class);

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH = "/resource/openoffice/default_authoryear.jstyle";
    public static final String DEFAULT_NUMERICAL_STYLE_PATH = "/resource/openoffice/default_numerical.jstyle";

    // All internal styles
    private final List<String> internalStyleFiles = Arrays.asList(DEFAULT_AUTHORYEAR_STYLE_PATH,
            DEFAULT_NUMERICAL_STYLE_PATH);

    private final JournalAbbreviationRepository repository;
    private final OpenOfficePreferences preferences;
    private final Charset encoding;

    // Lists of the internal
    // and external styles
    private final List<OOBibStyle> internalStyles = new ArrayList<>();
    private final List<OOBibStyle> externalStyles = new ArrayList<>();


    public StyleLoader(OpenOfficePreferences preferences, JournalAbbreviationRepository repository, Charset encoding) {
        this.repository = Objects.requireNonNull(repository);
        this.preferences = Objects.requireNonNull(preferences);
        this.encoding = Objects.requireNonNull(encoding);
        loadInternalStyles();
        loadExternalStyles();
    }

    public List<OOBibStyle> getStyles() {
        List<OOBibStyle> result = new ArrayList<>(internalStyles);
        result.addAll(externalStyles);
        return result;
    }

    public void addStyle(String filename) {
        Objects.requireNonNull(filename);
        try {
            OOBibStyle newStyle = new OOBibStyle(new File(filename), repository, encoding);
            if (externalStyles.contains(newStyle)) {
                LOGGER.info("External style file " + filename + " already existing.");
            } else if (newStyle.isValid()) {
                externalStyles.add(newStyle);
                storeExternalStyles();
            } else {
                LOGGER.error(String.format("Style with filename %s is invalid", filename));
            }
        } catch (FileNotFoundException e) {
            // The file couldn't be found... should we tell anyone?
            LOGGER.info("Cannot find external style file " + filename, e);
        } catch (IOException e) {
            LOGGER.info("Problem reading external style file " + filename, e);
        }

    }

    private void loadExternalStyles() {
        externalStyles.clear();
        // Read external lists
        List<String> lists = preferences.getExternalStyles();
        for (String filename : lists) {
            try {
                OOBibStyle style = new OOBibStyle(new File(filename), repository, encoding);
                if (style.isValid()) {
                    externalStyles.add(style);
                } else {
                    LOGGER.error(String.format("Style with filename %s is invalid", filename));
                }
            } catch (FileNotFoundException e) {
                // The file couldn't be found... should we tell anyone?
                LOGGER.info("Cannot find external style file " + filename, e);
            } catch (IOException e) {
                LOGGER.info("Problem reading external style file " + filename, e);
            }
        }
    }

    private void loadInternalStyles() {
        internalStyles.clear();
        for (String filename : internalStyleFiles) {
            try {
                internalStyles.add(new OOBibStyle(filename, repository));
            } catch (IOException e) {
                LOGGER.info("Problem reading internal style file " + filename, e);
            }
        }
    }

    private void storeExternalStyles() {
        List<String> filenames = new ArrayList<>(externalStyles.size());
        for (OOBibStyle style : externalStyles) {
            filenames.add(style.getPath());
        }
        preferences.setExternalStyles(filenames);
    }

    public boolean removeStyle(OOBibStyle style) {
        Objects.requireNonNull(style);
        if (!style.isFromResource()) {
            boolean result = externalStyles.remove(style);
            storeExternalStyles();
            return result;
        }
        return false;
    }

    public OOBibStyle getUsedStyle() {
        String filename = preferences.getCurrentStyle();
        if (filename != null) {
            for (OOBibStyle style : getStyles()) {
                if (filename.equals(style.getPath())) {
                    return style;
                }
            }
        }
        // Pick the first internal
        preferences.setCurrentStyle(internalStyles.get(0).getPath());
        return internalStyles.get(0);
    }
}
