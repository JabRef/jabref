package org.jabref.logic.openoffice.style;

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
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationLookupResult;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationMarkerNormEntry;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;
import org.jabref.model.openoffice.style.PageInfo;
import org.jabref.model.strings.StringUtil;

class OOBibStyleGetCitationMarker {

    private OOBibStyleGetCitationMarker() {
        /**/
    }

    /**
     * Look up the nth author and return the "proper" last name for
     * citation markers.
     *
     * Note: "proper" in the sense that it includes the "von" part
     *        of the name (followed by a space) if there is one.
     *
     * @param authorList     The author list.
     * @param number The number of the author to return.
     * @return The author name, or an empty String if inapplicable.
     */
    private static String getAuthorLastName(AuthorList authorList, int number) {
        StringBuilder stringBuilder = new StringBuilder();

        if (authorList.getNumberOfAuthors() > number) {
            Author author = authorList.getAuthor(number);
            // "von " if von exists
            Optional<String> von = author.getVon();
            if (von.isPresent() && !von.get().isEmpty()) {
                stringBuilder.append(von.get());
                stringBuilder.append(' ');
            }
            // last name if it exists
            stringBuilder.append(author.getLast().orElse(""));
        }

        return stringBuilder.toString();
    }

    private static String markupAuthorName(OOBibStyle style, String name) {
        return (style.getAuthorNameMarkupBefore()
                + name
                + style.getAuthorNameMarkupAfter());
    }

