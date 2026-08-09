package org.jabref.logic.journals;

import java.util.List;

import javafx.util.Pair;

public record JournalInformation(
        String title,
        String publisher,
        String hIndex,
        String issn,
        List<Pair<Integer, Double>> worksCount,
        List<Pair<Integer, Double>> citedByCount
) {
}
