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
        @SerializedName("DOI")
        String doi = "";
        String issue = "";
        String page = "";
        String title = "";
        String volume = "";
        @SerializedName("URL")
        String url = "";
        List<AuthorData> author = List.of();
        IssuedData issued = new IssuedData();
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
