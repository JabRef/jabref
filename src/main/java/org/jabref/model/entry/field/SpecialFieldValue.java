package org.jabref.model.entry.field;

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

    // keyword used in keyword field
    private final Optional<Keyword> keyword;

    /**
     * @param keyword - The keyword to be used at BibTex's keyword field. May be "null" if no keyword is to be set
     */
    SpecialFieldValue(String keyword) {
        this.keyword = Optional.ofNullable(keyword).map(Keyword::new);
    }

    public static SpecialFieldValue getRating(int ranking) {
        switch (ranking) {
            case 1:
                return RANK_1;
            case 2:
                return RANK_2;
            case 3:
                return RANK_3;
            case 4:
                return RANK_4;
            case 5:
                return RANK_5;
            default:
                throw new UnsupportedOperationException(ranking + "is not a valid ranking");
        }
    }

    public Optional<Keyword> getKeyword() {
        return keyword;
    }

    public Optional<String> getFieldValue() {
        return keyword.map(Keyword::toString);
    }

    public int toRating() {
        switch (this) {
            case RANK_1:
                return 1;
            case RANK_2:
                return 2;
            case RANK_3:
                return 3;
            case RANK_4:
                return 4;
            case RANK_5:
                return 5;
            default:
                throw new UnsupportedOperationException(this + "is not a valid ranking");
        }
    }
}
