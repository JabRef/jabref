package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.BracketedPattern;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Contains utility constants and methods for processing of CSL citations as generated by methods of <a href="https://github.com/michel-kraemer/citeproc-java">citeproc-java</a> ({@link org.jabref.logic.citationstyle.CitationStyleGenerator}).
 * <p>These methods are used in {@link CSLCitationOOAdapter} which inserts CSL citation text into an OO document.</p>
 */
public class CSLFormatUtils {

    // TODO: These are static final fields right now, should add the functionality to let user select these and store them in preferences.
    public static final String DEFAULT_BIBLIOGRAPHY_TITLE = "References";
    public static final String DEFAULT_BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT = "Heading 2";

    public static final String DEFAULT_BIBLIOGRAPHY_BODY_PARAGRAPH_FORMAT = "Body Text";

    public static final CitationStyleOutputFormat OUTPUT_FORMAT = CitationStyleOutputFormat.HTML;
    private static final Pattern YEAR_IN_CITATION_PATTERN = Pattern.compile("(.)(.*), (\\d{4}.*)");

    /**
     * Transforms provided HTML into a format that can be fully parsed and inserted into an OO document.
     * Context: The HTML produced by {@link org.jabref.logic.citationstyle.CitationStyleGenerator#generateBibliography(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateBibliography} or {@link org.jabref.logic.citationstyle.CitationStyleGenerator#generateCitation(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateCitation} is not directly (completely) parsable by {@link OOTextIntoOO#write(XTextDocument, XTextCursor, OOText) write}.
     * For more details, read the documentation for the {@link OOTextIntoOO} class.
     * <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">Additional Information</a>.
     *
     * @param html The HTML string to be transformed into OO-write ready HTML.
     * @return The formatted html string.
     */
    public static String transformHTML(String html) {
        // Initial clean up of escaped characters
        html = StringEscapeUtils.unescapeHtml4(html);

        // Handle margins (spaces between citation number and text)
        html = html.replaceAll("<div class=\"csl-left-margin\">(.*?)</div><div class=\"csl-right-inline\">(.*?)</div>", "$1 $2");

        // Remove unsupported tags
        html = html.replaceAll("<div[^>]*>", "");
        html = html.replace("</div>", "");

        // Remove unsupported links
        html = html.replaceAll("<a[^>]*>", "");
        html = html.replace("</a>", "");

        // Replace span tags with inline styles for bold
        html = html.replaceAll("<span style=\"font-weight: ?bold;?\">(.*?)</span>", "<b>$1</b>");

        // Replace span tags with inline styles for italic
        html = html.replaceAll("<span style=\"font-style: ?italic;?\">(.*?)</span>", "<i>$1</i>");

        // Replace span tags with inline styles for underline
        html = html.replaceAll("<span style=\"text-decoration: ?underline;?\">(.*?)</span>", "<u>$1</u>");

        html = html.replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Clean up any remaining span tags
        html = html.replaceAll("</?span[^>]*>", "");

        // Convert line breaks to paragraph breaks
        html = html.replaceAll("[\n\r]+", "<p></p>");

        // Remove leading paragraph tags (preserving any whitespaces after them for indentation)
        html = html.replaceAll("^\\s*<p>\\s*</p>", "");

        // Remove extra trailing paragraph tags when there are multiple (keeping one)
        html = html.replaceAll("(?:<p>\\s*</p>\\s*){2,}$", "<p></p>");

        return html;
    }

    /**
     * Alphanumeric citations are not natively supported by citeproc-java (see {@link org.jabref.logic.citationstyle.CitationStyleGenerator#generateCitation(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateCitation}).
     * Thus, we manually format a citation to produce its alphanumeric form.
     *
     * @param entries the list of entries for which the alphanumeric citation is to be generated.
     * @return the alphanumeric citation (for a single entry or a group of entries).
     */
    public static String generateAlphanumericCitation(List<BibEntry> entries, BibDatabaseContext bibDatabaseContext) {
        StringBuilder citation = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            BibEntry entry = entries.get(i);
            Optional<String> author = entry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase());
            Optional<String> year = entry.getResolvedFieldOrAlias(StandardField.YEAR, bibDatabaseContext.getDatabase());

