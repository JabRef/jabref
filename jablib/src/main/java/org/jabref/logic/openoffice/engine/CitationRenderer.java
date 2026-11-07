package org.jabref.logic.openoffice.engine;

import org.jspecify.annotations.NullMarked;

/// Renders an entire citation set without UNO access or document writes. Implementations receive
/// their style explicitly and must not mutate the request's groups or read global style preferences.
/// External conversion failures (for example BST's pandoc step) are checked render failures.
@NullMarked
public interface CitationRenderer {

    StyleCapabilities capabilities();

    RenderResult render(RenderRequest request) throws RenderException;
}
