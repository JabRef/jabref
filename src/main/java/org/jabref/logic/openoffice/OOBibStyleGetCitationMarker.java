package org.jabref.logic.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OOBibStyleGetCitationMarker {

    /**
     * Look up the nth author and return the "proper" last name for
     * citation markers.
     *
     * Note: "proper" in the sense that it includes the "von" part
     *        of the name (followed by a space) if there is one.
     *
     * @param al     The author list.
     * @param number The number of the author to return.
     * @return The author name, or an empty String if inapplicable.
     */
    private static String getAuthorLastName(AuthorList al,
                                            int number) {
        StringBuilder sb = new StringBuilder();

        if (al.getNumberOfAuthors() > number) {
            Author a = al.getAuthor(number);
            // "von " if von exists
            Optional<String> von = a.getVon();
            if (von.isPresent() && !von.isEmpty()) {
                sb.append(von.get());
                sb.append(' ');
            }
            // last name if it exists
            sb.append(a.getLast().orElse(""));
        }

        return sb.toString();
    }

    /**
     * @param authorListAsString Passed to {@code AuthorList.parse(authorListAsString)}
     *
     * @param maxAuthors The maximum number of authors to write out.
     *                   If there are more authors, then ET_AL_STRING is emitted
     *                   to mark their omission.
     *                   Set to -1 to write out all authors.
     *
     *                   maxAuthors=0 is pointless, now throws RuntimeException
     *                   (Earlier it behaved as maxAuthors=1)
     *
     *                   maxAuthors less than -1 : throw RuntimeException
     *
     * @param andString  For "A, B[ and ]C"
     * @param yearSep    Appended to result here.
     *                   For example " " in default_authoryear.jstyle
     *
     * @return "Au[AS]Bu[AS]Cu[OXFORD_COMMA][andString]Du[yearSep]"
     *      or "Au[etAlString][yearSep]"
     *
     *             where AS = AUTHOR_SEPARATOR
     *                   Au, Bu, Cu, Du are last names of authors.
     *
     *         Note:
     *          - The "Au[AS]Bu[AS]Cu" (or the "Au") part may be empty (maxAuthors==0 or nAuthors==0).
     *          - OXFORD_COMMA is only emitted if nAuthors is at least 3.
     *          - andString  is only emitted if nAuthors is at least 2.
     *          - yearSep is always emitted
     */
    private static String createAuthorList(OOBibStyle style,
                                           String authorListAsString,
                                           int maxAuthors,
                                           String andString,
                                           String yearSep) {
        Objects.requireNonNull(authorListAsString);

        // Apparently maxAuthorsBeforeEtAl is always 1 for in-text citations.
        // In reference lists can be for example 7,
        // (https://www.chicagomanualofstyle.org/turabian/turabian-author-date-citation-quick-guide.html)
        // but those are handled elsewhere.
        //
        // There is also
        // https://apastyle.apa.org/style-grammar-guidelines/ ...
        //          ... citations/basic-principles/same-year-first-author
        // suggesting the to avoid ambiguity, we may need more than one name
        // before "et al.". We do not currently do this kind of disambiguation,
        // but we might, one day.
        //
        final int maxAuthorsBeforeEtAl = 1;

        // The String to represent authors that are not mentioned,
        // e.g. " et al."
        String etAlString = style.getStringCitProperty(OOBibStyle.ET_AL_STRING);

        // The String to add between author names except the last two,
        // e.g. ", ".
        String authorSep = style.getStringCitProperty(OOBibStyle.AUTHOR_SEPARATOR);

        // The String to put after the second to last author in case
        // of three or more authors: (A, B[,] and C)
        String oxfordComma = style.getStringCitProperty(OOBibStyle.OXFORD_COMMA);

        StringBuilder sb = new StringBuilder();
        AuthorList al = AuthorList.parse(authorListAsString);
        final int nAuthors = al.getNumberOfAuthors();

        // To reduce ambiguity, throw on unexpected values of maxAuthors
        if (maxAuthors == 0) {
            throw new RuntimeException("maxAuthors = 0 in createAuthorList");
        }
        if (maxAuthors < -1) {
            throw new RuntimeException("maxAuthors < -1 in createAuthorList");
        }

        boolean emitAllAuthors = ((nAuthors <= maxAuthors) || (maxAuthors == -1));

        int nAuthorsToEmit = (emitAllAuthors
                              ? nAuthors
                              : Math.min(maxAuthorsBeforeEtAl, nAuthors));

        if (!emitAllAuthors) {
            // If we use "et al." maxAuthorsBeforeEtAl also limits the
            // number of authors emitted.
            nAuthorsToEmit = Math.min(nAuthorsToEmit, maxAuthorsBeforeEtAl);
        }

        if (nAuthorsToEmit > 0) {
            // The first author
            sb.append(getAuthorLastName(al, 0));
        }

        if (nAuthors >= 2) {

            if (emitAllAuthors) {
                // Emit last names, except for the last author
                int j = 1;
                while (j < (nAuthors - 1)) {
                    sb.append(authorSep);
                    sb.append(getAuthorLastName(al, j));
                    j++;
                }
                // oxfordComma if at least 3 authors
                if (nAuthors >= 3) {
                    sb.append(oxfordComma);
                }
                // Emit " and "+"LastAuthor"
                sb.append(andString);
                sb.append(getAuthorLastName(al, nAuthors - 1));

            } else {
                // Emit last names up to nAuthorsToEmit.
                //
                // The (maxAuthorsBeforeEtAl > 1) test may seem
                // superfluous, and yes, it is.
                // It is intended to make sure the compiler eliminates
                // this block as long as maxAuthorsBeforeEtAl is fixed
                // to 1.
                if (maxAuthorsBeforeEtAl > 1) {
                    int j = 1;
                    while (j < nAuthorsToEmit) {
                        sb.append(authorSep);
                        sb.append(getAuthorLastName(al, j));
                        j++;
                    }
                }
                sb.append(etAlString);
            }
        }

        sb.append(yearSep);
        return sb.toString();
    }

    /**
     * Modify entry and uniquefier arrays to facilitate a grouped
     * presentation of uniquefied entries.
     *
     * entries[ (from+1 .. to) ]  = null
     * uniquefiers[from] = String.join( separator, uniquefiers.subList(from,to_inclusive) )
     *
     * @param entries     The entry array.
     * @param uniquefiers The uniquefier array.
     * @param from        The first index to group (inclusive)
     * @param to          The last  index to group (inclusive)
     */
    private static void group(OOBibStyle style,
                              List<BibEntry> entries,
                              String[] uniquefiers,
                              int from,
                              int to) {

        String separator = style.getStringCitProperty(OOBibStyle.UNIQUEFIER_SEPARATOR);

        StringBuilder sb = new StringBuilder(uniquefiers[from]);
        for (int i = from + 1; i <= to; i++) {
            sb.append(separator);
            sb.append(uniquefiers[i]);
            entries.set(i, null); // kill BibEntry?
        }
        uniquefiers[from] = sb.toString();
    }


    /**
     * @param entries
     * @param database
     * @param inParenthesis Emit "(Au, 2000)" if true, "Au (2000)" if false.
     * @param uniquefiers null or of length entries.size(), with
     *                    elements null or letter making a cited
     *                    source unique in citations.
     *
     * @param unlimAuthors Can this be null?
     *                     If not null, then its elements are ...
     *
     * @param pageInfosForCitations
     *                    May be null.
     *                    Any or of its elements can be null.
     */
    public static String getCitationMarker(OOBibStyle style,
                                           List<BibEntry> entries,
                                           Map<BibEntry, BibDatabase> database,
                                           boolean inParenthesis,
                                           String[] uniquefiers,
                                           int[] unlimAuthors,
                                           List<String> pageInfosForCitations
        ) {

        List<String> pageInfos =
            OOBibStyle.regularizePageInfosForCitations(pageInfosForCitations,
                                                       entries.size());
        // Original:
        //
        // Look for groups of uniquefied entries that should be combined in the output.
        // E.g. (Olsen, 2005a, b) should be output instead of (Olsen, 2005a; Olsen, 2005b).
        //
        // Now:
        // - handle pageInfos
        // - allow duplicate entries with same or different pageInfos.
        //
        // We assume entries are already sorted, all we need is to
        // group consecutive entries if we can.
        //
        // We also assume, that identical entries have the same uniquefier.
        //
        // Possibilites for two consecutive entries:
        //   a.entry != b.entry, 
        //
        //
        int piv = -1;

        String tmpMarker = null;

        if (uniquefiers != null) {

            for (int i = 0; i < uniquefiers.length; i++) {

                if ((uniquefiers[i] == null) || uniquefiers[i].isEmpty()) {
                    // This entry has no uniquefier.
                    // Check if we just passed a group of more than one entry with uniquefier:
                    if ((piv > -1) && (i > (piv + 1))) {
                        // Do the grouping:
                        group(style, entries, uniquefiers, piv, i - 1);
                    }

                    piv = -1;
                } else {
                    BibEntry currentEntry = entries.get(i);
                    if (piv == -1) {
                        piv = i;
                        // createNormalizedCitationMarker
                        tmpMarker =
                            getAuthorYearParenthesisMarker(style,
                                                           Collections.singletonList(currentEntry),
                                                           database, // Map<BibEntry, BibDatabase>
                                                           null, // uniqueLetters
                                                           unlimAuthors /* from args!*/);
                    } else {
                        // See if this entry can go into a group with the previous one:
                        // createNormalizedCitationMarker
                        String thisMarker =
                            getAuthorYearParenthesisMarker(style,
                                                           Collections.singletonList(currentEntry),
                                                           database,
                                                           null,
                                                           unlimAuthors);

                        String authorField = style.getStringCitProperty(OOBibStyle.AUTHOR_FIELD);
                        int maxAuthors = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS);
                        String author = getCitationMarkerField(style,
                                                               currentEntry,
                                                               database.get(currentEntry),
                                                               authorField);
                        AuthorList al = AuthorList.parse(author);

                        // assumes (unlimAuthors != null)
                        int prevALim = unlimAuthors[i - 1]; // i always at least 1 here
                        if (!thisMarker.equals(tmpMarker)
                            || ((al.getNumberOfAuthors() > maxAuthors)
                                && (unlimAuthors[i] != prevALim))) {
                            // No match. Update piv to exclude the previous entry. But first check if the
                            // previous entry was part of a group:
                            if ((piv > -1) && (i > (piv + 1))) {
                                // Do the grouping:
                                group(style, entries, uniquefiers, piv, i - 1);
                            }
                            tmpMarker = thisMarker;
                            piv = i;
                        }
                    }
                }

            }
            // Finished with the loop. See if the last entries form a group:
            if (piv >= 0) {
                // Do the grouping:
                group(style, entries, uniquefiers, piv, uniquefiers.length - 1);
            }
        } // if uniquefiers are not null

        if (inParenthesis) {
            return getAuthorYearParenthesisMarker(style, entries, database, uniquefiers, unlimAuthors);
        } else {
            return getAuthorYearInTextMarker(style, entries, database, uniquefiers, unlimAuthors);
        }
    }


    /**
     * This method produces (Author, year) style citation strings in many different forms.
     *
     * @param entries           The list of BibEntry to get fields from.
     * @param database          A map of BibEntry-BibDatabase pairs.
     * @param uniquifiers       Optional parameter to separate similar citations.
     *                          Elements can be null if not needed.
     * @return The formatted citation.
     */
    private static String getAuthorYearParenthesisMarker(OOBibStyle style,
                                                         List<BibEntry> entries,
                                                         Map<BibEntry, BibDatabase> database,
                                                         String[] uniquifiers,
                                                         int[] unlimAuthors) {

        // The bibtex field providing author names, e.g. "author" or
        // "editor".
        String authorField = style.getStringCitProperty(OOBibStyle.AUTHOR_FIELD);

        // The maximum number of authors to write out in full without
        // using etal. Set to -1 to always write out all authors.
        int maxA = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS);

        // The String to separate authors from year, e.g. "; ".
        String yearSep = style.getStringCitProperty(OOBibStyle.YEAR_SEPARATOR);

        // The opening parenthesis.
        String startBrace = style.getStringCitProperty(OOBibStyle.BRACKET_BEFORE);

        // The closing parenthesis.
        String endBrace = style.getStringCitProperty(OOBibStyle.BRACKET_AFTER);

        // The String to separate citations from each other.
        String citationSeparator = style.getStringCitProperty(OOBibStyle.CITATION_SEPARATOR);

        // The bibtex field providing the year, e.g. "year".
        String yearField = style.getStringCitProperty(OOBibStyle.YEAR_FIELD);

        // The String to add between the two last author names, e.g. " & ".
        String andString = style.getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR);

        StringBuilder sb = new StringBuilder(startBrace);
        for (int j = 0; j < entries.size(); j++) {
            BibEntry currentEntry = entries.get(j);

            // Check if this entry has been nulled due to grouping with the previous entry(ies):
            if (currentEntry == null) {
                continue;
            }

            if (j > 0) {
                sb.append(citationSeparator);
            }

            BibDatabase currentDatabase = database.get(currentEntry);

            /* original:
             *
             * int unlimA = (unlimAuthors == null) ? -1 : unlimAuthors[j];
             * int maxAuthors = (unlimA > 0) ? unlimA : maxA;
             *
             * translation:
             *
             * if      ( unlimAuthors == null ) { maxAuthors = maxA; }
             * else if ( unlimAuthors[j] > 0  ) { maxAuthors = unlimAuthors[j]; }
             * else                             { maxAuthors = maxA; }
             *
             * maxAuthors = (( (unlimAuthors != null) && (unlimAuthors[j] > 0) )
             *               ? unlimAuthors[j]
             *               : maxA )
             */
            int maxAuthors = (( (unlimAuthors != null) && (unlimAuthors[j] > 0) )
                              ? unlimAuthors[j]
                              : maxA );

            String author = getCitationMarkerField(style, currentEntry, currentDatabase, authorField);
            String authorString = createAuthorList(style, author, maxAuthors, andString, yearSep);
            sb.append(authorString);
            String year = getCitationMarkerField(style, currentEntry, currentDatabase, yearField);
            if (year != null) {
                sb.append(year);
            }
            if ((uniquifiers != null) && (uniquifiers[j] != null)) {
                sb.append(uniquifiers[j]);
            }
        }
        sb.append(endBrace);
        return sb.toString();
    }

    /**
     * This method produces "Author (year)" style citation strings in many different forms.
     *
     * @param entries     The list of BibEntry to get fields from.
     * @param database    A map of BibEntry-BibDatabase pairs.
     * @param uniquefiers Optional parameters to separate similar citations. Can be null if not needed.
     * @return The formatted citation.
     */
    private static String getAuthorYearInTextMarker(OOBibStyle style,
                                                    List<BibEntry> entries,
                                                    Map<BibEntry, BibDatabase> database,
                                                    String[] uniquefiers,
                                                    int[] unlimAuthors) {
        // The bibtex field providing author names, e.g. "author" or "editor".
        String authorField = style.getStringCitProperty(OOBibStyle.AUTHOR_FIELD);

        // The maximum number of authors to write out in full without using etal. Set to
        // -1 to always write out all authors.
        int maxA = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS);

        // The String to separate authors from year, e.g. "; ".
        String yearSep = style.getStringCitProperty(OOBibStyle.IN_TEXT_YEAR_SEPARATOR);

        // The opening parenthesis.
        String startBrace = style.getStringCitProperty(OOBibStyle.BRACKET_BEFORE);

        // The closing parenthesis.
        String endBrace = style.getStringCitProperty(OOBibStyle.BRACKET_AFTER);

        // The String to separate citations from each other.
        String citationSeparator = style.getStringCitProperty(OOBibStyle.CITATION_SEPARATOR);

        // The bibtex field providing the year, e.g. "year".
        String yearField = style.getStringCitProperty(OOBibStyle.YEAR_FIELD);

        // The String to add between the two last author names, e.g. " & ".
        String andString = style.getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR_IN_TEXT);

        if (andString == null) {
            // Use the default one if no explicit separator for text is defined
            andString = style.getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            BibEntry currentEntry = entries.get(i);

            // Check if this entry has been nulled due to grouping with the previous entry(ies):
            if (currentEntry == null) {
                continue;
            }

            BibDatabase currentDatabase = database.get(currentEntry);

            /* original:
             *
             * int unlimA = (unlimAuthors == null) ? -1 : unlimAuthors[i];
             * int maxAuthors = unlimA > 0 ? unlimA : maxA;
             */
            int maxAuthors = (( (unlimAuthors != null) && (unlimAuthors[i] > 0) )
                              ? unlimAuthors[i]
                              : maxA );

            if (i > 0) {
                sb.append(citationSeparator);
            }
            String author = getCitationMarkerField(style, currentEntry, currentDatabase, authorField);
            String authorString = createAuthorList(style, author, maxAuthors, andString, yearSep);
            sb.append(authorString);
            sb.append(startBrace);
            String year = getCitationMarkerField(style, currentEntry, currentDatabase, yearField);
            if (year != null) {
                sb.append(year);
            }
            if ((uniquefiers != null) && (uniquefiers[i] != null)) {
                sb.append(uniquefiers[i]);
            }
            sb.append(endBrace);
        }
        return sb.toString();
    }

    /**
     * This method looks up a field for an entry in a database. Any
     * number of backup fields can be used if the primary field is
     * empty.
     *
     * @param entry    The entry.
     * @param database The database the entry belongs to.
     * @param fields   The field, or succession of fields, to look up.
     *                 If backup fields are needed, separate
     *                 field names by /. E.g. to use "author" with "editor" as backup,
     *                 specify StandardField.orFields(StandardField.AUTHOR, StandardField.EDITOR).
     * @return The resolved field content, or an empty string if the field(s) were empty.
     */
    private static String getCitationMarkerField(OOBibStyle style,
                                                 BibEntry entry,
                                                 BibDatabase database,
                                                 String fields) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        Objects.requireNonNull(database, "database cannot be null");

        Set<Field> authorFields =
            FieldFactory.parseOrFields(style.getStringCitProperty(OOBibStyle.AUTHOR_FIELD));
        for (Field field : FieldFactory.parseOrFields(fields)) {
            Optional<String> content = entry.getResolvedFieldOrAlias(field, database);

            if ((content.isPresent()) && !content.get().trim().isEmpty()) {
                if (authorFields.contains(field) && StringUtil.isInCurlyBrackets(content.get())) {
                    return "{" + style.fieldFormatter.format(content.get()) + "}";
                }
                return style.fieldFormatter.format(content.get());
            }
        }
        // No luck? Return an empty string:
        return "";
    }


}
