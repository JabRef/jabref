package org.jabref.logic.icore;

public class ConferenceRankingEntry {
    public final String title;
    public final String acronym;
    public final String source;
    public final String rank;
    public final String note;
    public final String dblp;
    public final String primaryFor;
    public final String comments;
    public final String averageRating;

    public ConferenceRankingEntry(String title, String acronym, String source, String rank,
                                  String note, String dblp, String primaryFor,
                                  String comments, String averageRating) {
        this.title = title;
        this.acronym = acronym;
        this.source = source;
        this.rank = rank;
        this.note = note;
        this.dblp = dblp;
        this.primaryFor = primaryFor;
        this.comments = comments;
        this.averageRating = averageRating;
    }
// Printing the data of of the csv we got based on the acronym lookup in a orderky format in a new window

    @Override
    public String toString() {
        return String.format("""
            Title: %s
            Acronym: %s
            Source: %s
            Rank: %s
            Note: %s
            DBLP: %s
            Primary FoR: %s
            Comments: %s
            Average Rating: %s
            """, title, acronym, source, rank, note, dblp, primaryFor, comments, averageRating);
    }
}
