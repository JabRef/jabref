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
        List<Pair<Integer, Double>> snipArray
) {
}
