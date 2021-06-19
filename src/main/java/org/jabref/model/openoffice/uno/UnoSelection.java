package org.jabref.model.openoffice.uno;

import java.util.Objects;
import java.util.Optional;

import com.sun.star.frame.XController;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XTextDocument;
import com.sun.star.view.XSelectionSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selection in the document.
 */
public class UnoSelection {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoSelection.class);

    private UnoSelection() { }

    private static Optional<XSelectionSupplier> getSelectionSupplier(XTextDocument doc) {
        if (doc == null) {
            LOGGER.warn("UnoSelection.getSelectionSupplier: doc is null");
            return Optional.empty();
        }
        Optional<XController> controller = UnoTextDocument.getCurrentController(doc);
        if (controller.isEmpty()) {
            LOGGER.warn("UnoSelection.getSelectionSupplier: getCurrentController(doc) returned empty");
            return Optional.empty();
        }
        Optional<XSelectionSupplier> supplier = UnoCast.cast(XSelectionSupplier.class, controller.get());
        if (supplier.isEmpty()) {
            LOGGER.warn("UnoSelection.getSelectionSupplier: cast to XSelectionSupplier returned empty");
            return Optional.empty();
        }
        return supplier;
    }

    /**
     * @return may be Optional.empty(), or some type supporting XServiceInfo
     *
     *
     * So far it seems the first thing we have to do
     * with a selection is to decide what do we have.
     *
     * One way to do that is accessing its XServiceInfo interface.
     *
     * Experiments using printServiceInfo with cursor in various
     * positions in the document:
     *
     * With cursor within the frame, in text:
     * *** xserviceinfo.getImplementationName: "SwXTextRanges"
     *      "com.sun.star.text.TextRanges"
     *
     * With cursor somewhere else in text:
     * *** xserviceinfo.getImplementationName: "SwXTextRanges"
     *      "com.sun.star.text.TextRanges"
     *
     * With cursor in comment (also known as "annotation"):
     * *** XSelectionSupplier is OK
     * *** Object initialSelection is null
     * *** xserviceinfo is null
     *
     * With frame selected:
     * *** xserviceinfo.getImplementationName: "SwXTextFrame"
     *     "com.sun.star.text.BaseFrame"
     *     "com.sun.star.text.TextContent"
     *     "com.sun.star.document.LinkTarget"
     *     "com.sun.star.text.TextFrame"
     *     "com.sun.star.text.Text"
     *
     * With cursor selecting an inserted image:
     * *** XSelectionSupplier is OK
     * *** Object initialSelection is OK
     * *** xserviceinfo is OK
     * *** xserviceinfo.getImplementationName: "SwXTextGraphicObject"
     *      "com.sun.star.text.BaseFrame"
     *      "com.sun.star.text.TextContent"
     *      "com.sun.star.document.LinkTarget"
     *      "com.sun.star.text.TextGraphicObject"
     */
    public static Optional<XServiceInfo> getSelectionAsXServiceInfo(XTextDocument doc) {
        Objects.requireNonNull(doc);
        Optional<XSelectionSupplier> supplier = getSelectionSupplier(doc);
        if (supplier.isEmpty()) {
            LOGGER.warn("getSelectionSupplier returned empty");
            return Optional.empty();
        }
        Object selection = supplier.get().getSelection();
        if (selection == null) {
            return Optional.empty();
        }
        Optional<XServiceInfo> result = UnoCast.cast(XServiceInfo.class, selection);
        if (result.isEmpty()) {
            LOGGER.warn("cast to XServiceInfo returned empty");
            return Optional.empty();
        }
        return result;
    }

    /**
     * Select the object represented by {@code newSelection} if it is
     * known and selectable in this {@code XSelectionSupplier} object.
     *
     * Presumably result from {@code XSelectionSupplier.getSelection()} is
     * usually OK. It also accepted
     * {@code XTextRange newSelection = doc.getText().getStart();}
     *
     */
    public static void select(XTextDocument doc, Object newSelection) {
        Objects.requireNonNull(doc);
        getSelectionSupplier(doc).ifPresent(e -> e.select(newSelection));
    }
}
