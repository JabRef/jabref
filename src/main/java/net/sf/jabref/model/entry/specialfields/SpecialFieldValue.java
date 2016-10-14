package net.sf.jabref.model.entry.specialfields;

import java.util.Optional;

import net.sf.jabref.model.entry.Keyword;

public enum SpecialFieldValue {

    PRINTED("printed", "togglePrinted"),
    CLEAR_PRIORITY(null, "clearPriority"),
    PRIO_1("prio1", "setPriority1"),
    PRIO_2("prio2", "setPriority2"),
    PRIO_3("prio3", "setPriority3"),
    QUALITY_ASSURED("qualityAssured", "toggleQualityAssured"),
    CLEAR_RANK(null, "clearRank"),
    RANK_1("rank1", "setRank1"),
    RANK_2("rank2", "setRank2"),
    RANK_3("rank3", "setRank3"),
    RANK_4("rank4", "setRank4"),
    RANK_5("rank5", "setRank5"),
    CLEAR_READ_STATUS(null, "clearReadStatus"),
    READ("read", "setReadStatusToRead"),
    SKIMMED("skimmed", "setReadStatusToSkimmed"),
    RELEVANT("relevant", "toggleRelevance");

    // keyword used at keyword field
    private final Optional<Keyword> keyword;

    // action belonging to this value
    private final String actionName;

    /**
     *
     * @param keyword - The keyword to be used at BibTex's keyword field. May be "null" if no keyword is to be set
     * @param actionName - the action to call
     */
    SpecialFieldValue(
            String keyword,
            String actionName) {
        this.keyword = Optional.ofNullable(keyword).map(Keyword::new);
        this.actionName = actionName;
    }

    public Optional<Keyword> getKeyword() {
        return keyword;
    }

    public String getActionName() {
        return this.actionName;
    }

    public Optional<String> getFieldValue() {
        return keyword.map(Keyword::toString);
    }

}
