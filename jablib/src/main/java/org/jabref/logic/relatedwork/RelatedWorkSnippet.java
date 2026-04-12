package org.jabref.logic.relatedwork;

/// Snippet extracted from a related work section
///
/// Example:
/// original text: "Unlike Smith et al. [10], our approach uses..."
/// contextText: "Unlike Smith et al., our approach uses..."
/// citationMarker: "[10]"
public record RelatedWorkSnippet(
        // Text extracted from a related work text
        String contextText,

        // Citation marker extracted from a related work text
        String citationMarker
) {
}
