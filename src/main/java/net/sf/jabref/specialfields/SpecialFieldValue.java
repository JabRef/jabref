package net.sf.jabref.specialfields;

import java.util.Optional;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.specialfields.SpecialFieldAction;
import net.sf.jabref.gui.specialfields.SpecialFieldMenuAction;
import net.sf.jabref.model.entry.Keyword;

public class SpecialFieldValue {

    private final SpecialField field;

    // keyword used at keyword field
    private final Optional<Keyword> keyword;

    // action belonging to this value
    private final String actionName;

    // localized menu string used at menu / button
    private final String menuString;

    private SpecialFieldAction action;

    private SpecialFieldMenuAction menuAction;

    private final String toolTipText;


    // value when used in a separate vield
    //private String fieldValue;

    /**
     *
     * @param field The special field this value is a value of
     * @param keyword - The keyword to be used at BibTex's keyword field. May be "null" if no keyword is to be set
     * @param actionName - the action to call
     * @param menuString - the string to display at a menu
     * @param toolTipText - the tool tip text
     */
    public SpecialFieldValue(
            SpecialField field,
            String keyword,
            String actionName,
            String menuString,
            String toolTipText) {
        this.field = field;
        this.keyword = Optional.ofNullable(keyword).map(Keyword::new);
        this.actionName = actionName;
        this.menuString = menuString;
        this.toolTipText = toolTipText;
    }

    public Optional<Keyword> getKeyword() {
        return keyword;
    }

    public String getActionName() {
        return this.actionName;
    }

    public String getMenuString() {
        return this.menuString;
    }

    public Optional<String> getFieldValue() {
        return keyword.map(Keyword::toString);
    }

    public String getToolTipText() {
        return this.toolTipText;
    }

    public SpecialFieldAction getAction(JabRefFrame frame) {
        if (this.action == null) {
            action = new SpecialFieldAction(frame, this.field, this.getFieldValue().orElse(null),
                    // if field contains only one value, it has to be nulled
                    // otherwise, another setting does not empty the field
                    this.field.getValues().size() == 1,
                    this.getMenuString());
        }
        return action;
    }

    public SpecialFieldMenuAction getMenuAction(JabRefFrame frame) {
        if (this.menuAction == null) {
            this.menuAction = new SpecialFieldMenuAction(this, frame);
        }
        return this.menuAction;
    }

}