    /**
     * @param authorList Parsed list of authors.
     *
     * @param maxAuthors The maximum number of authors to write out.
     *                   If there are more authors, then ET_AL_STRING is emitted
     *                   to mark their omission.
     *                   Set to -1 to write out all authors.
     *
     *                   maxAuthors=0 is pointless, now throws IllegalArgumentException
     *                   (Earlier it behaved as maxAuthors=1)
     *
     *                   maxAuthors less than -1 : throw IllegalArgumentException
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

        // getItalicEtAl is not necessary now, since etAlString could
        // itself contain the markup.
        // This is for backward compatibility.
        if (style.getItalicEtAl()) {
            etAlString = "<i>" + etAlString + "</i>";
        }

        // The String to add between author names except the last two,
        // e.g. ", ".
        String authorSep = style.getAuthorSeparator();

        // The String to put after the second to last author in case
        // of three or more authors: (A, B[,] and C)
        String oxfordComma = style.getOxfordComma();

        StringBuilder stringBuilder = new StringBuilder();

        final int nAuthors = authorList.getNumberOfAuthors();

        // To reduce ambiguity, throw on unexpected values of maxAuthors
        if (maxAuthors == 0 && nAuthors != 0) {
            throw new IllegalArgumentException("maxAuthors = 0 in formatAuthorList");
        }
        if (maxAuthors < -1) {
            throw new IllegalArgumentException("maxAuthors < -1 in formatAuthorList");
        }

        // emitAllAuthors == false means use "et al."
        boolean emitAllAuthors = ((nAuthors <= maxAuthors) || (maxAuthors == -1));

        int nAuthorsToEmit = (emitAllAuthors
                              ? nAuthors
                              // If we use "et al." maxAuthorsBeforeEtAl also limits the
                              // number of authors emitted.
                              : Math.min(maxAuthorsBeforeEtAl, nAuthors));

        if (nAuthorsToEmit >= 1) {
            stringBuilder.append(style.getAuthorsPartMarkupBefore());
            stringBuilder.append(style.getAuthorNamesListMarkupBefore());
            // The first author
            String name = getAuthorLastName(authorList, 0);
            stringBuilder.append(markupAuthorName(style, name));
        }

        if (nAuthors >= 2) {

            if (emitAllAuthors) {
                // Emit last names, except for the last author
                int j = 1;
                while (j < (nAuthors - 1)) {
                    stringBuilder.append(authorSep);
                    String name = getAuthorLastName(authorList, j);
                    stringBuilder.append(markupAuthorName(style, name));
                    j++;
                }
                // oxfordComma if at least 3 authors
                if (nAuthors >= 3) {
                    stringBuilder.append(oxfordComma);
                }
                // Emit " and "+"LastAuthor"
                stringBuilder.append(andString);
                String name = getAuthorLastName(authorList, nAuthors - 1);
                stringBuilder.append(markupAuthorName(style, name));

            } else {
                // Emit last names up to nAuthorsToEmit.
                //
                // The (maxAuthorsBeforeEtAl > 1) test is intended to
                // make sure the compiler eliminates this block as
                // long as maxAuthorsBeforeEtAl is fixed to 1.
                if (maxAuthorsBeforeEtAl > 1) {
                    int j = 1;
                    while (j < nAuthorsToEmit) {
                        stringBuilder.append(authorSep);
                        String name = getAuthorLastName(authorList, j);
                        stringBuilder.append(markupAuthorName(style, name));
                        j++;
                    }
                }
            }
        }

        if (nAuthorsToEmit >= 1) {
            stringBuilder.append(style.getAuthorNamesListMarkupAfter());
        }

        if (nAuthors >= 2 && !emitAllAuthors) {
            stringBuilder.append(etAlString);
        }

        stringBuilder.append(style.getAuthorsPartMarkupAfter());
        return stringBuilder.toString();
    }

    /**
     * On success, getRawCitationMarkerField returns content,
     * but we also need to know which field matched, because
     * for some fields (actually: for author names) we need to
     * reproduce the surrounding braces to inform AuthorList.parse
     * not to split up the content.
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
                                                 CitationLookupResult db,
                                                 OrFields fields) {
        Objects.requireNonNull(db);

        Optional<FieldAndContent> optionalFieldAndContent =
            getRawCitationMarkerField(db.entry, db.database, fields);

        if (optionalFieldAndContent.isEmpty()) {
            // No luck? Return an empty string:
            return "";
        }

        FieldAndContent fieldAndContent = optionalFieldAndContent.get();
        String result = style.getFieldFormatter().format(fieldAndContent.content);

        // If the field we found is mentioned in authorFieldNames and
        // content has a pair of braces around it, we add a pair of
        // braces around the result, so that AuthorList.parse does not split
        // the content.
        final OrFields fieldsToRebrace = style.getAuthorFieldNames();
        if (fieldsToRebrace.contains(fieldAndContent.field) && StringUtil.isInCurlyBrackets(fieldAndContent.content)) {
            result = "{" + result + "}";
        }
        return result;
    }

    private static AuthorList getAuthorList(OOBibStyle style, CitationLookupResult db) {

        // The bibtex fields providing author names, e.g. "author" or
        // "editor".
        OrFields authorFieldNames = style.getAuthorFieldNames();

        String authorListAsString = getCitationMarkerField(style, db, authorFieldNames);
        return AuthorList.parse(authorListAsString);
    }

    private enum AuthorYearMarkerPurpose {
        IN_PARENTHESIS,
        IN_TEXT,
        NORMALIZED
    }

    /**
     * How many authors would be emitted for entry, considering
     * style and entry.getIsFirstAppearanceOfSource()
     *
     * If entry is unresolved, return 0.
     */
    private static int calculateNAuthorsToEmit(OOBibStyle style, CitationMarkerEntry entry) {

        if (entry.getLookupResult().isEmpty()) {
            // unresolved
            return 0;
        }

        int maxAuthors = (entry.getIsFirstAppearanceOfSource()
                          ? style.getMaxAuthorsFirst()
                          : style.getMaxAuthors());

        AuthorList authorList = getAuthorList(style, entry.getLookupResult().get());
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
     *                style.getMaxAuthors, not getMaxAuthorsFirst)
     *
     * @param entries   The list of CitationMarkerEntry values to process.
     *
     *              Here we do not check for duplicate entries: those
     *              are handled by {@code getCitationMarker} by
     *              omitting them from the list.
     *
     *              Unresolved citations recognized by
     *              entry.getBibEntry() and/or
     *              entry.getDatabase() returning empty, and
     *              emitted as "Unresolved${citationKey}".
     *
     *              Neither uniqueLetter nor pageInfo are emitted
     *              for unresolved citations.
     *
     * @param startsNewGroup Should have the same length as {@code entries}, and
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
    private static OOText getAuthorYearParenthesisMarker2(OOBibStyle style,
                                                          AuthorYearMarkerPurpose purpose,
                                                          List<CitationMarkerEntry> entries,
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

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(style.getCitationGroupMarkupBefore());

        if (inParenthesis) {
            stringBuilder.append(startBrace); // shared parenthesis
        }

        for (int j = 0; j < entries.size(); j++) {
            CitationMarkerEntry entry = entries.get(j);
            boolean startingNewGroup = startsNewGroup[j];
            boolean endingAGroup = (j + 1 == entries.size()) || startsNewGroup[j + 1];

            if (!startingNewGroup) {
                // Just add our uniqueLetter
                String uniqueLetter = entry.getUniqueLetter().orElse(null);
                if (uniqueLetter != null) {
                    stringBuilder.append(uniquefierSeparator);
                    stringBuilder.append(uniqueLetter);
                }

                // And close the brace, if we are the last in the group.
                if (!inParenthesis && endingAGroup) {
                    stringBuilder.append(endBrace);
                }
                continue;
            }

            if (j > 0) {
                stringBuilder.append(citationSeparator);
            }

            StringBuilder pageInfoPart = new StringBuilder("");
            if (purpose != AuthorYearMarkerPurpose.NORMALIZED) {
                Optional<OOText> pageInfo =
                    PageInfo.normalizePageInfo(entry.getPageInfo());
                if (pageInfo.isPresent()) {
                    pageInfoPart.append(pageInfoSeparator);
                    pageInfoPart.append(OOText.toString(pageInfo.get()));
                }
            }

            final boolean isUnresolved = entry.getLookupResult().isEmpty();
            if (isUnresolved) {
                stringBuilder.append(String.format("Unresolved(%s)", entry.getCitationKey()));
                if (purpose != AuthorYearMarkerPurpose.NORMALIZED) {
                    stringBuilder.append(pageInfoPart);
                }
            } else {

                CitationLookupResult db = entry.getLookupResult().get();

                int maxAuthors = (purpose == AuthorYearMarkerPurpose.NORMALIZED
                                  ? style.getMaxAuthors()
                                  : calculateNAuthorsToEmit(style, entry));

                if (maxAuthorsOverride.isPresent()) {
                    maxAuthors = maxAuthorsOverride.get();
                }

                AuthorList authorList = getAuthorList(style, db);
                String authorString = formatAuthorList(style, authorList, maxAuthors, andString);
                stringBuilder.append(authorString);
                stringBuilder.append(yearSep);

                if (!inParenthesis) {
                    stringBuilder.append(startBrace); // parenthesis before year
                }

                String year = getCitationMarkerField(style, db, yearFieldNames);
                if (year != null) {
                    stringBuilder.append(year);
                }

                if (purpose != AuthorYearMarkerPurpose.NORMALIZED) {
                    String uniqueLetter = entry.getUniqueLetter().orElse(null);
                    if (uniqueLetter != null) {
                        stringBuilder.append(uniqueLetter);
                    }
                }

                if (purpose != AuthorYearMarkerPurpose.NORMALIZED) {
                    stringBuilder.append(pageInfoPart);
                }

                if (!inParenthesis && endingAGroup) {
                    stringBuilder.append(endBrace);  // parenthesis after year
                }
            }
        } // for j

        if (inParenthesis) {
            stringBuilder.append(endBrace); // shared parenthesis
        }
        stringBuilder.append(style.getCitationGroupMarkupAfter());
        return OOText.fromString(stringBuilder.toString());
    }

