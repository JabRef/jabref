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

        @SerializedName("container-title")
        String containerTitle = "";

        @SerializedName("collection-title")
        String collectionTitle = "";

        @SerializedName("collection-number")
        String collectionNumber = "";

        @SerializedName("DOI")
        String doi = "";

        String edition = "";

        @SerializedName("ISBN")
        String isbn = "";

        @SerializedName("ISSN")
        String issn = "";

        String issue = "";

        String number = "";

        String page = "";

        String publisher = "";

        @SerializedName(value = "publisher-place", alternate = "event-place")
        String location = "";

        String title = "";

        String volume = "";

        @SerializedName("URL")
        String url = "";

        List<AuthorData> author = List.of();

        List<AuthorData> editor = List.of();

        IssuedData issued = new IssuedData();

        String getFieldValue(String cslField) {
            return switch (cslField) {
                case "collection-title" ->
                        collectionTitle;
                case "container-title" ->
                        containerTitle;
                case "DOI" ->
                        doi;
                case "edition" ->
                        edition;
                case "event-place",
                     "publisher-place" ->
                        location;
                case "collection-number" ->
                        collectionNumber;
                case "ISBN" ->
                        isbn;
                case "ISSN" ->
                        issn;
                case "issue" ->
                        issue;
                case "number" ->
                        number;
                case "page" ->
                        page;
                case "publisher" ->
                        publisher;
                case "title" ->
                        title;
                case "URL" ->
                        url;
                case "volume" ->
                        volume;
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
