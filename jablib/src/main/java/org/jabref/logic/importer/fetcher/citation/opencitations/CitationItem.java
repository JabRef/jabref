package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.util.Optional;

import com.google.gson.annotations.SerializedName;

public class CitationItem {
    private String oci;
    private String citing;
    private String cited;
    private String creation;
    private String timespan;

    @SerializedName("journal_sc")
    private String journalSelfCitation;

    @SerializedName("author_sc")
    private String authorSelfCitation;

    public String getOci() {
        return oci;
    }

    public void setOci(String oci) {
        this.oci = oci;
    }

    public String getCiting() {
        return citing;
    }

    public void setCiting(String citing) {
        this.citing = citing;
    }

    public String getCited() {
        return cited;
    }

    public void setCited(String cited) {
        this.cited = cited;
    }

    public String getCreation() {
        return creation;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public String getTimespan() {
        return timespan;
    }

    public void setTimespan(String timespan) {
        this.timespan = timespan;
    }

    public String getJournalSelfCitation() {
        return journalSelfCitation;
    }

    public void setJournalSelfCitation(String journalSelfCitation) {
        this.journalSelfCitation = journalSelfCitation;
    }

    public String getAuthorSelfCitation() {
        return authorSelfCitation;
    }

    public void setAuthorSelfCitation(String authorSelfCitation) {
        this.authorSelfCitation = authorSelfCitation;
    }

    public Optional<String> extractDoi(String pidString) {
        if (pidString == null || pidString.isEmpty()) {
            return Optional.empty();
        }

        String[] pids = pidString.split("\\s+");
        for (String pid : pids) {
            if (pid.startsWith("doi:")) {
                return Optional.of(pid.substring(4));
            }
        }
        return Optional.empty();
    }

    public Optional<String> getCitingDoi() {
        return extractDoi(citing);
    }

    public Optional<String> getCitedDoi() {
        return extractDoi(cited);
    }
}
