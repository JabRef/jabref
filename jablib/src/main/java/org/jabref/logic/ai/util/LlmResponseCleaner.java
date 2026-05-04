package org.jabref.logic.ai.util;

import org.jspecify.annotations.Nullable;

/// Cleans raw LLM response strings by extracting content from the last
/// fenced code block (``` ... ```) if present, or simply trimming whitespace.
///
/// Rules:
/// <ol>
/// - If the response contains no ``` fences → return the string stripped of
/// leading/trailing whitespace.
/// - If one or more ``` fences exist → find the *last* complete block,
/// strip the optional language label on the opening fence (e.g. ````json`,
/// ````markdown`), and return the inner content trimmed.
/// - If the last fence is unclosed (no matching closing `````) → treat
/// everything after the opening fence line as the content.
/// </ol>
public final class LlmResponseCleaner {
    private static final String FENCE = "```";

    private LlmResponseCleaner() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Cleans the given LLM response string according to the rules above.
    ///
    /// @param response the raw LLM response; may be `null`
    /// @return the cleaned string, never `null`
    public static String clean(@Nullable String response) {
        if (response == null) {
            return "";
        }

        // No fences at all, then just trim.
        int firstFence = response.indexOf(FENCE);
        if (firstFence == -1) {
            return response.strip();
        }

        // Find the *last* opening fence.
        // An "opening" fence is any ``` that starts a block; we scan from the end.
        // Strategy: collect all fence positions, then walk them in reverse to find
        // the last one that acts as an opener (i.e. followed by content or a closer).
        int lastOpenerStart = findLastOpener(response);
        if (lastOpenerStart == -1) {
            // Shouldn't happen after the firstFence check, but be safe.
            return response.strip();
        }

        // Skip the opening fence marker itself (3 chars).
        int afterFenceMarker = lastOpenerStart + FENCE.length();

        // Skip optional language label: everything up to (but not including) the
        // first newline after the opening ```.
        int newlineAfterLabel = response.indexOf('\n', afterFenceMarker);
        if (newlineAfterLabel == -1) {
            // No newline after the opening fence line means there is no content.
            return "";
        }
        int contentStart = newlineAfterLabel + 1;

        // Look for a closing ``` after the content start.
        int closingFence = response.indexOf(FENCE, contentStart);

        String content;
        if (closingFence == -1) {
            // Unclosed fence, then take everything from contentStart to end.
            content = response.substring(contentStart);
        } else {
            content = response.substring(contentStart, closingFence);
        }

        String stripped = content.stripTrailing();
        return stripped.contains("\n") ? stripped : stripped.strip();
    }

    /// Returns the start index of the last "opening" ``` in the string.
    ///
    /// We treat every ``` as a potential opener/closer pair. Walking through the
    /// string we toggle a flag: the first ``` opens a block, the next closes it, etc.
    /// We remember the start index of each opener and return the last one seen.
    private static int findLastOpener(String s) {
        int lastOpener = -1;
        boolean inBlock = false;
        int pos = 0;

        while (pos < s.length()) {
            int fencePos = s.indexOf(FENCE, pos);
            if (fencePos == -1) {
                break;
            }

            if (!inBlock) {
                // This ``` opens a block.
                lastOpener = fencePos;
                inBlock = true;
            } else {
                // This ``` closes the current block.
                inBlock = false;
            }

            pos = fencePos + FENCE.length();
        }

        return lastOpener;
    }
}
