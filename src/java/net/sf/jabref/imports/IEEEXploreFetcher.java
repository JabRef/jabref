package net.sf.jabref.imports;

import java.awt.BorderLayout;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.*;
import net.sf.jabref.gui.ImportInspectionDialog;

public class IEEEXploreFetcher implements Runnable, EntryFetcher {

    ImportInspectionDialog dialog = null;
    JabRefFrame frame = null;
    HTMLConverter htmlConverter = new HTMLConverter();
    private String terms;
    String startUrl = "http://ieeexplore.ieee.org";
    String searchUrlPart = "/search/freesearchresult.jsp?queryText=";
    String endUrl = "+%3Cin%3E+metadata&ResultCount=25&ResultStart=";
    String risUrl = "http://ieeexplore.ieee.org/xpls/citationAct";
    private int perPage = 25, hits = 0, unparseable = 0, parsed = 0;
    private boolean shouldContinue = false;
    private JCheckBox fetchAstracts = new JCheckBox(Globals.lang("Include abstracts"), false);
    private boolean fetchingAbstracts = false;
    private static final int MAX_ABSTRACT_FETCH = 5;

    public IEEEXploreFetcher() {
    }


    //Pattern hitsPattern = Pattern.compile("Your search matched <strong>(\\d+)</strong>");
    Pattern hitsPattern = Pattern.compile(".*Your search matched <strong>(\\d+)</strong>.*");
    Pattern maxHitsPattern = Pattern.compile(".*A maximum of <strong>(\\d+)</strong>.*");
    Pattern entryPattern1 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                +"\\s+<A href='(.+)'>(.+)</A><br>\\s+Volume (.+),&nbsp;\\s*"
                +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");

    Pattern entryPattern2 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                    +"\\s+<A href='(.+)'>(.+)</A><br>\\s+Volume (.+),&nbsp;\\s+Issue (\\d+),&nbsp;\\s*"
                    +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");


    Pattern entryPattern3 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                    +"\\s+<A href='(.+)'>(.+)</A><br>\\s+Volume (.+),&nbsp;\\s+Issue (\\d+),&nbsp;" +
                    "\\s+Part (\\d+),&nbsp;\\s*" //"[\\s-\\d]+"
                    +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");

    Pattern entryPattern4 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                    +"\\s+<A href='(.+)'>(.+)</A><br>\\s*" //[\\s-\\da-z]+"
                    +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");

    Pattern abstractLinkPattern = Pattern.compile(
            "<a href=\"(.+)\" class=\"bodyCopySpaced\">Abstract</a>");

    Pattern ieeeArticleNumberPattern =
        Pattern.compile("<a href=\".*arnumber=(\\d+).*\">");

    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.add(fetchAstracts, BorderLayout.CENTER);
        return pan;
    }

    public void processQuery(String query, ImportInspectionDialog dialog, JabRefFrame frame) {
        this.dialog = dialog;
        this.frame =frame;
        this.terms = query;
        piv = 0;
        (new Thread(this)).start();
    }



    public String getTitle() {
        return Globals.menuTitle("Search IEEEXplore");
    }


    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getHelpPage() {
        return "IEEEXploreHelp.html";
    }

    public String getKeyName() {
        return "Search IEEXplore";
    }

    // This method is called by the dialog when the user has cancelled the import.
    public void cancelled() {
        shouldContinue = false;
    }

    // This method is called by the dialog when the user has selected the
// wanted entries, and clicked Ok. The callback object can update status
// line etc.
    public void done(int entriesImported) {
        //System.out.println("Number of entries parsed: "+parsed);
        //System.out.println("Parsing failed for "+unparseable+" entries");
    }

    // This method is called by the dialog when the user has cancelled or
