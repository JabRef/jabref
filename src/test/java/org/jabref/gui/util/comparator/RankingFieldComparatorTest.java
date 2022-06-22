package org.jabref.gui.util.comparator;

import java.util.Optional;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.model.entry.field.SpecialFieldValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RankingFieldComparatorTest {

    private RankingFieldComparator comparator;
    private final SpecialFieldValue value1 = SpecialFieldValue.RANK_1;
    private final SpecialFieldValue value2 = SpecialFieldValue.RANK_2;
    private final SpecialFieldValue value3 = SpecialFieldValue.RANK_3;
    private final Optional<SpecialFieldValueViewModel> rank1 = Optional.of(new SpecialFieldValueViewModel(value1));
    private final Optional<SpecialFieldValueViewModel> rank2 = Optional.of(new SpecialFieldValueViewModel(value2));
    private final Optional<SpecialFieldValueViewModel> rank3 = Optional.of(new SpecialFieldValueViewModel(value3));

    @BeforeEach
    public void setUp() {
        comparator = new RankingFieldComparator();
    }

    @Test
    public void compareHigherRankFirst() {
        assertEquals(-2, comparator.compare(rank3, rank1));
        assertEquals(-1, comparator.compare(rank2, rank1));
    }

    @Test
    public void compareLowerRankFirst() {
        assertEquals(1, comparator.compare(rank1, rank2));
        assertEquals(2, comparator.compare(rank1, rank3));
    }

    @Test
    public void compareSameRank() {
        assertEquals(0, comparator.compare(rank1, rank1));
    }

    @Test
    public void compareTwoEmptyInputs() {
        assertEquals(0, comparator.compare(Optional.empty(), Optional.empty()));
    }

    @Test
    public void compareTwoInputsWithFirstEmpty() {
        assertEquals(1, comparator.compare(Optional.empty(), rank1));
    }

    @Test
    public void compareTwoInputsWithSecondEmpty() {
        assertEquals(-1, comparator.compare(rank1, Optional.empty()));
    }
}
