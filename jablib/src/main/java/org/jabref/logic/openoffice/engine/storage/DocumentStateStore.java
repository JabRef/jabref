package org.jabref.logic.openoffice.engine.storage;

import java.util.Optional;

import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Persists the last successfully applied JabRef style independently of Zotero's document preferences.
@NullMarked
public final class DocumentStateStore {

    public static final String PROPERTY_NAME = "JabRef_document_style";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStateStore.class);

    @NullMarked
    public enum Family {
        BST,
        CSL,
        JSTYLE
    }

    @NullMarked
    public record State(Family family, String styleId) {
    }

    public Optional<State> read(XTextDocument document) throws WrappedTargetException {
        return UnoUserDefinedProperty.getStringValue(document, PROPERTY_NAME).flatMap(value -> {
            int separator = value.indexOf('\n');
            if (separator < 0) {
                LOGGER.warn("Ignoring malformed JabRef document style state");
                return Optional.empty();
            }
            try {
                return Optional.of(new State(Family.valueOf(value.substring(0, separator)), value.substring(separator + 1)));
            } catch (IllegalArgumentException exception) {
                LOGGER.debug("Ignoring unknown JabRef document style family", exception);
                return Optional.empty();
            }
        });
    }

    public void store(XTextDocument document, State state) throws IllegalTypeException, PropertyVetoException, WrappedTargetException {
        UnoUserDefinedProperty.setStringProperty(document, PROPERTY_NAME, state.family().name() + "\n" + state.styleId());
    }
}
