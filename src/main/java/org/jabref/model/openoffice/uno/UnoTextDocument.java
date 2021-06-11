package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnoTextDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoTextDocument.class);

    private UnoTextDocument() { }

    /**
     *  @return True if we cannot reach the current document.
     */
    public static boolean isDocumentConnectionMissing(XTextDocument doc) {

        boolean missing = false;
        if (doc == null) {
            missing = true;
        }

        // Attempt to check document is really available
        if (!missing) {
            try {
                UnoReferenceMark.getNameAccess(doc);
            } catch (NoDocumentException ex) {
                missing = true;
            } catch (com.sun.star.lang.DisposedException ex) {
                missing = true;
            }
        }
        return missing;
    }

    public static Optional<XController> getCurrentController(XTextDocument doc) {
        if (doc == null) {
            return Optional.empty();
        }
        XController controller = doc.getCurrentController();
        if (controller == null) {
            LOGGER.warn("doc.getCurrentController() returned null");
            return Optional.empty();
        }
        return Optional.of(controller);
    }

    /**
     *  @param doc The XTextDocument we want the frame title for. Null allowed.
     *  @return The title or Optional.empty()
     */
    public static Optional<String> getFrameTitle(XTextDocument doc) {

        Optional<XFrame> frame = getCurrentController(doc).map(XController::getFrame);
        if (frame.isEmpty()) {
            return Optional.empty();
        }

        Optional<XPropertySet> propertySet = UnoCast.cast(XPropertySet.class, frame.get());
        if (propertySet.isEmpty()) {
            return Optional.empty();
        }

        try {
            Optional<Object> frameTitleObj = UnoProperties.getValueAsObject(propertySet.get(), "Title");
            if (frameTitleObj.isEmpty()) {
                return Optional.empty();
            }
            String frameTitleString = String.valueOf(frameTitleObj.get());
            return Optional.ofNullable(frameTitleString);
        } catch (WrappedTargetException e) {
            LOGGER.warn("Could not get document title", e);
            return Optional.empty();
        }
    }

    static Optional<XDocumentProperties> getDocumentProperties(XTextDocument doc) {
        return (Optional.ofNullable(doc)
                .flatMap(e -> UnoCast.cast(XDocumentPropertiesSupplier.class, e))
                .map(XDocumentPropertiesSupplier::getDocumentProperties));
    }
}