// signalled a stop. It is expected that any long-running fetch operations
// will stop after this method is called.
    public void stopFetching() {
        shouldContinue = false;
    }

    /**
     * The code that runs the actual search and fetch operation.
     */
    public void run() {
        frame.block();
        shouldContinue = true;
        parsed = 0;
        unparseable = 0;
        String address = makeUrl(0);
        try {
            URL url = new URL(address);
            // Fetch the search page and put the contents in a String:
            //String page = getResultsFromFile(new File("/home/alver/div/temp.txt"));
            //URLDownload ud = new URLDownload(new JPanel(), url, new File("/home/alver/div/temp.txt"));
            //ud.download();

            //dialog.setVisible(true);
            String page = getResults(url);
            hits = getNumberOfHits(page, "Your search matched", hitsPattern);

            frame.unblock();
            if (hits == 0) {
                dialog.dispose();
                JOptionPane.showMessageDialog(frame, Globals.lang("No entries found for the search string '%0'",
                        terms),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                fetchingAbstracts = fetchAstracts.isSelected();
                if (fetchingAbstracts && (hits > MAX_ABSTRACT_FETCH)) {
                    fetchingAbstracts = false;
                    JOptionPane.showMessageDialog(frame,
                            Globals.lang("%0 entries found. To reduce server load, abstracts "
                            +"will only be downloaded for searches returning %1 hits or less.",
                                    new String[] {String.valueOf(hits), String.valueOf(MAX_ABSTRACT_FETCH)}),
                            Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                }
                dialog.setVisible(true);
            }

            int maxHits = getNumberOfHits(page, "A maximum of", maxHitsPattern);
            //String page = getResultsFromFile(new File("/home/alver/div/temp50.txt"));

            //List entries = new ArrayList();
            //System.out.println("Number of hits: "+hits);
            //System.out.println("Maximum returned: "+maxHits);
            if (hits > maxHits)
                hits = maxHits;
            //parse(dialog, page, 0, 51);
            //dialog.setProgress(perPage/2, hits);
            parse(dialog, page, 0, 1);
            int firstEntry = perPage;
            while (shouldContinue && (firstEntry < hits)) {
                //System.out.println("Fetching from: "+firstEntry);
                address = makeUrl(firstEntry);
                //System.out.println(address);
                page = getResults(new URL(address));
                //dialog.setProgress(firstEntry+perPage/2, hits);
                if (!shouldContinue)
                    break;

                parse(dialog, page, 0, 1+firstEntry);
                firstEntry += perPage;

            }
            dialog.entryListComplete();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Connection to IEEEXplore failed"),
                    Globals.lang("Search IEEExplore"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            frame.unblock(); // We call this to ensure no lockup.
        }


    }

    private String makeUrl(int startIndex) {
        StringBuffer sb = new StringBuffer(startUrl).append(searchUrlPart);
        sb.append(terms.replaceAll(" ", "+"));
        sb.append(endUrl);
        sb.append(String.valueOf(startIndex));
        return sb.toString();
    }

    int piv = 0;

    private void parse(ImportInspectionDialog dialog, String text, int startIndex, int firstEntryNumber) {
        piv = startIndex;
        int entryNumber = firstEntryNumber;
        List<BibtexEntry> entries = new ArrayList<BibtexEntry>();
        BibtexEntry entry;
        while (((entry = parseNextEntry(text, piv, entryNumber)) != null)
            && (shouldContinue)) {
            if (entry.getField("title") != null) {
                entries.add(entry);
                dialog.addEntries(entries);
                dialog.setProgress(parsed+unparseable, hits);
                entries.clear();
                parsed++;
            }
            entryNumber++;
            //break;
        }


    }

    private BibtexEntry parseEntryRis(String number)
        throws IOException
    {
        URL url;
        URLConnection conn;
        try {
            url = new URL(risUrl);
            conn = url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        PrintWriter out = new PrintWriter(
                conn.getOutputStream());
        out.write(
                "fileFormate=ris&arnumber="+
                URLEncoder.encode(
                    "<arnumber>"+number+"</arnumber>",
                    "UTF-8"));
        out.flush();
        out.close();
        InputStream inp = conn.getInputStream();
        List<BibtexEntry> items = new RisImporter().importEntries(inp);
        inp.close();
        if (items.size() > 0)
            return items.get(0);
        else
            return null;
    }

    private BibtexEntry parseNextEntry(String allText, int startIndex, int entryNumber)
    {
        BibtexEntry entry = null;
        String toFind = new StringBuffer().append("<div align=\"left\"><strong>")
                .append(entryNumber).append(".</strong></div>").toString();
        int index = allText.indexOf(toFind, startIndex);
        int endIndex = allText.indexOf("</table>", index+1);
        if (endIndex < 0)
            endIndex = allText.length();

        if (index >= 0) {
            piv = index+1;
            String text = allText.substring(index, endIndex);
            // Fetching abstracts takes time, and causes a lot
            // of requests, so this should be optional or disabled:
            if (fetchingAbstracts) {
                Matcher number =
                    ieeeArticleNumberPattern.matcher(text);
                if (number.find()) {
                    try {
                        entry = parseEntryRis(number.group(1));
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
            if (entry != null) { // fetch successful
                // we just need to add DOI, it is not included in RIS.
                int pgInd = text.indexOf("Digital Object Identifier ");
                if (pgInd >= 0) {
                    int fieldEnd = text.indexOf("<br>", pgInd);
                    if (fieldEnd >= 0) {
                        entry.setField("doi",
                            text.substring(pgInd+26, fieldEnd).trim());
                    }
                }
                return entry;
            }
            BibtexEntryType type;
            String sourceField;
            if (text.indexOf("IEEE JNL") >= 0) {
                type = BibtexEntryType.getType("article");
                sourceField = "journal";
            } else {
                type = BibtexEntryType.getType("inproceedings");
                sourceField = "booktitle";
            }

            index = 0;
            entry = new BibtexEntry(Util.createNeutralId(), type);
            //System.out.println(text);
            Matcher m1 = entryPattern1.matcher(text);
            Matcher m2 = entryPattern2.matcher(text);
            Matcher m3 = entryPattern3.matcher(text);
            Matcher m4 = entryPattern4.matcher(text);
            Matcher m;
            String tmp;
            String rest = "";
            if (m1.find()) {
                m = m1;
                // Title:
                entry.setField("title", convertHTMLChars(m.group(1)));
                // Author:
                tmp = convertHTMLChars(m.group(2));
                if (tmp.charAt(tmp.length()-1) == ';')
                    tmp= tmp.substring(0, tmp.length()-1);
                entry.setField("author", tmp.replaceAll("; ", " and "));
                // Publication:
                tmp = m.group(4);
                entry.setField(sourceField, convertHTMLChars(tmp));
                // Volume:
                entry.setField("volume", convertHTMLChars(m.group(5)));
                // Month:
                entry.setField("month", convertHTMLChars(m.group(6)));
                // Year
                entry.setField("year", m.group(7));

            }
            else if (m2.find()) {
                m = m2;
                // Title:
                entry.setField("title", convertHTMLChars(m.group(1)));
                // Author:
                tmp = convertHTMLChars(m.group(2));
                if (tmp.charAt(tmp.length()-1) == ';')
                    tmp= tmp.substring(0, tmp.length()-1);
                entry.setField("author", tmp.replaceAll("; ", " and "));
                // Publication:
                tmp = m.group(4);
                entry.setField(sourceField, convertHTMLChars(tmp));
                // Volume:
                entry.setField("volume", convertHTMLChars(m.group(5)));
                // Number:
                entry.setField("number", convertHTMLChars(m.group(6)));
                // Month:
                entry.setField("month", convertHTMLChars(m.group(7)));
                // Year:
                entry.setField("year", m.group(8));

            }
            else if (m3.find()) {
                m = m3;
                // Title:
                entry.setField("title", convertHTMLChars(m.group(1)));
                // Author:
                tmp = convertHTMLChars(m.group(2));
                if (tmp.charAt(tmp.length()-1) == ';')
                    tmp= tmp.substring(0, tmp.length()-1);
                entry.setField("author", tmp.replaceAll("; ", " and "));
                // Publication:
                tmp = m.group(4);
                entry.setField(sourceField, convertHTMLChars(tmp));
                // Volume:
                entry.setField("volume", convertHTMLChars(m.group(5)));
                // Number:
                entry.setField("number", convertHTMLChars(m.group(6)));
                // Month:
                entry.setField("month", convertHTMLChars(m.group(8)));
                // Year
                entry.setField("year", m.group(9));

            }
            else if (m4.find()) {
                m = m4;
                // Title:
                entry.setField("title", convertHTMLChars(m.group(1)));
                // Author:
                tmp = convertHTMLChars(m.group(2));
                if (tmp.charAt(tmp.length()-1) == ';')
                    tmp= tmp.substring(0, tmp.length()-1);
                entry.setField("author", tmp.replaceAll("; ", " and "));
                // Publication:
                tmp = m.group(4);
                entry.setField(sourceField, convertHTMLChars(tmp));
                // Month:
                entry.setField("month", convertHTMLChars(m.group(5)));
                // Year
                entry.setField("year", m.group(6));

            } else {
                System.err.println("---no structure match---");
                System.err.println(text);
                unparseable++;
            }
            int pgInd = text.indexOf("Page(s):");
            if (pgInd >= 0) {
                // Try to set pages:
                rest = text.substring(pgInd+8);
                pgInd = rest.indexOf("<br>");
                if (pgInd >= 0) {
                    tmp = rest.substring(0, pgInd);
                    entry.setField("pages", tmp.replaceAll("\\s+", "").replaceAll("-","--"));
                }
                // Try to set doi:
                pgInd = rest.indexOf("Digital Object Identifier ", pgInd);
                if (pgInd >= 0) {
                    int fieldEnd = rest.indexOf("<br>", pgInd);
                    if (fieldEnd >= 0) {
                        entry.setField("doi", rest.substring(pgInd+26, fieldEnd).trim());
                    }
                }
            }


            return entry;
        }
        return null;
    }

    /**
     * This method must convert HTML style char sequences to normal characters.
     * @param text The text to handle.
     * @return The converted text.
     */
    private String convertHTMLChars(String text) {

        return htmlConverter.format(text);
    }


    /**
     * Find out how many hits were found.
     * @param page
     */
    private int getNumberOfHits(String page, String marker, Pattern pattern) throws IOException {
        int ind = page.indexOf(marker);
        if (ind < 0)
            throw new IOException(Globals.lang("Could not parse number of hits"));
        String substring = page.substring(ind, Math.min(ind+42, page.length()));
        Matcher m = pattern.matcher(substring);
        if (!m.find())
            return 0;
        if (m.groupCount() >= 1) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
                throw new IOException(Globals.lang("Could not parse number of hits"));
            }
        }
        throw new IOException(Globals.lang("Could not parse number of hits"));
    }

    /**
     * Download the URL and return contents as a String.
     * @param source
     * @return
     * @throws IOException
     */
    public String getResults(URL source) throws IOException {
        
        InputStream in = source.openStream();
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = in.read(buffer);
            if(bytesRead == -1) break;
            for (int i=0; i<bytesRead; i++)
                sb.append((char)buffer[i]);
        }
        return sb.toString();
    }

    /**
     * Read results from a file instead of an URL. Just for faster debugging.
     * @param f
     * @return
     * @throws IOException
     */
    public String getResultsFromFile(File f) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = in.read(buffer);
            if(bytesRead == -1) break;
            for (int i=0; i<bytesRead; i++)
                sb.append((char)buffer[i]);
        }
        return sb.toString();
    }


    /**
     * Download and parse the web page containing an entry's Abstract:
     * @param link
     * @return
     * @throws IOException
     */
    public String fetchAbstract(String link) throws IOException {
        URL url = new URL(link);
        String page = getResults(url);
        //System.out.println(link);

        //System.out.println("Fetched abstract page.");

        String marker = "Abstract</span><br>";
        int index = page.indexOf(marker);
        int endIndex = page.indexOf("</td>", index + 1);
        if ((index >= 0) && (endIndex > index)) {
            return new String(page.substring(index + marker.length(), endIndex).trim());
        }

        return null;
    }

}
