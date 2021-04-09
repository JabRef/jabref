package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.strings.StringUtil;

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
            if (von.isPresent() && !von.get().isEmpty()) {
                sb.append(von.get());
                sb.append(' ');
            }
            // last name if it exists
            sb.append(a.getLast().orElse(""));
        }

        return sb.toString();
    }

    /**
     * @param authorList Parsed list of authors.
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
     */
    private static String formatAuthorList(OOBibStyle style,
                                           AuthorList authorList,
                                           int maxAuthors,
                                           String andString) {

        Objects.requireNonNull(authorList);

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
        String etAlString = style.getEtAlString();

        // The String to add between author names except the last two,
        // e.g. ", ".
        String authorSep = style.getAuthorSeparator();

        // The String to put after the second to last author in case
        // of three or more authors: (A, B[,] and C)
        String oxfordComma = style.getOxfordComma();

        StringBuilder sb = new StringBuilder();

        final int nAuthors = authorList.getNumberOfAuthors();

        // To reduce ambiguity, throw on unexpected values of maxAuthors
        if (maxAuthors == 0 && nAuthors != 0) {
            throw new RuntimeException("maxAuthors = 0 in formatAuthorList");
        }
        if (maxAuthors < -1) {
            throw new RuntimeException("maxAuthors < -1 in formatAuthorList");
        }

        // emitAllAuthors == false means use "et al."
        boolean emitAllAuthors = ((nAuthors <= maxAuthors) || (maxAuthors == -1));

        int nAuthorsToEmit = (emitAllAuthors
                              ? nAuthors
                              // If we use "et al." maxAuthorsBeforeEtAl also limits the
                              // number of authors emitted.
                              : Math.min(maxAuthorsBeforeEtAl, nAuthors));

        if (nAuthorsToEmit > 0) {
            // The first author
            sb.append(getAuthorLastName(authorList, 0));
        }

        if (nAuthors >= 2) {

            if (emitAllAuthors) {
                // Emit last names, except for the last author
                int j = 1;
                while (j < (nAuthors - 1)) {
                    sb.append(authorSep);
                    sb.append(getAuthorLastName(authorList, j));
                    j++;
                }
                // oxfordComma if at least 3 authors
                if (nAuthors >= 3) {
                    sb.append(oxfordComma);
                }
                // Emit " and "+"LastAuthor"
                sb.append(andString);
                sb.append(getAuthorLastName(authorList, nAuthors - 1));

            } else {
                // Emit last names up to nAuthorsToEmit.
                //
                // The (maxAuthorsBeforeEtAl > 1) test is intended to
                // make sure the compiler eliminates this block as
                // long as maxAuthorsBeforeEtAl is fixed to 1.
                if (maxAuthorsBeforeEtAl > 1) {
                    int j = 1;
                    while (j < nAuthorsToEmit) {
                        sb.append(authorSep);
                        sb.append(getAuthorLastName(authorList, j));
                        j++;
                    }
                }
                sb.append(etAlString);
            }
        }

        return sb.toString();
    }

    /**
     * On success, getRawCitationMarkerField returns content,
     * but we also need to know which field matched, because
     * for some fields (actually: for author names) we need to
     * reproduce the surrounding braces to inform AuthorList.parse
     * not to split up teh content.
     */
    private static class FieldAndContent {
        Field field;
        String content;
        FieldAndContent(Field field, String content) {
            this.field = field;
            this.content = content;
        }
    }

    /**
     * @return the field and the content of the first nonempty (after trimming)
     * field (or alias) from {@code fields} found in {@code entry}.
     * Return {@code Optional.empty()} if found nothing.
     */
    private static Optional<FieldAndContent> getRawCitationMarkerField(BibEntry entry,
                                                                       BibDatabase database,
                                                                       OrFields fields) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        Objects.requireNonNull(database, "database cannot be null");

        for (Field field : fields /* FieldFactory.parseOrFields(fields)*/) {
            Optional<String> optionalContent = entry.getResolvedFieldOrAlias(field, database);
            final boolean foundSomething = (optionalContent.isPresent()
                                            && !optionalContent.get().trim().isEmpty());
            if (foundSomething) {
                return Optional.of(new FieldAndContent(field, optionalContent.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * This method looks up a field for an entry in a database.
     *
     * Any number of backup fields can be used if the primary field is
     * empty.
     *
     * @param entry    The entry.
     * @param database The database the entry belongs to.
     *
     * @param fields   A list of fields, to look up, using first nonempty hit.
     *
     *                 If backup fields are needed, separate field
     *                 names by /.
     *
     *                 E.g. to use "author" with "editor" as backup,
     *                 specify
     *                     FieldFactory.serializeOrFields(StandardField.AUTHOR,
     *                                                    StandardField.EDITOR)
     *
     * @return The resolved field content, or an empty string if the
     *         field(s) were empty.
     *
     *
     *
     */
    private static String getCitationMarkerField(OOBibStyle style,
                                                 BibEntry entry,
                                                 BibDatabase database,
                                                 OrFields fields) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        Objects.requireNonNull(database, "database cannot be null");

        Optional<FieldAndContent> optionalFieldAndContent =
            getRawCitationMarkerField(entry, database, fields);

        if (optionalFieldAndContent.isEmpty()) {
            // No luck? Return an empty string:
            return "";
        }

        FieldAndContent fc = optionalFieldAndContent.get();
        String result = style.fieldFormatter.format(fc.content);

        // If the field we found is mentioned in authorFieldNames and
        // content has a pair of braces around it, we add a pair of
        // braces around the result, so that AuthorList.parse does not split
        // the content.
        final OrFields fieldsToRebrace = style.getAuthorFieldNames();
        if (fieldsToRebrace.contains(fc.field) && StringUtil.isInCurlyBrackets(fc.content)) {
            result = "{" + result + "}";
        }
        return result;
    }

    private static AuthorList getAuthorList(OOBibStyle style,
                                            BibEntry entry,
                                            BibDatabase database) {

        // The bibtex fields providing author names, e.g. "author" or
        // "editor".
        OrFields authorFieldNames = style.getAuthorFieldNames();

        String authorListAsString = getCitationMarkerField(style,
                                                           entry,
                                                           database,
                                                           authorFieldNames);
        return AuthorList.parse(authorListAsString);
    }

    private enum AuthorYearMarkerPurpose {
        IN_PARENTHESIS,
        IN_TEXT,
        NORMALIZED
    }

    /**
     * How many authors would be emitted for ce, considering
     * style and ce.getIsFirstAppearanceOfSource()
     *
     * If ce is unresolved, return 0.
     */
    private static int calculateNAuthorsToEmit(OOBibStyle style,
                                               CitationMarkerEntry ce) {

        int maxAuthors = (ce.getIsFirstAppearanceOfSource()
                          ? style.getMaxAuthorsFirst()
                          : style.getMaxAuthors());

        BibEntry bibEntry = ce.getBibEntryOrNull();
        BibDatabase database = ce.getDatabaseOrNull();
        boolean isUnresolved = (bibEntry == null) || (database == null);
        if (isUnresolved) {
            return 0;
        }

        AuthorList authorList = getAuthorList(style, bibEntry, database);
        int nAuthors = authorList.getNumberOfAuthors();

        if (maxAuthors == -1) {
            return nAuthors;
        } else {
            return Integer.min(nAuthors, maxAuthors);
        }
    }

    /**
     * Produce (Author, year) or "Author (year)" style citation strings.
     *
     * @param purpose IN_PARENTHESIS and NORMALIZED puts parentheses around the whole,
     *                IN_TEXT around each (year,uniqueLetter,pageInfo) part.
     *
     *                NORMALIZED omits uniqueLetter and pageInfo,
     *                ignores isFirstAppearanceOfSource (always
     *                style.getMaxAuthors, not getMaxAuthorsFirst) and
     *                probably assumes a single CitationMarkerEntry.
     *
     * @param ces   The list of CitationMarkerEntry values to process.
     *
     *              Here we do not check for duplicate entries: those
     *              are handled by {@code getCitationMarker} by
     *              omitting them from the list.
     *
     *              Unresolved citations recognized by
     *              ce.getBibEntryOrNull() and/or
     *              ce.getDatabaseOrNull() returning null, and
     *              emitted as "Unresolved${citationKey}".
     *
     *              Neither uniqueLetter nor pageInfo are emitted
     *              for unresolved citations.
     *
     * @param startsNewGroup Should have the same length as {@code ces}, and
     *               contain true for entries starting a new group,
     *               false for those that only add a uniqueLetter to
     *               the grouped presentation.
     *
     * @param maxAuthorsOverride If not empty, always show this number of authors.
     *               Added to allow NORMALIZED to use maxAuthors value that differs from
     *               style.getMaxAuthors()
     *
     * @return The formatted citation.
     *
     */
    private static String getAuthorYearParenthesisMarker(OOBibStyle style,
                                                         AuthorYearMarkerPurpose purpose,
                                                         List<CitationMarkerEntry> ces,
                                                         boolean[] startsNewGroup,
                                                         Optional<Integer> maxAuthorsOverride) {

        boolean inParenthesis = (purpose == AuthorYearMarkerPurpose.IN_PARENTHESIS
                                 || purpose == AuthorYearMarkerPurpose.NORMALIZED);

        // The String to separate authors from year, e.g. "; ".
        String yearSep = (inParenthesis
                          ? style.getYearSeparator()
                          : style.getYearSeparatorInText());

        // The opening parenthesis.
        String startBrace = style.getBracketBefore();

        // The closing parenthesis.
        String endBrace = style.getBracketAfter();

        // The String to separate citations from each other.
        String citationSeparator = style.getCitationSeparator();

        // The bibtex field providing the year, e.g. "year".
        OrFields yearFieldNames = style.getYearFieldNames();

        // The String to add between the two last author names, e.g. " & ".
        String andString = (inParenthesis
                            ? style.getAuthorLastSeparator()
                            : style.getAuthorLastSeparatorInTextWithFallBack());

        String pageInfoSeparator = style.getPageInfoSeparator();
        String uniquefierSeparator = style.getUniquefierSeparator();

        StringBuilder sb = new StringBuilder();
        if (inParenthesis) {
            sb.append(startBrace);
        }

        for (int j = 0; j < ces.size(); j++) {
            CitationMarkerEntry ce = ces.get(j);
            boolean startingNewGroup = startsNewGroup[j];
            boolean endingAGroup = (j + 1 == ces.size()) || startsNewGroup[j + 1];

            if (!startingNewGroup) {
                // Just add our uniqueLetter
                String uniqueLetter = ce.getUniqueLetterOrNull();
                if (uniqueLetter != null) {
                    sb.append(uniquefierSeparator);
                    sb.append(uniqueLetter);
                }

                // And close the brace, if we are the last in the group.
                if (!inParenthesis && endingAGroup) {
                    sb.append(endBrace);
                }
                continue;
            }

            if (j > 0) {
                sb.append(citationSeparator);
            }

            BibDatabase currentDatabase = ce.getDatabaseOrNull();
            BibEntry currentEntry = ce.getBibEntryOrNull();

            boolean isUnresolved = (currentEntry == null) || (currentDatabase == null);

            if (isUnresolved) {
                sb.append(String.format("Unresolved(%s)", ce.getCitationKey()));
            } else {

                int maxAuthors = (purpose == AuthorYearMarkerPurpose.NORMALIZED
                                  ? style.getMaxAuthors()
                                  : calculateNAuthorsToEmit(style, ce));

                if (maxAuthorsOverride.isPresent()) {
                    maxAuthors = maxAuthorsOverride.get();
                }

                AuthorList authorList = getAuthorList(style,
                                                      currentEntry,
                                                      currentDatabase);
                String authorString = formatAuthorList(style, authorList, maxAuthors, andString);
                sb.append(authorString);
                sb.append(yearSep);

                if (!inParenthesis) {
                    sb.append(startBrace);
                }

                String year = getCitationMarkerField(style,
                                                     currentEntry,
                                                     currentDatabase,
                                                     yearFieldNames);
                if (year != null) {
                    sb.append(year);
                }

                if (purpose != AuthorYearMarkerPurpose.NORMALIZED) {
                    String uniqueLetter = ce.getUniqueLetterOrNull();
                    if (uniqueLetter != null) {
                        sb.append(uniqueLetter);
                    }

                    String pageInfo = OOBibStyle.regularizePageInfo(ce.getPageInfoOrNull());
                    if (pageInfo != null) {
                        sb.append(pageInfoSeparator);
                        sb.append(pageInfo);
                    }
                }

                if (!inParenthesis && endingAGroup) {
                    sb.append(endBrace);
                }
            }
        } // for j

        if (inParenthesis) {
            sb.append(endBrace);
        }
        return sb.toString();
    }

    // "" is more convenient to compare for equality than null-or-String
    private static String nullToEmptyString(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    /**
     * @param ce          A citation to process.
     *
     * @return A normalized citation marker for deciding which
     *         citations need uniqueLetters.
     *
     * For details of what "normalized" means: {@see getAuthorYearParenthesisMarker}
     */
    public static String getNormalizedCitationMarker(OOBibStyle style,
                                                     CitationMarkerEntry ce,
                                                     Optional<Integer> maxAuthorsOverride) {
        boolean[] startsNewGroup = {true};
        return getAuthorYearParenthesisMarker(style,
                                              AuthorYearMarkerPurpose.NORMALIZED,
                                              Collections.singletonList(ce),
                                              startsNewGroup,
                                              maxAuthorsOverride);
    }

    /**
     * Produce citation marker for a citation group.
     *
     * Attempts to join consecutive citations: if normalized citations
     *    markers match and no pageInfo is present, the second entry
     *    can be presented by appending its uniqueLetter to the
     *    previous.
     *
     *    If either entry has pageInfo, join is inhibited.
     *    If the previous entry has more names than we need
     *    we check with extended normalizedMarkers if they match.
     *
     * For consecutive identical entries, the second one is omitted.
     *     Identical requires same pageInfo here, we do not try to merge them.
     *     Note: notifying the user about them would be nice.
     *
     * @param citationMarkerEntries A group of citations to process.
     *
     * @param inParenthesis If true, put parenthesis around the whole group,
     *             otherwise around each (year,uniqueLetter,pageInfo) part.
     *
     * @param nonUniqueCitationMarkerHandling What should happen if we
     *             stumble upon citations with identical normalized
     *             citation markers which cite different sources and
     *             are not distinguished by uniqueLetters.
     *
     *             Note: only consecutive citations are checked.
     *
     */
    public static String
    getCitationMarker(OOBibStyle style,
                      List<CitationMarkerEntry> citationMarkerEntries,
                      boolean inParenthesis,
                      OOBibStyle.NonUniqueCitationMarker nonUniqueCitationMarkerHandling) {

        final int nEntries = citationMarkerEntries.size();

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
        // We also assume, that identical entries have the same uniqueLetters.
        //

        List<String> normalizedMarkers = new ArrayList<>(nEntries);
        for (CitationMarkerEntry citationMarkerEntry : citationMarkerEntries) {
            String nm = getNormalizedCitationMarker(style,
                                                    citationMarkerEntry,
                                                    Optional.empty());
            normalizedMarkers.add(nm);
        }

        // How many authors would be emitted without grouping.
        // Later overwritten for group members with value for
        // first of the group.
        int[] nAuthorsToEmit = new int[nEntries];
        int[] nAuthorsToEmitRevised = new int[nEntries];
        for (int i = 0; i < nEntries; i++) {
            CitationMarkerEntry ce = citationMarkerEntries.get(i);
            nAuthorsToEmit[i] = calculateNAuthorsToEmit(style, ce);
            nAuthorsToEmitRevised[i] = calculateNAuthorsToEmit(style, ce);
        }

        boolean[] startsNewGroup = new boolean[nEntries];
        List<CitationMarkerEntry> filteredCitationMarkerEntries = new ArrayList<>(nEntries);
        int i_out = 0;

        if (nEntries > 0) {
            filteredCitationMarkerEntries.add(citationMarkerEntries.get(0));
            startsNewGroup[i_out] = true;
            i_out++;
        }

        for (int i = 1; i < nEntries; i++) {
            CitationMarkerEntry ce1 = citationMarkerEntries.get(i - 1);
            CitationMarkerEntry ce2 = citationMarkerEntries.get(i);
            String nm1 = normalizedMarkers.get(i - 1);
            String nm2 = normalizedMarkers.get(i);

            BibEntry bibEntry1 = ce1.getBibEntryOrNull();
            BibEntry bibEntry2 = ce2.getBibEntryOrNull();

            BibDatabase database1 = ce1.getDatabaseOrNull();
            BibDatabase database2 = ce2.getDatabaseOrNull();

            boolean isUnresolved1 = (bibEntry1 == null) || (database1 == null);
            boolean isUnresolved2 = (bibEntry2 == null) || (database2 == null);

            boolean startingNewGroup;
            boolean sameAsPrev;
            if (isUnresolved2) {
                startingNewGroup = true;
                sameAsPrev = false; // keep it visible
            } else {
                // Does the number of authors to be shown differ?
                // Since we compared normalizedMarkers, the difference
                // between maxAuthors and maxAuthorsFirst may invalidate
                // our expectation that adding uniqueLetter is valid.

                boolean firstAppearanceInhibitsJoin;
                if (isUnresolved1) {
                    firstAppearanceInhibitsJoin = true; // no join for unresolved
                } else {
                    boolean isFirst1 = ce1.getIsFirstAppearanceOfSource();
                    boolean isFirst2 = ce2.getIsFirstAppearanceOfSource();

                    // nAuthorsToEmitRevised[i-1] may have been indirectly increased,
                    // we have to check that too.
                    if (!isFirst1 &&
                        !isFirst2 &&
                        (nAuthorsToEmitRevised[i - 1] == nAuthorsToEmit[i - 1])) {
                        // we can rely on normalizedMarkers
                        firstAppearanceInhibitsJoin = false;
                    } else if (style.getMaxAuthors() == style.getMaxAuthorsFirst()) {
                        // we can rely on normalizedMarkers
                        firstAppearanceInhibitsJoin = false;
                    } else {
                        int prevShown = nAuthorsToEmitRevised[i - 1];
                        int need = nAuthorsToEmit[i];

                        if (prevShown < need) {
                            // We do not retrospectively change the number of authors shown
                            // at the previous entry, take that as decided.
                            firstAppearanceInhibitsJoin = true;
                        } else {
                            // prevShown >= need
                            // Check with extended normalizedMarkers.
                            String nmx1 = getNormalizedCitationMarker(style, ce1, Optional.of(prevShown));
                            String nmx2 = getNormalizedCitationMarker(style, ce2, Optional.of(prevShown));
                            firstAppearanceInhibitsJoin = !nmx2.equals(nmx1);
                        }
                    }
                }

                String pi2 = nullToEmptyString(OOBibStyle.regularizePageInfo(ce2.getPageInfoOrNull()));
                String pi1 = nullToEmptyString(OOBibStyle.regularizePageInfo(ce1.getPageInfoOrNull()));

                String ul2 = ce2.getUniqueLetterOrNull();
                String ul1 = ce1.getUniqueLetterOrNull();

                boolean uniqueLetterPresenceChanged = (ul2 == null) != (ul1 == null);

                String xul2 = nullToEmptyString(ul2);
                String xul1 = nullToEmptyString(ul1);

                String k2 = ce2.getCitationKey();
                String k1 = ce1.getCitationKey();

                boolean uniqueLetterDoesNotMakeUnique = (nm2.equals(nm1)
                                                         && xul2.equals(xul1)
                                                         && !k2.equals(k1));

                if (uniqueLetterDoesNotMakeUnique &&
                    nonUniqueCitationMarkerHandling == OOBibStyle.NonUniqueCitationMarker.THROWS) {
                    throw new RuntimeException("different citation keys,"
                                               + " but same normalizedMarker and uniqueLetter");
                }

                boolean pageInfoInhibitsJoin;
                if (pi1.equals("") && pi2.equals("")) {
                    pageInfoInhibitsJoin = false;
                } else {
                    pageInfoInhibitsJoin = !(k2.equals(k1) && pi2.equals(pi1));
                }

                boolean normalizedMarkerChanged = !nm2.equals(nm1);
                startingNewGroup = (
                    normalizedMarkerChanged
                    || firstAppearanceInhibitsJoin
                    || pageInfoInhibitsJoin
                    || uniqueLetterPresenceChanged
                    || uniqueLetterDoesNotMakeUnique);

                if (!startingNewGroup) {
                    // inherit from first of group. Used at next i.
                    nAuthorsToEmitRevised[i] = nAuthorsToEmitRevised[i - 1];
                }

                sameAsPrev = (!startingNewGroup
                              && xul2.equals(xul1)
                              && k2.equals(k1)
                              && pi2.equals(pi1));
            }

            if (!sameAsPrev) {
                filteredCitationMarkerEntries.add(ce2);
                startsNewGroup[i_out] = startingNewGroup;
                i_out++;
            }
        }

        return getAuthorYearParenthesisMarker(style,
                                              (inParenthesis
                                               ? AuthorYearMarkerPurpose.IN_PARENTHESIS
                                               : AuthorYearMarkerPurpose.IN_TEXT),
                                              filteredCitationMarkerEntries,
                                              startsNewGroup,
                                              Optional.empty());
    }
}