    /**
     * Add / override methods for the purpose of creating a normalized citation marker.
     */
    private static class CitationMarkerNormEntryWrap implements CitationMarkerEntry {

        CitationMarkerNormEntry inner;

        CitationMarkerNormEntryWrap(CitationMarkerNormEntry inner) {
            this.inner = inner;
        }

        @Override
        public String getCitationKey() {
            return inner.getCitationKey();
        }

        @Override
        public Optional<CitationLookupResult> getLookupResult() {
            return inner.getLookupResult();
        }

        @Override
        public Optional<String> getUniqueLetter() {
            return Optional.empty();
        }

        @Override
        public Optional<OOText> getPageInfo() {
            return Optional.empty();
        }

        @Override
        public boolean getIsFirstAppearanceOfSource() {
            return false;
        }
    }

    /**
     * @param normEntry          A citation to process.
     *
     * @return A normalized citation marker for deciding which
     *         citations need uniqueLetters.
     *
     * For details of what "normalized" means: {@see getAuthorYearParenthesisMarker2}
     *
     * Note: now includes some markup.
     */
    static OOText getNormalizedCitationMarker(OOBibStyle style,
                                              CitationMarkerNormEntry normEntry,
                                              Optional<Integer> maxAuthorsOverride) {
        boolean[] startsNewGroup = {true};
        CitationMarkerEntry entry = new CitationMarkerNormEntryWrap(normEntry);
        return getAuthorYearParenthesisMarker2(style,
                                               AuthorYearMarkerPurpose.NORMALIZED,
                                               Collections.singletonList(entry),
                                               startsNewGroup,
                                               maxAuthorsOverride);
    }

