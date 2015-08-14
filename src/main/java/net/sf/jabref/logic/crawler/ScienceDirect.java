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
package net.sf.jabref.logic.crawler;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.external.FindFullText;
import net.sf.jabref.external.FullTextFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FullTextFinder implementation that attempts to find a PDF URL from a ScienceDirect article page.
 */
public class ScienceDirect implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(ScienceDirect.class);

    private static final String URL_EXP = "sciencedirect.com";

    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        // TODO: http://dev.elsevier.com/
        // https://github.com/Mashape/unirest-java
        return Optional.empty();
    }
}
