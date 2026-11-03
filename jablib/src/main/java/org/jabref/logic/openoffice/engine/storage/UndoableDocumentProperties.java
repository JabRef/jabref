package org.jabref.logic.openoffice.engine.storage;

import java.util.Map;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.uno.UnoCast;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.document.UndoFailedException;
import com.sun.star.document.XUndoAction;
import com.sun.star.document.XUndoManager;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Type;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Writer does not undo custom-property edits itself. Each property change therefore contributes
/// its own delta to the caller's current undo context, alongside the corresponding mark/text edits.
@NullMarked
public final class UndoableDocumentProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableDocumentProperties.class);

    private UndoableDocumentProperties() {
    }

    /// Validate every conversion delta without coercing typed properties to strings or writing them.
    public static void preflight(XTextDocument document, Map<String, Optional<String>> changes)
            throws WrappedTargetException, IllegalTypeException, PropertyVetoException, NotRemoveableException {
        XPropertySet properties = UnoUserDefinedProperty.getPropertyContainer(document).flatMap(container -> UnoCast.cast(XPropertySet.class, container))
                                                        .orElseThrow(() -> new WrappedTargetException("Cannot inspect document properties"));
        XPropertySetInfo information = properties.getPropertySetInfo();
        for (Map.Entry<String, Optional<String>> change : changes.entrySet()) {
            if (!information.hasPropertyByName(change.getKey())) {
                continue;
            }
            try {
                Property property = information.getPropertyByName(change.getKey());
                if (!Type.STRING.equals(property.Type) || !(properties.getPropertyValue(change.getKey()) instanceof String)) {
                    throw new IllegalTypeException("Citation property must be a string: " + change.getKey(), document);
                }
                if ((property.Attributes & PropertyAttribute.READONLY) != 0) {
                    throw new PropertyVetoException("Citation property is read-only: " + change.getKey(), document);
                }
                if (change.getValue().isEmpty() && (property.Attributes & PropertyAttribute.REMOVEABLE) == 0) {
                    throw new NotRemoveableException("Citation property is not removable: " + change.getKey(), document);
                }
            } catch (UnknownPropertyException exception) {
                LOGGER.warn("Citation property disappeared during preflight", exception);
                throw new WrappedTargetException(exception, "Citation property disappeared during preflight", document, exception);
            }
        }
    }

    public static void set(XTextDocument document, String name, Optional<String> value)
            throws WrappedTargetException, NotRemoveableException, IllegalTypeException, PropertyVetoException {
        Optional<String> before = UnoUserDefinedProperty.getStringValue(document, name);
        if (before.equals(value)) {
            return;
        }
        restore(document, name, value);
        try {
            register(document, name, before, value);
        } catch (WrappedTargetException | RuntimeException exception) {
            throw compensateRegistrationFailure(document, name, before, exception);
        }
    }

    public static void remove(XTextDocument document, String name) throws WrappedTargetException, NotRemoveableException {
        Optional<String> before = UnoUserDefinedProperty.getStringValue(document, name);
        if (before.isEmpty()) {
            return;
        }
        UnoUserDefinedProperty.removeIfExists(document, name);
        try {
            register(document, name, before, Optional.empty());
        } catch (WrappedTargetException | RuntimeException exception) {
            throw compensateRegistrationFailure(document, name, before, exception);
        }
    }

    private static WrappedTargetException compensateRegistrationFailure(XTextDocument document, String name, Optional<String> before, Throwable cause) {
        WrappedTargetException failure = cause instanceof WrappedTargetException wrapped ? wrapped
                                                                                         : new WrappedTargetException(cause, "Could not register citation property undo", document, cause);
        try {
            restore(document, name, before);
        } catch (NotRemoveableException | WrappedTargetException | PropertyVetoException | IllegalTypeException | RuntimeException restoreFailure) {
            LOGGER.error("Could not compensate citation property {} after undo registration failed", name, restoreFailure);
            failure.addSuppressed(restoreFailure);
        }
        LOGGER.warn("Citation property undo registration failed; attempted immediate compensation for {}", name, failure);
        return failure;
    }

    private static void register(XTextDocument document, String name, Optional<String> before, Optional<String> after)
            throws WrappedTargetException {
        Optional<XUndoManager> manager = UnoUndo.getXUndoManager(document).filter(undo -> !undo.isLocked());
        if (manager.isEmpty()) {
            return;
        }
        try {
            manager.orElseThrow().addUndoAction(new PropertyUndoAction(document, name, before, after));
        } catch (com.sun.star.lang.IllegalArgumentException exception) {
            LOGGER.warn("Could not register undo for citation property {}", name, exception);
            throw new WrappedTargetException(exception, "Could not register citation property undo", document, exception);
        }
    }

    private static void restore(XTextDocument document, String name, Optional<String> value)
            throws NotRemoveableException, WrappedTargetException, PropertyVetoException, IllegalTypeException {
        if (value.isPresent()) {
            UnoUserDefinedProperty.setStringProperty(document, name, value.orElseThrow());
        } else {
            UnoUserDefinedProperty.removeIfExists(document, name);
        }
    }

    @NullMarked
    private static final class PropertyUndoAction extends WeakBase implements XUndoAction {
        private final XTextDocument document;
        private final String name;
        private final Optional<String> before;
        private final Optional<String> after;

        private PropertyUndoAction(XTextDocument document, String name, Optional<String> before, Optional<String> after) {
            this.document = document;
            this.name = name;
            this.before = before;
            this.after = after;
        }

        @Override
        public String getTitle() {
            return Localization.lang("Manage citations");
        }

        @Override
        public void undo() throws UndoFailedException {
            replay(before);
        }

        @Override
        public void redo() throws UndoFailedException {
            replay(after);
        }

        private void replay(Optional<String> value) throws UndoFailedException {
            try {
                // Use the raw property API: replay must not register another undo action.
                restore(document, name, value);
            } catch (NotRemoveableException | WrappedTargetException | PropertyVetoException | IllegalTypeException exception) {
                LOGGER.warn("Could not restore citation property {}", name, exception);
                throw new UndoFailedException(exception, "Could not restore citation properties", this, exception);
            }
        }
    }
}
