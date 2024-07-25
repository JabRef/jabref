package org.jabref.logic.openoffice.action;

import java.io.IOException;
import java.util.List;

import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.logic.openoffice.frontend.UpdateBibliography;
import org.jabref.logic.openoffice.frontend.UpdateCitationMarkers;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOProcess;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoScreenRefresh;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.tinylog.Logger;

/**
 * Update document: citation marks and bibliography
 */
public class Update {

    private Update() {
    }

    /**
     * @return the list of unresolved citation keys
     */
    private static List<String> updateDocument(XTextDocument doc,
                                               OOFrontend frontend,
                                               List<BibDatabase> databases,
                                               JStyle style,
                                               FunctionalTextViewCursor fcursor,
                                               boolean doUpdateBibliography,
                                               boolean alwaysAddCitedOnPages)
            throws
            CreationException,
            NoDocumentException,
            WrappedTargetException,
            IllegalArgumentException {

        final boolean useLockControllers = true;

        frontend.imposeGlobalOrder(doc, fcursor);
        OOProcess.produceCitationMarkers(frontend.citationGroups, databases, style);

        try {
            if (useLockControllers) {
                UnoScreenRefresh.lockControllers(doc);
            }

            UpdateCitationMarkers.applyNewCitationMarkers(doc, frontend, style);

            if (doUpdateBibliography) {
                UpdateBibliography.rebuildBibTextSection(doc,
                        frontend,
                        frontend.citationGroups.getBibliography().get(),
                        style,
                        alwaysAddCitedOnPages);
            }

            return frontend.citationGroups.getUnresolvedKeys();
        } catch (
                IOException e) {
            Logger.warn("Error while updating document", e);
        } finally {
            if (useLockControllers && UnoScreenRefresh.hasControllersLocked(doc)) {
                UnoScreenRefresh.unlockControllers(doc);
            }
        }
        return frontend.citationGroups.getUnresolvedKeys();
    }

    public static class SyncOptions {

        public final List<BibDatabase> databases;
        boolean updateBibliography;
        boolean alwaysAddCitedOnPages;

        public SyncOptions(List<BibDatabase> databases) {
            this.databases = databases;
            this.updateBibliography = false;
            this.alwaysAddCitedOnPages = false;
        }

        public SyncOptions setUpdateBibliography(boolean value) {
            this.updateBibliography = value;
            return this;
        }

        public SyncOptions setAlwaysAddCitedOnPages(boolean value) {
            this.alwaysAddCitedOnPages = value;
            return this;
        }
    }

    public static List<String> synchronizeDocument(XTextDocument doc,
                                                   OOFrontend frontend,
                                                   JStyle style,
                                                   FunctionalTextViewCursor fcursor,
                                                   SyncOptions syncOptions)
            throws
            CreationException,
            NoDocumentException,
            WrappedTargetException,
            IllegalArgumentException {

        return Update.updateDocument(doc,
                frontend,
                syncOptions.databases,
                style,
                fcursor,
                syncOptions.updateBibliography,
                syncOptions.alwaysAddCitedOnPages);
    }

    /**
     * Reread document before sync
     */
    public static List<String> resyncDocument(XTextDocument doc,
                                              JStyle style,
                                              FunctionalTextViewCursor fcursor,
                                              SyncOptions syncOptions)
            throws
            CreationException,
            NoDocumentException,
            WrappedTargetException,
            IllegalArgumentException {

        OOFrontend frontend = new OOFrontend(doc);

        return Update.synchronizeDocument(doc, frontend, style, fcursor, syncOptions);
    }
}
