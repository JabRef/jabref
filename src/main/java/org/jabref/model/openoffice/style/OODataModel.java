package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;

/**   What is the data stored?   */
public enum OODataModel {

    /** JabRef52: pageInfo belongs to CitationGroup, not Citation. */
    JabRef52,

    /** JabRef60: pageInfo belongs to Citation. */
    JabRef60;

    /**
     * @param pageInfo Nullable.
     * @return JabRef60 style pageInfo list with pageInfo in the last slot.
     */
    public static List<Optional<OOText>> fakePageInfos(String pageInfo, int nCitations) {
        List<Optional<OOText>> pageInfos = new ArrayList<>(nCitations);
        for (int i = 0; i < nCitations; i++) {
            pageInfos.add(Optional.empty());
        }
        if (pageInfo != null) {
            final int last = nCitations - 1;
            Optional<OOText> optionalPageInfo = Optional.ofNullable(OOText.fromString(pageInfo));
            pageInfos.set(last, PageInfo.normalizePageInfo(optionalPageInfo));
        }
        return pageInfos;
    }
}
