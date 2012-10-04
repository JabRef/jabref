/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.labelPattern.LabelPatternUtil;

/**
 * This importer parses text format citations using the online API of FreeCite -
 * Open Source Citation Parser http://freecite.library.brown.edu/
 */
public class FreeCiteImporter extends ImportFormat {

    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {
        // TODO: We don't know how to recognize text files, therefore we return
        // "false"
        return false;
    }

    @Override
    public List<BibtexEntry> importEntries(InputStream in, OutputPrinter status)
            throws IOException {
        String text = new Scanner(in).useDelimiter("\\A").next();
        return importEntries(text, status);
    }

    public List<BibtexEntry> importEntries(String text, OutputPrinter status) {
        // URLencode the string for transmission
        String urlencodedCitation = null;
        try {
            urlencodedCitation = URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // e.printStackTrace();
        }
        String data = "citation=" + urlencodedCitation;

        // Send the request
        URL url;
        URLConnection conn;
        try {
            url = new URL("http://freecite.library.brown.edu/citations/create");
            conn = url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        try {
            conn.setRequestProperty("accept", "text/xml");
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

            // write parameters
            writer.write(data);
            writer.flush();
        } catch (IOException e) {
            status.showMessage(Globals.lang("Unable to connect to freecite online service."));
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        // output is in conn.getInputStream();
        // new InputStreamReader(conn.getInputStream())

        List<BibtexEntry> res = new ArrayList<BibtexEntry>();
        
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader parser = factory.createXMLStreamReader(conn.getInputStream());
            while (parser.hasNext()) {
                if ((parser.getEventType() == XMLStreamConstants.START_ELEMENT)
                        && (parser.getLocalName().equals("citation"))) {
                    parser.nextTag();
                    
                    StringBuilder noteSB = new StringBuilder();

                    BibtexEntry e = new BibtexEntry();
                    // fallback type
                    BibtexEntryType type = BibtexEntryType.INPROCEEDINGS;

                    while (! (   parser.getEventType() == XMLStreamConstants.END_ELEMENT
                              && parser.getLocalName().equals("citation"))) {
                        if (parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
                            String ln = parser.getLocalName();
                            if (ln.equals("authors")) {
                                StringBuilder sb = new StringBuilder();
                                parser.nextTag();

                                while (parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
                                    // author is directly nested below authors
                                    assert (parser.getLocalName()
                                            .equals("author"));

                                    String author = parser.getElementText();
                                    if (sb.length() == 0) {
                                        // first author
                                        sb.append(author);
                                    } else {
                                        sb.append(" and ");
                                        sb.append(author);
                                    }
                                    assert(parser.getEventType() == XMLStreamConstants.END_ELEMENT);
                                    assert(parser.getLocalName().equals("author"));
                                    parser.nextTag();
                                    // current tag is either begin:author or
                                    // end:authors
                                }
                                e.setField("author", sb.toString());
                            } else if (ln.equals("journal")) {
                                // we guess that the entry is a journal
                                // the alternative way is to parse
                                // ctx:context-objects / ctx:context-object / ctx:referent / ctx:metadata-by-val / ctx:metadata / journal / rft:genre
                                // the drawback is that ctx:context-objects is NOT nested in citation, but a separate element
                                // we would have to change the whole parser to parse that format.
                                type = BibtexEntryType.ARTICLE;
                                e.setField(ln, parser.getElementText());
                            } else if (ln.equals("tech")) {
                                type = BibtexEntryType.TECHREPORT;
                                // the content of the "tech" field seems to contain the number of the technical report
                                e.setField("number", parser.getElementText());
                            } else if ( ln.equals("doi")
                                     || ln.equals("institution")
                                     || ln.equals("location")
                                     || ln.equals("number")
                                     || ln.equals("note") 
                                     || ln.equals("title") 
                                     || ln.equals("pages")
                                     || ln.equals("publisher")
                                     || ln.equals("volume")
                                     || ln.equals("year")) {
                                e.setField(ln, parser.getElementText());
                            } else if (ln.equals("booktitle")) {
                                String booktitle = parser.getElementText();
                                if (booktitle.startsWith("In ")) {
                                    // special treatment for parsing of
                                    // "In proceedings of..." references
                                    booktitle = booktitle.substring(3);
                                }
                                e.setField("booktitle", booktitle);
                            } else if (ln.equals("raw_string")) {
                                // raw input string is ignored
                            } else {
                                // all other tags are stored as note
                                noteSB.append(ln);
                                noteSB.append(":");
                                noteSB.append(parser.getElementText());
                                noteSB.append(Globals.NEWLINE);
                            }
                        }
                        parser.next();
                    }
                    
                    if (noteSB.length() > 0) {
                        String note = e.getField("note");
                        if (note != null) {
                            // "note" could have been set during the parsing as FreeCite also returns "note" 
                            note = note.concat(Globals.NEWLINE).concat(noteSB.toString());
                        } else {
                            note = noteSB.toString();
                        }
                        e.setField("note", note);
                    }
                    
                    // type has been derived from "genre"
                    // has to be done before label generation as label generation is dependent on entry type
                    e.setType(type);

                    // autogenerate label (BibTeX key)
                    e = LabelPatternUtil.makeLabel(JabRef.jrf.basePanel().metaData(), JabRef.jrf.basePanel().database(), e);
                    
                    res.add(e);
                }
                parser.next();
            }
            parser.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return res;
    }

    @Override
    public String getFormatName() {
        return "text citations";
    }

}
