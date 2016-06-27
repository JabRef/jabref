package net.sf.jabref.gui.importer.fetcher;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.ACMPortalFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;

public class ACMPortalFetcherGUI extends ACMPortalFetcher implements PreviewEntryFetcherGUI {

    private final JRadioButton guideButton = new JRadioButton(Localization.lang("The Guide to Computing Literature"));
    private final JRadioButton acmButton = new JRadioButton(Localization.lang("The ACM Digital Library"));
    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);


    private String makeUrl() {
        StringBuilder sb = new StringBuilder(getStartUrl()).append(getSearchUrlPart())
                .append(getTerms().replace(" ", "%20")).append(getSearchUrlPartII());

        if (isAcmOrGuide()) {
            sb.append("ACM");
        } else {
            sb.append("GUIDE");
        }
        sb.append(getEndUrl());
        return sb.toString();
    }

    @Override
    public JPanel getOptionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));

        guideButton.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(acmButton);
        group.add(guideButton);

        panel.add(absCheckBox);
        panel.add(acmButton);
        panel.add(guideButton);

        return panel;
    }

    @Override
    public boolean processQueryGetPreview(String query, FetcherPreviewDialog preview, OutputPrinter status) {
        setTerms(query);
        setPiv(0);
        setShouldContinue(true);
        setAcmOrGuide(acmButton.isSelected());
        setFetchAbstract(absCheckBox.isSelected());
        String address = makeUrl();
        LinkedHashMap<String, JLabel> previews = new LinkedHashMap<>();

        try {
            URL url = new URL(address);

            URLDownload dl = new URLDownload(url);

            String page = dl.downloadToString();

            int hits = getNumberOfHits(page, getResultsFoundPattern(), ACMPortalFetcher.getHitsPattern());

            int index = page.indexOf(getResultsFoundPattern());
            if (index >= 0) {
                page = page.substring(index + getResultsFoundPattern().length());
            }

            if (hits == 0) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", getTerms()),
                        Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
                return false;
            } else if (hits > 20) {
                status.showMessage(
                        Localization.lang("%0 entries found. To reduce server load, only %1 will be downloaded.",
                                String.valueOf(hits), String.valueOf(getPerPage())),
                        Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
            }

            hits = getNumberOfHits(page, getPageRangePattern(), getMaxHitsPattern());
            parse(page, Math.min(hits, getPerPage()), previews);
            for (Map.Entry<String, JLabel> entry : previews.entrySet()) {
                preview.addEntry(entry.getKey(), entry.getValue());
            }
            return true;

        } catch (MalformedURLException e) {
            getLogger().warn("Problem with ACM fetcher URL", e);
        } catch (ConnectException e) {
            status.showMessage(Localization.lang("Could not connect to %0", getTitle()),
                    Localization.lang("Search %0", getTitle()), JOptionPane.ERROR_MESSAGE);
            getLogger().warn("Problem with ACM connection", e);
        } catch (IOException e) {
            status.showMessage(e.getMessage(), Localization.lang("Search %0", getTitle()), JOptionPane.ERROR_MESSAGE);
            getLogger().warn("Problem with ACM Portal", e);
        }
        return false;

    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_ACM;
    }

    /**
     * Find out how many hits were found.
     * @param page
     * @param marker
     * @param pattern
     */
    private static int getNumberOfHits(String page, String marker, Pattern pattern) throws IOException {
        int index = page.indexOf(marker);
        if (index >= 0) {
            String substring = page.substring(index, Math.min(index + 100, page.length()));
            Matcher matcher = pattern.matcher(substring);
            if (matcher.find()) {
                try {
                    String number = matcher.group(1);
                    number = number.replace(",", ""); // Remove , as in 1,234
                    return Integer.parseInt(number);
                } catch (NumberFormatException ex) {
                    throw new IOException("Cannot parse number of hits");
                }
            } else {
                getLogger().info("Unmatched! " + substring);
            }
        }
        throw new IOException("Cannot parse number of hits");
    }

    private void parse(String text, int hits, Map<String, JLabel> entries) {
        int entryNumber = 1;
        while (getNextEntryURL(text, entryNumber, entries) && (entryNumber <= hits)) {
            entryNumber++;
        }
    }

    private static String getEntryBibTeXURL(String fullCitation) {
        // Get ID
        Matcher idMatcher = getIdPattern().matcher(fullCitation);
        if (idMatcher.find()) {
            return idMatcher.group(1);
        }
        getLogger().info("Did not find ID in: " + fullCitation);
        return null;
    }

    private boolean getNextEntryURL(String allText, int entryNumber, Map<String, JLabel> entries) {
        int index = allText.indexOf(getNextEntryPattern(), getPiv());
        int endIndex = allText.indexOf(getEndEntryPattern(), index);
        setPiv(endIndex);

        if (index >= 0) {
            String text = allText.substring(index, endIndex);
            // Always try RIS import first
            Matcher fullCitation = getFullCitationPattern().matcher(text);
            String item;
            if (fullCitation.find()) {
                String link = getEntryBibTeXURL(fullCitation.group(1));
                if (endIndex > 0) {
                    StringBuilder sb = new StringBuilder();

                    // Find authors:
                    int authStart = text.indexOf(getAuthorMarker());
                    if (authStart >= 0) {
                        int authEnd = text.indexOf("</div>", authStart + getAuthorMarker().length());
                        if (authEnd >= 0) {
                            sb.append("<p>").append(text.substring(authStart, authEnd)).append("</p>");
                        }

                    }
                    // Find title:
                    Matcher titM = getTitlePattern().matcher(text);
                    if (titM.find()) {
                        sb.append("<p>").append(titM.group(1)).append("</p>");
                    }

                    int sourceStart = text.indexOf(getSourceMarker());
                    if (sourceStart >= 0) {
                        int sourceEnd = text.indexOf("</div>", sourceStart + getSourceMarker().length());
                        if (sourceEnd >= 0) {
                            String sourceText = text.substring(sourceStart, sourceEnd);
                            // Find source:
                            Matcher source = getSourcePattern().matcher(sourceText);
                            if (source.find()) {
                                sb.append("<p>").append(source.group(1)).append("</p>");
                            }
                        }
                    }

                    item = sb.toString();
                } else {
                    item = link;
                }

                JLabel preview = new JLabel("<html>" + item + "</html>");
                preview.setPreferredSize(new Dimension(750, 100));
                entries.put(link, preview);
                return true;
            }
            getLogger().warn("Citation unmatched " + Integer.toString(entryNumber));
            return false;
        }
        return false;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        return false;
    }
}
