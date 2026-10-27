package org.jabref.logic.openoffice;

import java.util.List;

import org.jabref.logic.util.strings.StringUtil;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

class ZoteroCitationData {
    public static final String CSL_CITATION_SCHEMA = "https://github.com/citation-style-language/schema/raw/master/csl-citation.json";

    String schema = CSL_CITATION_SCHEMA;
    @SerializedName("citationID")
    String citationId = "";
    List<CitationItemData> citationItems = List.of();

    public static class CitationItemData {
        String id = "";
        List<String> uris = List.of();
        @SerializedName("suppress-author")
        @Nullable
        Boolean suppressAuthor;
        @SerializedName("itemData")
        ItemData itemData = new ItemData();
    }

    public static class ItemData {
        String id = "";
        @SerializedName("citation-key")
        String citationKey = "";
        @Nullable
        String type;

        @SerializedName("abstract")
        @Nullable
        String abstractText;

        @SerializedName("call-number")
        @Nullable
        String callNumber;

        @SerializedName("chapter-number")
        @Nullable
        String chapterNumber;

        @SerializedName("container-title")
        @Nullable
        String containerTitle;

        @SerializedName("collection-title")
        @Nullable
        String collectionTitle;

        @SerializedName("collection-number")
        @Nullable
        String collectionNumber;

        @SerializedName("DOI")
        @Nullable
        String doi;

        @Nullable
        String edition;

        @SerializedName("event-place")
        @Nullable
        String eventPlace;

        @SerializedName("ISBN")
        @Nullable
        String isbn;

        @SerializedName("ISSN")
        @Nullable
        String issn;

        @Nullable
        String issue;

        @Nullable
        String keyword;

        @Nullable
        String language;

        @Nullable
        String number;

        @SerializedName("number-of-pages")
        @Nullable
        String numberOfPages;

        @SerializedName("number-of-volumes")
        @Nullable
        String numberOfVolumes;

        @Nullable
        String page;

        @Nullable
        String publisher;

        @SerializedName("publisher-place")
        @Nullable
        String publisherPlace;

        @Nullable
        String status;

        @Nullable
        String title;

        @SerializedName(value = "title-short", alternate = "shortTitle")
        @Nullable
        String shortTitle;

        @Nullable
        String volume;

        @SerializedName("volume-title")
        @Nullable
        String volumeTitle;

        @SerializedName("URL")
        @Nullable
        String url;

        @Nullable
        String version;

        @Nullable
        String note;

        @Nullable
        String section;

        @Nullable
        List<AuthorData> author;

        @Nullable
        IssuedData issued;

        String getFieldValue(String cslField) {
            @Nullable String value = switch (cslField) {
                case "abstract" ->
                        abstractText;
                case "call-number" ->
                        callNumber;
                case "chapter-number" ->
                        chapterNumber;
                case "collection-title" ->
                        collectionTitle;
                case "container-title" ->
                        containerTitle;
                case "DOI" ->
                        doi;
                case "edition" ->
                        edition;
                case "event-place" ->
                        eventPlace;
                case "publisher-place" ->
                        publisherPlace;
                case "collection-number" ->
                        collectionNumber;
                case "ISBN" ->
                        isbn;
                case "ISSN" ->
                        issn;
                case "issue" ->
                        issue;
                case "keyword" ->
                        keyword;
                case "language" ->
                        language;
                case "number" ->
                        number;
                case "number-of-pages" ->
                        numberOfPages;
                case "number-of-volumes" ->
                        numberOfVolumes;
                case "page" ->
                        page;
                case "publisher" ->
                        publisher;
                case "status" ->
                        status;
                case "title-short",
                     "shortTitle" ->
                        shortTitle;
                case "title" ->
                        title;
                case "URL" ->
                        url;
                case "volume" ->
                        volume;
                case "volume-title" ->
                        volumeTitle;
                case "version" ->
                        version;
                case "note" ->
                        note;
                case "section" ->
                        section;
                default ->
                        null;
            };
            return value == null ? "" : value;
        }

        void omitEmptyFields() {
            type = blankToNull(type);
            abstractText = blankToNull(abstractText);
            callNumber = blankToNull(callNumber);
            chapterNumber = blankToNull(chapterNumber);
            containerTitle = blankToNull(containerTitle);
            collectionTitle = blankToNull(collectionTitle);
            collectionNumber = blankToNull(collectionNumber);
            doi = blankToNull(doi);
            edition = blankToNull(edition);
            eventPlace = blankToNull(eventPlace);
            isbn = blankToNull(isbn);
            issn = blankToNull(issn);
            issue = blankToNull(issue);
            keyword = blankToNull(keyword);
            language = blankToNull(language);
            number = blankToNull(number);
            numberOfPages = blankToNull(numberOfPages);
            numberOfVolumes = blankToNull(numberOfVolumes);
            page = blankToNull(page);
            publisher = blankToNull(publisher);
            publisherPlace = blankToNull(publisherPlace);
            status = blankToNull(status);
            title = blankToNull(title);
            shortTitle = blankToNull(shortTitle);
            volume = blankToNull(volume);
            volumeTitle = blankToNull(volumeTitle);
            url = blankToNull(url);
            version = blankToNull(version);
            note = blankToNull(note);
            section = blankToNull(section);

            if (author != null) {
                List<AuthorData> nonEmptyAuthors = author.stream()
                                                         .peek(AuthorData::omitEmptyFields)
                                                         .filter(authorData -> !authorData.isEmpty())
                                                         .toList();
                author = nonEmptyAuthors.isEmpty() ? null : nonEmptyAuthors;
            }

            if (issued != null) {
                issued.omitEmptyFields();
                if (issued.isEmpty()) {
                    issued = null;
                }
            }
        }
    }

    public static class AuthorData {
        @Nullable
        String family;
        @Nullable
        String given;

        void omitEmptyFields() {
            family = blankToNull(family);
            given = blankToNull(given);
        }

        boolean isEmpty() {
            return StringUtil.isBlank(family) && StringUtil.isBlank(given);
        }
    }

    public static class IssuedData {
        @Nullable
        String raw;
        @SerializedName("date-parts")
        @Nullable
        List<List<Object>> dateParts;

        void omitEmptyFields() {
            raw = blankToNull(raw);
            if (dateParts != null && dateParts.isEmpty()) {
                dateParts = null;
            }
        }

        boolean isEmpty() {
            return StringUtil.isBlank(raw) && (dateParts == null || dateParts.isEmpty());
        }
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return StringUtil.isBlank(value) ? null : value;
    }
}
