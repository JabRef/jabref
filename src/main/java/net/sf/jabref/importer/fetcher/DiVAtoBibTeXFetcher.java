/*  Copyright (C) 2012 JabRef contributors.
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.jabref.importer.fetcher;

import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DiVAtoBibTeXFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(DiVAtoBibTeXFetcher.class);

    private static final String URL_PATTERN = "http://www.diva-portal.org/smash/getreferences?referenceFormat=BibTex&pids=%s";
    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();


    protected static String getUrlPattern() {
        return URL_PATTERN;
    }

    protected ProtectTermsFormatter getProtectTermsFormatter() {
        return protectTermsFormatter;
    }

    protected UnitsToLatexFormatter getUnitsToLatexFormatter() {
        return unitsToLatexFormatter;
    }

    public Log getLogger() {
        return LOGGER;
    }

    @Override
    public void stopFetching() {
        // nothing needed as the fetching is a single HTTP GET
    }

    @Override
    public String getTitle() {
        return "DiVA";
    }

}
