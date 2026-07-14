package org.jabref.logic.openoffice;

import java.util.List;

import com.google.gson.annotations.SerializedName;

class ZoteroCitationData {
    List<CitationItemData> citationItems = List.of();

    public static class CitationItemData {
        int id;
        @SerializedName("itemData")
        ItemData itemData = new ItemData();
    }

    public static class ItemData {
        String type = "";

        @SerializedName("abstract")
        String abstractText = "";

        @SerializedName("call-number")
        String callNumber = "";

        @SerializedName("chapter-number")
        String chapterNumber = "";

        @SerializedName("container-title")
        String containerTitle = "";

        @SerializedName("collection-title")
        String collectionTitle = "";

        @SerializedName("collection-number")
        String collectionNumber = "";

        @SerializedName("DOI")
        String doi = "";

        String edition = "";

        @SerializedName("event-place")
        String eventPlace = "";

        @SerializedName("ISBN")
        String isbn = "";

        @SerializedName("ISSN")
        String issn = "";

        String issue = "";

        String keyword = "";

        String language = "";

        String number = "";

        @SerializedName("number-of-pages")
        String numberOfPages = "";

        @SerializedName("number-of-volumes")
        String numberOfVolumes = "";

        String page = "";

        String publisher = "";

        @SerializedName("publisher-place")
        String publisherPlace = "";

        String status = "";

        String title = "";

        @SerializedName(value = "title-short", alternate = "shortTitle")
        String shortTitle = "";

        String volume = "";

        @SerializedName("volume-title")
        String volumeTitle = "";

        @SerializedName("URL")
        String url = "";

        String version = "";

        String note = "";

        String section = "";

        List<AuthorData> author = List.of();

        IssuedData issued = new IssuedData();

        String getFieldValue(String cslField) {
            return switch (cslField) {
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
                        "";
            };
        }
    }

    public static class AuthorData {
        String family = "";
        String given = "";
    }

    public static class IssuedData {
        @SerializedName("date-parts")
        List<List<String>> dateParts = List.of();
    }
}
