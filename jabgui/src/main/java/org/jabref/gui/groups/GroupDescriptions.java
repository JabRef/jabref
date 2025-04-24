package org.jabref.gui.groups;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.strings.StringUtil;

public class GroupDescriptions {

    private GroupDescriptions() {
    }

    public static String getShortDescriptionKeywordGroup(KeywordGroup keywordGroup, boolean showDynamic) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (showDynamic) {
            sb.append("<i>").append(StringUtil.quoteForHTML(keywordGroup.getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(keywordGroup.getName()));
        }
        sb.append("</b> - ");
        sb.append(Localization.lang("dynamic group"));
        sb.append(" <b>");
        sb.append(keywordGroup.getSearchField());
        sb.append("</b> ");
        sb.append(Localization.lang("contains"));
        sb.append(" <b>");
        sb.append(StringUtil.quoteForHTML(keywordGroup.getSearchExpression()));
        sb.append("</b>)");
        switch (keywordGroup.getHierarchicalContext()) {
            case INCLUDING:
                sb.append(", ").append(Localization.lang("includes subgroups"));
                break;
            case REFINING:
                sb.append(", ").append(Localization.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    public static String getShortDescriptionExplicitGroup(ExplicitGroup explicitGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(explicitGroup.getName()).append("</b> - ").append(Localization.lang("static group"));
        switch (explicitGroup.getHierarchicalContext()) {
            case INCLUDING:
                sb.append(", ").append(Localization.lang("includes subgroups"));
                break;
            case REFINING:
                sb.append(", ").append(Localization.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    public static String getShortDescriptionAllEntriesGroup() {
        return Localization.lang("<b>All Entries</b> (this group cannot be edited or removed)");
    }

    public static String getShortDescription(SearchGroup searchGroup, boolean showDynamic) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (showDynamic) {
            sb.append("<i>").append(StringUtil.quoteForHTML(searchGroup.getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(searchGroup.getName()));
        }
        sb.append("</b> - ");
        sb.append(Localization.lang("dynamic group"));
        sb.append(" (");
        sb.append(Localization.lang("search expression"));
        sb.append(" <b>").append(StringUtil.quoteForHTML(searchGroup.getSearchExpression())).append("</b>)");
        switch (searchGroup.getHierarchicalContext()) {
            case INCLUDING:
                sb.append(", ").append(Localization.lang("includes subgroups"));
                break;
            case REFINING:
                sb.append(", ").append(Localization.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }
}