            if (author.isPresent() && year.isPresent()) {
                AuthorList authorList = AuthorList.parse(author.get());
                String alphaKey = BracketedPattern.authorsAlphaV2(authorList);

                // Extract last two digits of the year
                String shortYear = year.get().length() >= 2 ?
                        year.get().substring(year.get().length() - 2) :
                        year.get();

                citation.append(alphaKey).append(shortYear);
            } else {
                citation.append(entry.getCitationKey().orElse(""));
            }

            if (i < entries.size() - 1) {
                citation.append("; ");
            }
        }
        citation.append("]");
        return citation.toString();
    }

    public static String generateAlphanumericInTextCitation(BibEntry entry, BibDatabaseContext bibDatabaseContext) {
        String inTextCitation = generateAlphanumericCitation(List.of(entry), bibDatabaseContext);

        String authorName = entry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase())
                                        .map(AuthorList::parse)
                                        .map(list -> BracketedPattern.joinAuthorsOnLastName(list, 1, "", " et al."))
                                        .orElse("");

        return authorName + " " + inTextCitation;
    }

    /**
     * Method to update citation number of a bibliographic entry (to be inserted in the list of references).
     * By default, citeproc-java ({@link org.jabref.logic.citationstyle.CitationStyleGenerator#generateBibliography(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateBibliography}) always starts the numbering of a list of references with "1".
     * If a citation doesn't correspond to the first cited entry, the number should be changed to the appropriate current citation number.
     * The numbers should be globally unique. If an entry has been cited before, the older citation number corresponding to it should be reused.
     * The number can be enclosed in different formats, such as "1", "1.", "1)", "(1)" or "[1]".
     * <p>
     * <b>Precondition:</b> Use ONLY with numeric citation styles.</p>
     *
     * @param citation the numeric citation with an unresolved number.
     * @param currentNumber the correct number to update the citation with.
     * @return the bibliographic citation with resolved number.
     */
    public static String updateSingleBibliographyNumber(String citation, int currentNumber) {
        Pattern pattern = Pattern.compile("(\\[|\\()?(\\d+)(\\]|\\))?(\\.)?\\s*");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        boolean numberReplaced = false;

        while (matcher.find()) {
            if (!numberReplaced) {
                String prefix = matcher.group(1) != null ? matcher.group(1) : "";
                String suffix = matcher.group(3) != null ? matcher.group(3) : "";
                String dot = matcher.group(4) != null ? "." : "";
                String space = matcher.group().endsWith(" ") ? " " : "";

                String replacement = prefix + currentNumber + suffix + dot + space;

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                numberReplaced = true;
            } else {
                matcher.appendReplacement(sb, matcher.group());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Extracts year from a citation having single or multiple entries, for the purpose of using in in-text citations.
     *
     * @param formattedCitation the citation cleaned up and formatted using {@link CSLFormatUtils#transformHTML transformHTML}.
     */
    public static String changeToInText(String formattedCitation) {
        Matcher matcher = YEAR_IN_CITATION_PATTERN.matcher(formattedCitation);
        if (matcher.find()) {
            return matcher.group(2) + " " + matcher.group(1) + matcher.group(3);
        }
        return formattedCitation;
    }

    /**
     * Generates Author Prefix for an in-text citation
     */
    public static String generateAuthorPrefix(BibEntry currentEntry, BibDatabaseContext bibDatabaseContext) {
        return currentEntry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase())
                           .map(AuthorList::parse)
                           .map(list -> BracketedPattern.joinAuthorsOnLastName(list, 1, "", " et al.") + " ")
                           .orElse("");
    }
}
