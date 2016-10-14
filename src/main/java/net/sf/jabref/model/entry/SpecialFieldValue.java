package net.sf.jabref.model.entry;

import java.util.Optional;

public class SpecialFieldValue {

    // keyword used at keyword field
    private final Optional<Keyword> keyword;

    // action belonging to this value
    private final String actionName;

    private final String toolTipText;

    /**
     *
     * @param keyword - The keyword to be used at BibTex's keyword field. May be "null" if no keyword is to be set
     * @param actionName - the action to call
     * @param toolTipText - the tool tip text
     */
    public SpecialFieldValue(
            String keyword,
            String actionName,
            String toolTipText) {
        this.keyword = Optional.ofNullable(keyword).map(Keyword::new);
        this.actionName = actionName;
        this.toolTipText = toolTipText;
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

    public String getToolTipText() {
        return this.toolTipText;
    }

}
