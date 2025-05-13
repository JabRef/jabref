package org.jabref.logic.journals;

import java.util.List;

import javafx.util.Pair;

public record JournalInformation(
        String title,
        String publisher,
        String coverageStartYear,
        String coverageEndYear,
        String subjectArea,
        String country,
        String categories,
        String scimagoId,
        String hIndex,
        String issn,
        List<Pair<Integer, Double>> sjrArray,
        List<Pair<Integer, Double>> snipArray,
        List<Pair<Integer, Double>> docsThisYear,
        List<Pair<Integer, Double>> docsPrevious3Years,
        List<Pair<Integer, Double>> citableDocsPrevious3Years,
        List<Pair<Integer, Double>> citesOutgoing,
        List<Pair<Integer, Double>> citesOutgoingPerDoc,
        List<Pair<Integer, Double>> citesIncomingByRecentlyPublished,
        List<Pair<Integer, Double>> citesIncomingPerDocByRecentlyPublished
) {
}
