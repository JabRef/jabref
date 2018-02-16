package org.jabref.model.entry.specialfields;

import java.util.Optional;

import org.jabref.model.entry.Keyword;

public enum SpecialFieldValue {

    PRINTED("printed"),
    CLEAR_PRIORITY(null),
    PRIORITY_HIGH("prio1"),
    PRIORITY_MEDIUM("prio2"),
    PRIORITY_LOW("prio3"),
    QUALITY_ASSURED("qualityAssured"),
    CLEAR_RANK(null),
    RANK_1("rank1"),
    RANK_2("rank2"),
    RANK_3("rank3"),
    RANK_4("rank4"),
    RANK_5("rank5"),
    CLEAR_READ_STATUS(null),
    READ("read"),
    SKIMMED("skimmed"),
    RELEVANT("relevant");

    // keyword used at keyword field
    private final Optional<Keyword> keyword;

    /**
     *
     * @param keyword - The keyword to be used at BibTex's keyword field. May be "null" if no keyword is to be set
     */
    SpecialFieldValue(String keyword) {
        this.keyword = Optional.ofNullable(keyword).map(Keyword::new);
    }

    public Optional<Keyword> getKeyword() {
        return keyword;
    }

    public Optional<String> getFieldValue() {
        return keyword.map(Keyword::toString);
    }

}
