package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.document.EmptyUndoStackException;
import com.sun.star.document.UndoContextNotClosedException;
import com.sun.star.document.UndoFailedException;
import com.sun.star.document.UndoManagerEvent;
import com.sun.star.document.XUndoAction;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerListener;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.util.InvalidStateException;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Groups a prepared document mutation into one undo action. Visual ordering and rendering happen
/// before the controller lock, because Writer's view cursor cannot move while controllers are locked.
@NullMarked
public final class DocumentSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSession.class);
    private final XTextDocument document;

    public DocumentSession(XTextDocument document) {
        this.document = document;
    }

    public void requireRollbackSupport() throws JabRefException {
        rollbackManager();
    }

    private XUndoManager rollbackManager() throws JabRefException {
        Optional<XUndoManager> manager = UnoUndo.getXUndoManager(document).filter(undo -> !undo.isLocked());
        if (manager.isEmpty()) {
            throw new JabRefException("Conversion requires an unlocked Writer undo manager",
                    Localization.lang("Cannot convert citations without an unlocked undo manager. Enable undo in Writer and try again."));
        }
        return manager.orElseThrow();
    }

    /// Conversion owns a root undo context with a sentinel even if the first real write fails.
    /// Only a successfully closed, still-owned context can be rolled back, never an older user action.
    public <T> OOResult<T, JabRefException> writeWithRollback(String title, Supplier<OOResult<T, JabRefException>> action) {
        XUndoManager manager;
        try {
            manager = rollbackManager();
        } catch (JabRefException exception) {
            return OOResult.error(exception);
        }
        TransactionBoundary boundary = new TransactionBoundary(title);
        manager.addUndoManagerListener(boundary);
        List<OOResult<T, JabRefException>> results = new ArrayList<>(1);
        boolean entered = false;
        boolean registered = false;
        try {
            try {
                manager.enterUndoContext(title);
                entered = true;
                if (boundary.depth != 1) {
                    return OOResult.error(new JabRefException("Conversion cannot join another undo context",
                            Localization.lang("Cannot convert citations inside another undo operation. Finish the current Writer operation and try again.")));
                }
                manager.addUndoAction(boundary);
                registered = true;
                UnoRedlines.withRecordChangesSuspended(document, () -> {
                    document.lockControllers();
                    try {
                        results.add(action.get());
                    } finally {
                        document.unlockControllers();
                    }
                });
            } catch (WrappedTargetException | UnoRedlines.TrackChangesRestoreException | RuntimeException exception) {
                LOGGER.warn("Citation conversion transaction failed", exception);
                results.clear();
                results.add(OOResult.error(new JabRefException("Citation conversion transaction failed", Localization.lang("Could not insert citation"), exception)));
            } finally {
                if (entered) {
                    try {
                        manager.leaveUndoContext();
                    } catch (InvalidStateException | RuntimeException exception) {
                        boundary.invalidated = true;
                        LOGGER.error("Could not close citation conversion undo context", exception);
                        results.clear();
                        results.add(OOResult.error(rollbackFailure(exception)));
                    }
                }
            }
            OOResult<T, JabRefException> result = results.getFirst();
            if (result.isError() && registered) {
                try {
                    if (!boundary.closed || boundary.invalidated || !manager.isUndoPossible() || !manager.getCurrentUndoActionTitle().equals(title)) {
                        LOGGER.error("Cannot establish conversion rollback ownership: entry depth {}, exit depth {}, closed {}, invalidated {}, undo possible {}",
                                boundary.depth, boundary.exitDepth, boundary.closed, boundary.invalidated, manager.isUndoPossible());
                        return OOResult.error(rollbackFailure(result.getError()));
                    }
                    UnoRedlines.withRecordChangesSuspended(document, () -> {
                        document.lockControllers();
                        try {
                            manager.undo();
                            if (!boundary.undone) {
                                throw new UndoFailedException("Conversion undo sentinel was not replayed");
                            }
                            manager.clearRedo();
                        } catch (EmptyUndoStackException | UndoContextNotClosedException | UndoFailedException exception) {
                            throw new WrappedTargetException(exception, "Could not roll back citation conversion", document, exception);
                        } finally {
                            document.unlockControllers();
                        }
                    });
                    if (hasSecondaryFailure(result.getError())) {
                        return OOResult.error(rollbackFailure(result.getError()));
                    }
                } catch (EmptyUndoStackException | WrappedTargetException | UnoRedlines.TrackChangesRestoreException | RuntimeException exception) {
                    exception.addSuppressed(result.getError());
                    return OOResult.error(rollbackFailure(exception));
                }
            }
            return result;
        } finally {
            manager.removeUndoManagerListener(boundary);
        }
    }

    private static boolean hasSecondaryFailure(Throwable failure) {
        // Failed property compensation or Track Changes restoration can leave state outside UNO's undo log.
        return failure instanceof UnoRedlines.TrackChangesRestoreException || failure.getSuppressed().length > 0
                || Optional.ofNullable(failure.getCause()).map(DocumentSession::hasSecondaryFailure).orElse(false);
    }

    private JabRefException rollbackFailure(Throwable cause) {
        LOGGER.error("Citation conversion rollback failed; document recovery may be required", cause);
        return new JabRefException("Citation conversion rollback failed",
                Localization.lang("Citation conversion failed and could not be rolled back completely. Check the document and restore a saved copy if necessary."), cause);
    }

    @NullMarked
    private static final class TransactionBoundary extends WeakBase implements XUndoAction, XUndoManagerListener {
        private final String title;
        private int depth = -1;
        private int exitDepth = -1;
        private boolean closed;
        private boolean invalidated;
        private boolean undone;

        private TransactionBoundary(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public void undo() {
            undone = true;
        }

        @Override
        public void redo() {
            undone = false;
        }

        @Override
        public void enteredContext(UndoManagerEvent event) {
            if (depth == -1) {
                depth = event.UndoContextDepth;
            }
        }

        @Override
        public void leftContext(UndoManagerEvent event) {
            exitDepth = event.UndoContextDepth;
            if (event.UndoContextDepth == 0) {
                closed = true;
            }
        }

        @Override
        public void allActionsCleared(EventObject event) {
            invalidated = true;
        }

        @Override
        public void resetAll(EventObject event) {
            invalidated = true;
        }

        @Override
        public void cancelledContext(UndoManagerEvent event) {
            // Writer cancels empty nested formatting contexts during reference-mark replacement.
            if (event.UndoContextDepth == 0) {
                invalidated = true;
            }
        }

        @Override
        public void undoActionAdded(UndoManagerEvent event) {
        }

        @Override
        public void actionUndone(UndoManagerEvent event) {
        }

        @Override
        public void actionRedone(UndoManagerEvent event) {
        }

        @Override
        public void redoActionsCleared(EventObject event) {
        }

        @Override
        public void enteredHiddenContext(UndoManagerEvent event) {
        }

        @Override
        public void leftHiddenContext(UndoManagerEvent event) {
        }

        @Override
        public void disposing(EventObject event) {
            invalidated = true;
        }
    }

    public <T> OOResult<T, JabRefException> write(String title, Supplier<OOResult<T, JabRefException>> action) {
        List<OOResult<T, JabRefException>> result = new ArrayList<>(1);
        UnoUndo.enterUndoContext(document, title);
        try {
            UnoRedlines.withRecordChangesSuspended(document, () -> {
                document.lockControllers();
                try {
                    result.add(action.get());
                } finally {
                    document.unlockControllers();
                }
            });
            return result.getFirst();
        } catch (UnoRedlines.TrackChangesRestoreException exception) {
            LOGGER.warn("Could not restore change recording after updating citations", exception);
            return OOResult.error(new JabRefException("Could not restore change recording",
                    Localization.lang("JabRef updated the document, but could not restore Track Changes. Please verify [Edit]/[Track Changes]/[Record]."), exception));
        } catch (WrappedTargetException exception) {
            LOGGER.warn("Could not suspend change recording", exception);
            return OOResult.error(new JabRefException(exception));
        } finally {
            UnoUndo.leaveUndoContext(document);
        }
    }
}
