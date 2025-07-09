package org.jabref.logic.icore;

public record ConferenceRankingEntry(
        String title,
        String acronym,
        String source,
        String rank,
        String note,
        String dblp,
        String primaryFor,
        String averageRating
) {
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
            Average Rating: %s
            """, title, acronym, source, rank, note, dblp, primaryFor, averageRating);
    }
}
