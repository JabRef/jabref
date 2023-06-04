package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Checks whether URL exists in note field, and stores it under url field .
 */
public class URLCleanup implements CleanupJob {

    private static final Field NOTE_FIELD = StandardField.NOTE;
    private static final Field URL_FIELD = StandardField.URL;

    @SuppressWarnings("checkstyle:InvalidJavadocPosition")
    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        String NoteFieldValue = entry.getField(NOTE_FIELD).orElse(null);

        /*
         * The urlRegex was originally fetched from a suggested solution in
         * https://stackoverflow.com/questions/28185064/python-infinite-loop-in-regex-to-match-url. In order to be
         * functional, we made the necessary adjustments regarding Java features. (mainly doubled backslashes)
         */
        String urlRegex = (
                "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.]"
                + "[a-z]{2,4}/)(?:[^\\s()<>\\\\]+|\\(([^\\s()<>\\\\]+|(\\([^\\s()"
                + "<>\\\\]+\\)))*\\))+(?:\\(([^\\s()<>\\\\]+|(\\([^\\s()<>\\\\]+\\"
                + ")))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))"
        );

        final Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(NoteFieldValue);

        if (matcher.find()) {
            String url = matcher.group();

            // Remove the URL from the NoteFieldValue
            String newNoteFieldValue = NoteFieldValue
                    .replace(url, "")
                    .replace("/url{}", "")
                    .replace(",", "").trim();

            /*
             * In case the url and note fields hold the same URL, then we just
             * remove it from the note field, and no other action is performed.
             */
            if (entry.hasField(URL_FIELD)) {
                String UrlFieldValue = entry.getField(URL_FIELD).orElse(null);
                if (UrlFieldValue.equals(url)) {
                    entry.setField(NOTE_FIELD, newNoteFieldValue).ifPresent(changes::add);
                }
            } else {
                entry.setField(NOTE_FIELD, newNoteFieldValue).ifPresent(changes::add);
                entry.setField(URL_FIELD, url).ifPresent(changes::add);
            }
        }
        return changes;
    }
}
