package net.sf.jabref.search.describer;

import net.sf.jabref.Globals;
import net.sf.jabref.util.StringUtil;

public class BasicSearchDescriber implements SearchDescriber {

    private final boolean regExp;
    private final boolean caseSensitive;
    private final String query;

    public BasicSearchDescriber(boolean caseSensitive, boolean regExp, String query) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.query = query;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(regExp ? Globals.lang(
                "This group contains entries in which any field contains the regular expression <b>%0</b>",
                StringUtil.quoteForHTML(query))
                : Globals.lang("This group contains entries in which any field contains the term <b>%0</b>",
                StringUtil.quoteForHTML(query)));
        sb.append(" (").append(caseSensitive ? Globals.lang("case sensitive")
                : Globals.lang("case insensitive")).append("). ");
        sb.append(Globals.lang(
                "Entries cannot be manually assigned to or removed from this group."));
        sb.append("<p><br>").append(Globals.lang(
                "Hint%c To search specific fields only, enter for example%c<p><tt>author%esmith and title%eelectrical</tt>"));
        return sb.toString();
    }
}
