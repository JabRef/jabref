package org.jabref.logic.openoffice.engine.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoCast;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.star.document.UndoManagerEvent;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerListener;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifyBroadcaster;
import com.sun.star.util.XModifyListener;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Writes inside existing canonical marks. One instance belongs to one document connection;
/// its bounded, in-memory baseline compares formatted text, never Writer's unformatted strings.
/// Document edits and undo/redo invalidate that baseline. Callers bracket their own complete write
/// actions with `beginUpdate` and `endUpdate`, and detach listeners with `close` on disconnect.
@NullMarked
public final class MarkWriter implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkWriter.class);

    private final XTextDocument document;
    private final XComponentContext context;
    private final Cache<String, OOText> lastWritten = Caffeine.newBuilder().maximumSize(2048).build();
    private final AtomicInteger updateDepth = new AtomicInteger();
    private final DocumentListener listener = new DocumentListener();
    private final Optional<XModifyBroadcaster> modifications;
    private final Optional<XUndoManager> undoManager;
    private volatile boolean closed;

    public MarkWriter(XTextDocument document, XComponentContext context) {
        this.document = document;
        this.context = context;
        modifications = UnoCast.cast(XModifyBroadcaster.class, document);
        undoManager = UnoUndo.getXUndoManager(document);
        modifications.ifPresent(broadcaster -> broadcaster.addModifyListener(listener));
        undoManager.ifPresent(manager -> manager.addUndoManagerListener(listener));
    }

    /// Bracket the complete document action, including undo-context and controller-lock changes.
    /// Calls may nest; direct `write` and `apply` calls also protect their own modifications.
    public void beginUpdate() {
        if (!closed) {
            updateDepth.incrementAndGet();
        }
    }

    public void endUpdate() {
        if (closed) {
            return;
        }
        if (updateDepth.getAndUpdate(depth -> Math.max(0, depth - 1)) == 0) {
            LOGGER.warn("Unbalanced canonical writer update scope");
            clear();
        }
    }

    /// Returns the number of markers actually rewritten. Missing output is rejected before writing.
    public int apply(MarkStore store, List<CitationGroup> groups, Map<CitationGroupId, OOText> markers)
            throws NoDocumentException, CreationException, WrappedTargetException {
        requireOpen();
        for (CitationGroup group : groups) {
            if (!markers.containsKey(group.groupId) || store.getMarkData(group).isEmpty()) {
                throw new CreationException("Missing rendered text or stored range for citation group: " + group.groupId.citationGroupIdAsString());
            }
        }
        beginUpdate();
        try {
            int changed = 0;
            for (CitationGroup group : groups) {
                if (write(store, group, Optional.ofNullable(markers.get(group.groupId))
                                                .orElseThrow(() -> new CreationException("Missing rendered citation text")))) {
                    changed++;
                }
            }
            return changed;
        } finally {
            endUpdate();
        }
    }

    public boolean write(MarkStore store, CitationGroup group, OOText marker)
            throws NoDocumentException, CreationException, WrappedTargetException {
        requireOpen();
        beginUpdate();
        try {
            String uid = store.getMarkData(group)
                              .orElseThrow(() -> new CreationException("Unknown canonical citation group"))
                              .uniqueId();
            OOText text = group.citationType.withText() ? marker : OOText.fromString("");
            if (modifications.isPresent() && Optional.ofNullable(lastWritten.getIfPresent(uid)).filter(text::equals).isPresent()) {
                return false;
            }
            lastWritten.invalidate(uid);
            ReferenceMarkTextWriter.write(document, context, store.referenceMark(group), text);
            if (!closed && modifications.isPresent()) {
                lastWritten.put(uid, text);
            }
            return true;
        } finally {
            endUpdate();
        }
    }

    public void invalidate(String uniqueId) {
        lastWritten.invalidate(uniqueId);
    }

    /// Discards rendered baselines without ending document observation.
    public void clear() {
        lastWritten.invalidateAll();
    }

    /// Ends this connection's observation permanently; repeated close calls are harmless.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        clear();
        try {
            modifications.ifPresent(broadcaster -> broadcaster.removeModifyListener(listener));
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not detach the canonical writer modification listener", exception);
        }
        try {
            undoManager.ifPresent(manager -> manager.removeUndoManagerListener(listener));
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not detach the canonical writer undo listener", exception);
        }
    }

    private void requireOpen() throws CreationException {
        if (closed) {
            throw new CreationException("Canonical citation writer is closed");
        }
    }

    private void externalChange() {
        if (updateDepth.get() == 0) {
            clear();
        }
    }

    @NullMarked
    private final class DocumentListener extends WeakBase implements XModifyListener, XUndoManagerListener {
        @Override
        public void modified(EventObject event) {
            externalChange();
        }

        @Override
        public void undoActionAdded(UndoManagerEvent event) {
            externalChange();
        }

        @Override
        public void actionUndone(UndoManagerEvent event) {
            clear();
        }

        @Override
        public void actionRedone(UndoManagerEvent event) {
            clear();
        }

        @Override
        public void allActionsCleared(EventObject event) {
            externalChange();
        }

        @Override
        public void redoActionsCleared(EventObject event) {
            externalChange();
        }

        @Override
        public void resetAll(EventObject event) {
            externalChange();
        }

        @Override
        public void enteredContext(UndoManagerEvent event) {
        }

        @Override
        public void enteredHiddenContext(UndoManagerEvent event) {
        }

        @Override
        public void leftContext(UndoManagerEvent event) {
        }

        @Override
        public void leftHiddenContext(UndoManagerEvent event) {
        }

        @Override
        public void cancelledContext(UndoManagerEvent event) {
        }

        @Override
        public void disposing(EventObject event) {
            closed = true;
            clear();
        }
    }
}