    private static List<OOText>
    getNormalizedCitationMarkers(OOBibStyle style,
                                 List<CitationMarkerEntry> citationMarkerEntries,
                                 Optional<Integer> maxAuthorsOverride) {

        List<OOText> normalizedMarkers = new ArrayList<>(citationMarkerEntries.size());
        for (CitationMarkerEntry citationMarkerEntry : citationMarkerEntries) {
            OOText normalized = getNormalizedCitationMarker(style,
                                                            citationMarkerEntry,
                                                            maxAuthorsOverride);
            normalizedMarkers.add(normalized);
        }
        return normalizedMarkers;
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
    public static OOText
    createCitationMarker(OOBibStyle style,
                         List<CitationMarkerEntry> citationMarkerEntries,
                         boolean inParenthesis,
                         NonUniqueCitationMarker nonUniqueCitationMarkerHandling) {

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

        List<OOText> normalizedMarkers = getNormalizedCitationMarkers(style,
                                                                      citationMarkerEntries,
                                                                      Optional.empty());

        // How many authors would be emitted without grouping.
        int[] nAuthorsToEmit = new int[nEntries];
        int[] nAuthorsToEmitRevised = new int[nEntries];
        for (int i = 0; i < nEntries; i++) {
            CitationMarkerEntry entry = citationMarkerEntries.get(i);
            int nAuthors = calculateNAuthorsToEmit(style, entry);
            nAuthorsToEmit[i] = nAuthors;
            nAuthorsToEmitRevised[i] = nAuthors;
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
            final CitationMarkerEntry ce1 = citationMarkerEntries.get(i - 1);
            final CitationMarkerEntry ce2 = citationMarkerEntries.get(i);

            final String nm1 = OOText.toString(normalizedMarkers.get(i - 1));
            final String nm2 = OOText.toString(normalizedMarkers.get(i));

            final boolean isUnresolved1 = ce1.getLookupResult().isEmpty();
            final boolean isUnresolved2 = ce2.getLookupResult().isEmpty();

            boolean startingNewGroup;
            boolean sameAsPrev; /* true indicates ce2 may be omitted from output */
            if (isUnresolved2) {
                startingNewGroup = true;
                sameAsPrev = false; // keep it visible
            } else {
                // Does the number of authors to be shown differ?
                // Since we compared normalizedMarkers, the difference
                // between maxAuthors and maxAuthorsFirst may invalidate
                // our expectation that adding uniqueLetter is valid.

                boolean nAuthorsShownInhibitsJoin;
                if (isUnresolved1) {
                    nAuthorsShownInhibitsJoin = true; // no join for unresolved
                } else {
                    final boolean isFirst1 = ce1.getIsFirstAppearanceOfSource();
                    final boolean isFirst2 = ce2.getIsFirstAppearanceOfSource();

                    // nAuthorsToEmitRevised[i-1] may have been indirectly increased,
                    // we have to check that too.
                    if (!isFirst1 &&
                        !isFirst2 &&
                        (nAuthorsToEmitRevised[i - 1] == nAuthorsToEmit[i - 1])) {
                        // we can rely on normalizedMarkers
                        nAuthorsShownInhibitsJoin = false;
                    } else if (style.getMaxAuthors() == style.getMaxAuthorsFirst()) {
                        // we can rely on normalizedMarkers
                        nAuthorsShownInhibitsJoin = false;
                    } else {
                        final int prevShown = nAuthorsToEmitRevised[i - 1];
                        final int need = nAuthorsToEmit[i];

                        if (prevShown < need) {
                            // We do not retrospectively change the number of authors shown
                            // at the previous entry, take that as decided.
                            nAuthorsShownInhibitsJoin = true;
                        } else {
                            // prevShown >= need
                            // Check with extended normalizedMarkers.
                            OOText nmx1 =
                                getNormalizedCitationMarker(style, ce1, Optional.of(prevShown));
                            OOText nmx2 =
                                getNormalizedCitationMarker(style, ce2, Optional.of(prevShown));
                            boolean extendedMarkersDiffer = !nmx2.equals(nmx1);
                            nAuthorsShownInhibitsJoin = extendedMarkersDiffer;
                        }
                    }
                }

                final boolean citationKeysDiffer = !ce2.getCitationKey().equals(ce1.getCitationKey());
                final boolean normalizedMarkersDiffer = !nm2.equals(nm1);

                Optional<OOText> pageInfo2 = PageInfo.normalizePageInfo(ce2.getPageInfo());
                Optional<OOText> pageInfo1 = PageInfo.normalizePageInfo(ce1.getPageInfo());
                final boolean bothPageInfosAreEmpty = pageInfo2.isEmpty() && pageInfo1.isEmpty();
                final boolean pageInfosDiffer = !pageInfo2.equals(pageInfo1);

                Optional<String> ul2 = ce2.getUniqueLetter();
                Optional<String> ul1 = ce1.getUniqueLetter();
                final boolean uniqueLetterPresenceChanged = (ul2.isPresent() != ul1.isPresent());
                final boolean uniqueLettersDiffer = !ul2.equals(ul1);

                final boolean uniqueLetterDoesNotMakeUnique = (citationKeysDiffer
                                                               && !normalizedMarkersDiffer
                                                               && !uniqueLettersDiffer);

                if (uniqueLetterDoesNotMakeUnique &&
                    nonUniqueCitationMarkerHandling.equals(NonUniqueCitationMarker.THROWS)) {
                    throw new IllegalArgumentException("different citation keys,"
                                                       + " but same normalizedMarker and uniqueLetter");
                }

                final boolean pageInfoInhibitsJoin = (bothPageInfosAreEmpty
                                                      ? false
                                                      : (citationKeysDiffer || pageInfosDiffer));

                startingNewGroup = (normalizedMarkersDiffer
                                    || nAuthorsShownInhibitsJoin
                                    || pageInfoInhibitsJoin
                                    || uniqueLetterPresenceChanged
                                    || uniqueLetterDoesNotMakeUnique);

                if (!startingNewGroup) {
                    // inherit from first of group. Used at next i.
                    nAuthorsToEmitRevised[i] = nAuthorsToEmitRevised[i - 1];
                }

                sameAsPrev = (!startingNewGroup
                              && !uniqueLettersDiffer
                              && !citationKeysDiffer
                              && !pageInfosDiffer);
            }

            if (!sameAsPrev) {
                filteredCitationMarkerEntries.add(ce2);
                startsNewGroup[i_out] = startingNewGroup;
                i_out++;
            }
        }

        return getAuthorYearParenthesisMarker2(style,
                                               (inParenthesis
                                               ? AuthorYearMarkerPurpose.IN_PARENTHESIS
                                               : AuthorYearMarkerPurpose.IN_TEXT),
                                              filteredCitationMarkerEntries,
                                              startsNewGroup,
                                              Optional.empty());
    }
}
