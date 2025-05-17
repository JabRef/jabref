package org.jabref.gui.fieldeditors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;

/**
 * URL utilities for URLs in the JabRef GUI.
 * <p>
 * For logic-oriented URL utilities see {@link org.jabref.logic.util.URLUtil}.
 */
public class URLUtil {

    private URLUtil() {
    }

    /**
     * Look for the last '.' in the link, and return the following characters.
     * <p>
     * This gives the extension for most reasonably named links.
     *
     * @param link The link
     * @return The suffix, excluding the dot (e.g. "pdf")
     */
    public static Optional<String> getSuffix(final String link, ExternalApplicationsPreferences externalApplicationsPreferences) {
        String strippedLink = link;
        try {
            // Try to strip the query string, if any, to get the correct suffix:
            URL url = org.jabref.logic.util.URLUtil.create(link);
            if ((url.getQuery() != null) && (url.getQuery().length() < (link.length() - 1))) {
                strippedLink = link.substring(0, link.length() - url.getQuery().length() - 1);
            }
        } catch (MalformedURLException e) {
            // Don't report this error, since this getting the suffix is a non-critical
            // operation, and this error will be triggered and reported elsewhere.
        }
        // First see if the stripped link gives a reasonable suffix:
        String suffix;
        int strippedLinkIndex = strippedLink.lastIndexOf('.');
        if ((strippedLinkIndex <= 0) || (strippedLinkIndex == (strippedLink.length() - 1))) {
            suffix = null;
        } else {
            suffix = strippedLink.substring(strippedLinkIndex + 1);
        }
        if (!ExternalFileTypes.isExternalFileTypeByExt(suffix, externalApplicationsPreferences)) {
            // If the suffix doesn't seem to give any reasonable file type, try
            // with the non-stripped link:
            int index = link.lastIndexOf('.');
            if ((index <= 0) || (index == (link.length() - 1))) {
                // No occurrence, or at the end
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (strippedLink.substring(strippedLinkIndex + 1).indexOf('/') >= 1) {
                    return Optional.empty();
                } else {
                    return Optional.of(suffix); // return the first one we found, anyway.
                }
            } else {
                // Check if there are path separators in the suffix - if so, it is definitely
                // not a proper suffix, so we should give up:
                if (link.substring(index + 1).indexOf('/') >= 1) {
                    return Optional.empty();
                } else {
                    return Optional.of(link.substring(index + 1));
                }
            }
        } else {
            return Optional.ofNullable(suffix);
        }
    }
}
