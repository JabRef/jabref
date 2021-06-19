package org.jabref.model.openoffice.style;

import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;

public class PageInfo {

    private PageInfo() {
        // hide public constructor
    }

    /*
     * pageInfo normalization
     */
    public static Optional<OOText> normalizePageInfo(Optional<OOText> o) {
        if (o == null || o.isEmpty() || "".equals(OOText.toString(o.get()))) {
            return Optional.empty();
        }
        String s = OOText.toString(o.get());
        if (s.trim().equals("")) {
            return Optional.empty();
        }
        return Optional.of(OOText.fromString(s.trim()));
    }

    /**
     * Defines sort order for pageInfo strings.
     *
     * Optional.empty comes before non-empty.
     */
    public static int comparePageInfo(Optional<OOText> a, Optional<OOText> b) {

        Optional<OOText> aa = PageInfo.normalizePageInfo(a);
        Optional<OOText> bb = PageInfo.normalizePageInfo(b);
        if (aa.isEmpty() && bb.isEmpty()) {
            return 0;
        }
        if (aa.isEmpty()) {
            return -1;
        }
        if (bb.isEmpty()) {
            return +1;
        }
        return aa.get().toString().compareTo(bb.get().toString());
    }
}
