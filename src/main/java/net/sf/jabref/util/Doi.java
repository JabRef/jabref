package net.sf.jabref.util;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Doi {
    private static final Log LOGGER = LogFactory.getLog(Doi.class);

    // Doi resolver URL
    public static final String RESOLVER = "http://doi.org/";
    // Doi
    private String doi;
    // Doi-regexp provided by http://stackoverflow.com/a/10324802/873282
    // Some Doi's are not caught by the regexp in the above link, i.e. 10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S
    // Removed <> from non-permitted characters
    // See http://www.doi.org/doi_handbook/2_Numbering.html#2.6
    // Regex
    private static final String REGEX_DOI = "\\b(10[.][0-9]{3,}(?:[.][0-9]+)*/(?:(?![\"&\\'])\\S)+)\\b";
    private static final String REGEX_HTTP_DOI = "http[s]?://[^\\s]*?" + REGEX_DOI;
    // Pattern
    private static final Pattern PLAIN_DOI = Pattern.compile(REGEX_DOI, Pattern.CASE_INSENSITIVE);

    /**
     * Creates a Doi from various schemes including URL or plain DOIs.
     *
     * @param doi the Doi string
     * @throws NullPointerException if doi is null
     * @throws IllegalArgumentException if doi does not include a valid Doi
     */
    public Doi(String doi) {
        Objects.requireNonNull(doi);
        // URL decoding
        if(containsHttpDoi(doi)) {
            try {
                doi = URLDecoder.decode(doi, "UTF-8");
            } catch(UnsupportedEncodingException e) {
                LOGGER.error("Unsupported encoding: " + e);
            }
        }

        Matcher matcher = PLAIN_DOI.matcher(doi);
        if (matcher.find()) {
            this.doi = matcher.group();
        } else {
            throw new IllegalArgumentException(doi + " is not a valid Doi.");
        }
    }

    /**
     * Check if the String matches a plain Doi
     *
     * @param value the String to check
     * @return true if value contains a Doi
     */
    public static boolean containsDoi(String value) {
        return value != null && value.matches(".*" + REGEX_DOI + ".*");
    }

    /**
     * Check if the String matches a URI presentation of a Doi
     *
     * <example>
     *     The Doi name "10.1006/jmbi.1998.2354" would be made an actionable link as "http://doi.org/10.1006/jmbi.1998.2354".
     * </example>
     *
     * @param value the String to check
     * @return true if value contains a URI presentation of a Doi
     */
    public static boolean containsHttpDoi(String value) {
        return value != null && value.matches(".*" + REGEX_HTTP_DOI + ".*");
    }

    /**
     * Return the plain Doi
     *
     * @return the plain Doi value.
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Return a URI presentation for the Doi
     *
     * @return a URI representation of the Doi
     */
    public String getUri() {
        return RESOLVER + doi;
    }

    // TODO: move this GUI code
    public static void removeDOIfromBibtexEntryField(BibtexEntry bes, String fieldName, NamedCompound ce) {
        String origValue = bes.getField(fieldName);
        String value = origValue;
        value = value.replaceAll(REGEX_HTTP_DOI, "");
        value = value.replaceAll(REGEX_DOI, "");
        value = value.trim();
        if (value.isEmpty()) {
            value = null;
        }
        if (!origValue.equals(value)) {
            ce.addEdit(new UndoableFieldChange(bes, fieldName, origValue, value));
            bes.setField(fieldName, value);
        }
    }
}
