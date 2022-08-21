package org.jabref.gui.openoffice;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.NoDocumentFoundException;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoCast;
import org.jabref.model.openoffice.uno.UnoTextDocument;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sun.star.uno.UnoRuntime.queryInterface;

/**
 * Establish connection to a document opened in OpenOffice or LibreOffice.
 */
public class OOBibBaseConnect {

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibBaseConnect.class);

    private final DialogService dialogService;
    private final XDesktop xDesktop;

    /**
     * Created when connected to a document.
     * <p>
     * Cleared (to null) when we discover we lost the connection.
     */
    private XTextDocument xTextDocument;

    public OOBibBaseConnect(Path loPath, DialogService dialogService)
            throws
            BootstrapException,
            CreationException {

        this.dialogService = dialogService;
        this.xDesktop = simpleBootstrap(loPath);
    }

    private XDesktop simpleBootstrap(Path loPath)
            throws
            CreationException,
            BootstrapException {

        // Get the office component context:
        XComponentContext context = org.jabref.gui.openoffice.Bootstrap.bootstrap(loPath);
        XMultiComponentFactory sem = context.getServiceManager();

        // Create the desktop, which is the root frame of the
        // hierarchy of frames that contain viewable components:
        Object desktop;
        try {
            desktop = sem.createInstanceWithContext("com.sun.star.frame.Desktop", context);
        } catch (com.sun.star.uno.Exception e) {
            throw new CreationException(e.getMessage());
        }
        return UnoCast.cast(XDesktop.class, desktop).get();
    }

    /**
     *  Close any open office connection, if none exists does nothing
     */
    public static void closeOfficeConnection() {
        try {
            // get the bridge factory from the local service manager
            XBridgeFactory bridgeFactory = queryInterface(XBridgeFactory.class,
                                                          org.jabref.gui.openoffice.Bootstrap.createSimpleServiceManager()
                    .createInstance("com.sun.star.bridge.BridgeFactory"));

            if (bridgeFactory != null) {
                for (XBridge bridge : bridgeFactory.getExistingBridges()) {
                    // dispose of this bridge after closing its connection
                    queryInterface(XComponent.class, bridge).dispose();
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Exception disposing office process connection bridge:", ex);
        }
    }

    private static List<XTextDocument> getTextDocuments(XDesktop desktop)
            throws
            NoSuchElementException,
            WrappedTargetException {

        List<XTextDocument> result = new ArrayList<>();

        XEnumerationAccess enumAccess = desktop.getComponents();
        XEnumeration compEnum = enumAccess.createEnumeration();

        while (compEnum.hasMoreElements()) {
            Object next = compEnum.nextElement();
            XComponent comp = UnoCast.cast(XComponent.class, next).get();
            Optional<XTextDocument> doc = UnoCast.cast(XTextDocument.class, comp);
            doc.ifPresent(result::add);
        }
        return result;
    }

    /**
     * Run a dialog allowing the user to choose among the documents in `list`.
     *
     * @return Null if no document was selected. Otherwise the document selected.
     */
    private static XTextDocument selectDocumentDialog(List<XTextDocument> list,
                                                      DialogService dialogService) {

        class DocumentTitleViewModel {

            private final XTextDocument xTextDocument;
            private final String description;

            public DocumentTitleViewModel(XTextDocument xTextDocument) {
                this.xTextDocument = xTextDocument;
                this.description = UnoTextDocument.getFrameTitle(xTextDocument).orElse("");
            }

            public XTextDocument getXtextDocument() {
                return xTextDocument;
            }

            @Override
            public String toString() {
                return description;
            }
        }

        List<DocumentTitleViewModel> viewModel = (list.stream()
                                                      .map(DocumentTitleViewModel::new)
                                                      .collect(Collectors.toList()));

        // This whole method is part of a background task when
        // auto-detecting instances, so we need to show dialog in FX
        // thread
        Optional<DocumentTitleViewModel> selectedDocument =
                dialogService
                        .showChoiceDialogAndWait(Localization.lang("Select document"),
                                Localization.lang("Found documents:"),
                                Localization.lang("Use selected document"),
                                viewModel);

        return selectedDocument
                .map(DocumentTitleViewModel::getXtextDocument)
                .orElse(null);
    }

    /**
     * Choose a document to work with.
     * <p>
     * Assumes we have already connected to LibreOffice or OpenOffice.
     * <p>
     * If there is a single document to choose from, selects that. If there are more than one, shows selection dialog. If there are none, throws NoDocumentFoundException
     * <p>
     * After successful selection connects to the selected document and extracts some frequently used parts (starting points for managing its content).
     * <p>
     * Finally initializes this.xTextDocument with the selected document and parts extracted.
     */
    public void selectDocument(boolean autoSelectForSingle)
            throws
            NoDocumentFoundException,
            NoSuchElementException,
            WrappedTargetException {

        XTextDocument selected;
        List<XTextDocument> textDocumentList = getTextDocuments(this.xDesktop);
        if (textDocumentList.isEmpty()) {
            throw new NoDocumentFoundException("No Writer documents found");
        } else if ((textDocumentList.size() == 1) && autoSelectForSingle) {
            selected = textDocumentList.get(0); // Get the only one
        } else { // Bring up a dialog
            selected = OOBibBaseConnect.selectDocumentDialog(textDocumentList,
                    this.dialogService);
        }

        if (selected == null) {
            return;
        }

        this.xTextDocument = selected;
    }

    /**
     * Mark the current document as missing.
     */
    private void forgetDocument() {
        this.xTextDocument = null;
    }

    /**
     * A simple test for document availability.
     * <p>
     * See also `isDocumentConnectionMissing` for a test actually attempting to use teh connection.
     */
    public boolean isConnectedToDocument() {
        return this.xTextDocument != null;
    }

    /**
     * @return true if we are connected to a document
     */
    public boolean isDocumentConnectionMissing() {
        XTextDocument doc = this.xTextDocument;

        if (doc == null) {
            return true;
        }

        if (UnoTextDocument.isDocumentConnectionMissing(doc)) {
            forgetDocument();
            return true;
        }
        return false;
    }

    /**
     * Either return a valid XTextDocument or throw NoDocumentException.
     */
    public XTextDocument getXTextDocumentOrThrow()
            throws
            NoDocumentException {
        if (isDocumentConnectionMissing()) {
            throw new NoDocumentException("Not connected to document");
        }
        return this.xTextDocument;
    }

    public OOResult<XTextDocument, OOError> getXTextDocument() {
        if (isDocumentConnectionMissing()) {
            return OOResult.error(OOError.from(new NoDocumentException()));
        }
        return OOResult.ok(this.xTextDocument);
    }

    /**
     * The title of the current document, or Optional.empty()
     */
    public Optional<String> getCurrentDocumentTitle() {
        if (isDocumentConnectionMissing()) {
            return Optional.empty();
        } else {
            return UnoTextDocument.getFrameTitle(this.xTextDocument);
        }
    }
}
