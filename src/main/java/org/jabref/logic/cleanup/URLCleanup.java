package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Checks whether URL exists in note field, and stores it under url field.
 */
public class URLCleanup implements CleanupJob {

    /*
     * The urlRegex was originally fetched from a suggested solution in
     * https://stackoverflow.com/questions/28185064/python-infinite-loop-in-regex-to-match-url.
     * In order to be functional, we made the necessary adjustments regarding Java
     * features (mainly doubled backslashes).
     */
    public static final String URL_REGEX = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.]"
            + "[a-z]{2,4}/)(?:[^\\s()<>\\\\]+|\\(([^\\s()<>\\\\]+|(\\([^\\s()"
            + "<>\\\\]+\\)))*\\))+(?:\\(([^\\s()<>\\\\]+|(\\([^\\s()<>\\\\]+\\"
            + ")))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    public static final String DATE_TERMS_REGEX = "accessed on|visited on|retrieved on|viewed on";

    private static final Field NOTE_FIELD = StandardField.NOTE;
    private static final Field URL_FIELD = StandardField.URL;
    private static final Field URLDATE_FIELD = StandardField.URLDATE;

    final Pattern urlPattern = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);
    final Pattern dateTermsPattern = Pattern.compile(DATE_TERMS_REGEX, Pattern.CASE_INSENSITIVE);
    final Pattern datePattern = Pattern.compile(Date.DATE_REGEX, Pattern.CASE_INSENSITIVE);
    private final NormalizeDateFormatter formatter = new NormalizeDateFormatter();

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        String noteFieldValue = entry.getField(NOTE_FIELD).orElse("");

        final Matcher urlMatcher = urlPattern.matcher(noteFieldValue);
        final Matcher dateTermsMatcher = dateTermsPattern.matcher(noteFieldValue);
        final Matcher dateMatcher = datePattern.matcher(noteFieldValue);

        if (urlMatcher.find()) {
            String url = urlMatcher.group();

            // Remove the URL from the NoteFieldValue
            String newNoteFieldValue = noteFieldValue
                    .replace(url, "")

                    /*
                     * The following regex erases unnecessary remaining
                     * content in note field. Explanation:
                     * <ul>
                     *     <li>"(, )?": Matches an optional comma followed by a space</li>
                     *     <li>"\\?": Matches an optional backslash</li>
                     *     <li>"url\{\}": Matches the literal string "url{}"</li>
                     * </ul>
                     * Note that the backslashes are doubled as Java requirement
                     */
                    .replaceAll("(, )?\\\\?url\\{\\}(, )?", "");

            /*
             * In case the url and note fields hold the same URL, then we just
             * remove it from the note field, and no other action is performed.
             */
            if (entry.hasField(URL_FIELD)) {
                String urlFieldValue = entry.getField(URL_FIELD).orElse("");
                if (urlFieldValue.equals(url)) {
                    entry.setField(NOTE_FIELD, newNoteFieldValue).ifPresent(changes::add);
                }
            } else {
                entry.setField(NOTE_FIELD, newNoteFieldValue).ifPresent(changes::add);
                entry.setField(URL_FIELD, url).ifPresent(changes::add);
            }

            if (dateTermsMatcher.find()) {
                String term = dateTermsMatcher.group();
                newNoteFieldValue = newNoteFieldValue
                        .replace(term, "");
                if (dateMatcher.find()) {
                    String date = dateMatcher.group();
                    String formattedDate = formatter.format(date);
                    newNoteFieldValue = newNoteFieldValue
                            .replace(date, "").trim()
                            .replaceAll("^,|,$", "").trim(); // either starts or ends with a comma

                    // Same approach with the URL cleanup.
                    if (entry.hasField(URLDATE_FIELD)) {
                        String urlDateFieldValue = entry.getField(URLDATE_FIELD).orElse("");
                        if (urlDateFieldValue.equals(formattedDate)) {
                            entry.setField(NOTE_FIELD, newNoteFieldValue).ifPresent(changes::add);
                        }
                    } else {
                        entry.setField(NOTE_FIELD, newNoteFieldValue).ifPresent(changes::add);
                        entry.setField(URLDATE_FIELD, formattedDate).ifPresent(changes::add);
                    }
                }
            }
        }
        return changes;
    }
}
