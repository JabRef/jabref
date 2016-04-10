/*  Copyright (C) 2012, 2015 JabRef contributors.
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
package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.model.entry.EntryType;

/**
 * This importer parses text format citations using the online API of FreeCite -
 * Open Source Citation Parser http://freecite.library.brown.edu/
 */
public class FreeCiteImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(FreeCiteImporter.class);

    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {
        // TODO: We don't know how to recognize text files, therefore we return
        // "false"
        return false;
    }

    @Override
    public List<BibEntry> importEntries(InputStream in, OutputPrinter status)
            throws IOException {
        try (Scanner scan = new Scanner(in)) {
            String text = scan.useDelimiter("\\A").next();
            return importEntries(text, status);
        }
    }

    public List<BibEntry> importEntries(String text, OutputPrinter status) {
        // URLencode the string for transmission
        String urlencodedCitation = null;
        try {
            urlencodedCitation = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Unsupported encoding", e);
        }

        // Send the request
        URL url;
        URLConnection conn;
        try {
            url = new URL("http://freecite.library.brown.edu/citations/create");
            conn = url.openConnection();
        } catch (MalformedURLException e) {
            LOGGER.warn("Bad URL", e);
            return Collections.emptyList();
        } catch (IOException e) {
            LOGGER.warn("Could not download", e);
            return Collections.emptyList();
        }
        try {
            conn.setRequestProperty("accept", "text/xml");
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

            String data = "citation=" + urlencodedCitation;
            // write parameters
            writer.write(data);
            writer.flush();
        } catch (IllegalStateException e) {
            LOGGER.warn("Already connected.", e);
        } catch (IOException e) {
            status.showMessage(Localization.lang("Unable to connect to FreeCite online service."));
            LOGGER.warn("Unable to connect to FreeCite online service.", e);
            return Collections.emptyList();
        }
        // output is in conn.getInputStream();
        // new InputStreamReader(conn.getInputStream())

        List<BibEntry> res = new ArrayList<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader parser = factory.createXMLStreamReader(conn.getInputStream());
            while (parser.hasNext()) {
                if ((parser.getEventType() == XMLStreamConstants.START_ELEMENT)
                        && "citation".equals(parser.getLocalName())) {
                    parser.nextTag();

                    StringBuilder noteSB = new StringBuilder();

                    BibEntry e = new BibEntry();
                    // fallback type
                    EntryType type = BibtexEntryTypes.INPROCEEDINGS;

                    while (!((parser.getEventType() == XMLStreamConstants.END_ELEMENT)
                            && "citation".equals(parser.getLocalName()))) {
                        if (parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
                            String ln = parser.getLocalName();
                            if ("authors".equals(ln)) {
                                StringBuilder sb = new StringBuilder();
                                parser.nextTag();

                                while (parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
                                    // author is directly nested below authors
                                    assert "author".equals(parser.getLocalName());

                                    String author = parser.getElementText();
                                    if (sb.length() == 0) {
                                        // first author
                                        sb.append(author);
                                    } else {
                                        sb.append(" and ");
                                        sb.append(author);
                                    }
                                    assert parser.getEventType() == XMLStreamConstants.END_ELEMENT;
                                    assert "author".equals(parser.getLocalName());
                                    parser.nextTag();
                                    // current tag is either begin:author or
                                    // end:authors
                                }
                                e.setField("author", sb.toString());
                            } else if ("journal".equals(ln)) {
                                // we guess that the entry is a journal
                                // the alternative way is to parse
                                // ctx:context-objects / ctx:context-object / ctx:referent / ctx:metadata-by-val / ctx:metadata / journal / rft:genre
                                // the drawback is that ctx:context-objects is NOT nested in citation, but a separate element
                                // we would have to change the whole parser to parse that format.
                                type = BibtexEntryTypes.ARTICLE;
                                e.setField(ln, parser.getElementText());
                            } else if ("tech".equals(ln)) {
                                type = BibtexEntryTypes.TECHREPORT;
                                // the content of the "tech" field seems to contain the number of the technical report
                                e.setField("number", parser.getElementText());
                            } else if ("doi".equals(ln)
                                    || "institution".equals(ln)
                                    || "location".equals(ln)
                                    || "number".equals(ln)
                                    || "note".equals(ln)
                                    || "title".equals(ln)
                                    || "pages".equals(ln)
                                    || "publisher".equals(ln)
                                    || "volume".equals(ln)
                                    || "year".equals(ln)) {
                                e.setField(ln, parser.getElementText());
                            } else if ("booktitle".equals(ln)) {
                                String booktitle = parser.getElementText();
                                if (booktitle.startsWith("In ")) {
                                    // special treatment for parsing of
                                    // "In proceedings of..." references
                                    booktitle = booktitle.substring(3);
                                }
                                e.setField("booktitle", booktitle);
                            } else if ("raw_string".equals(ln)) {
                                // raw input string is ignored
                            } else {
                                // all other tags are stored as note
                                noteSB.append(ln);
                                noteSB.append(':');
                                noteSB.append(parser.getElementText());
                                noteSB.append(Globals.NEWLINE);
                            }
                        }
                        parser.next();
                    }

                    if (noteSB.length() > 0) {
                        String note = e.getField("note");
                        if (note == null) {
                            note = noteSB.toString();
                        } else {
                            // "note" could have been set during the parsing as FreeCite also returns "note"
                            note = note.concat(Globals.NEWLINE).concat(noteSB.toString());
                        }
                        e.setField("note", note);
                    }

                    // type has been derived from "genre"
                    // has to be done before label generation as label generation is dependent on entry type
                    e.setType(type);

                    // autogenerate label (BibTeX key)
                    LabelPatternUtil.makeLabel(JabRef.mainFrame.getCurrentBasePanel().getBibDatabaseContext().getMetaData(), JabRef.mainFrame.getCurrentBasePanel().getDatabase(), e);

                    res.add(e);
                }
                parser.next();
            }
            parser.close();
        } catch (IOException | XMLStreamException ex) {
            LOGGER.warn("Could not parse", ex);
            return Collections.emptyList();
        }

        return res;
    }

    @Override
    public String getFormatName() {
        return "text citations";
    }

}
