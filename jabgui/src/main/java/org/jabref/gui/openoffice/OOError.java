package org.jabref.gui.openoffice;

import org.jabref.gui.DialogService;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.NoDocumentFoundException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.lang.DisposedException;

class OOError extends JabRefException {

    private String localizedTitle;

    public OOError(String title, String localizedMessage) {
        super(localizedMessage, localizedMessage);
        this.localizedTitle = title;
    }

    public OOError(String title, String localizedMessage, Throwable cause) {
        super(localizedMessage, localizedMessage, cause);
        this.localizedTitle = title;
    }

    public String getTitle() {
        return localizedTitle;
    }

    public OOError setTitle(String title) {
        localizedTitle = title;
        return this;
    }

    public void showErrorDialog(DialogService dialogService) {
        dialogService.showErrorDialogAndWait(getTitle(), getLocalizedMessage());
    }

    /*
     * Conversions from exception caught
     */

    public static OOError from(JabRefException err) {
        return new OOError(
                Localization.lang("JabRefException"),
                err.getLocalizedMessage(),
                err);
    }

    // For DisposedException
    public static OOError from(DisposedException err) {
        return new OOError(
                Localization.lang("Connection lost"),
                Localization.lang("Connection to OpenOffice/LibreOffice has been lost."
                        + " Please make sure OpenOffice/LibreOffice is running,"
                        + " and try to reconnect."),
                err);
    }

    // For NoDocumentException
    public static OOError from(NoDocumentException err) {
        return new OOError(
                Localization.lang("Not connected to document"),
                Localization.lang("Not connected to any Writer document."
                        + " Please make sure a document is open,"
                        + " and use the 'Select Writer document' button"
                        + " to connect to it."),
                err);
    }

    // For NoDocumentFoundException
    public static OOError from(NoDocumentFoundException err) {
        return new OOError(
                Localization.lang("No Writer documents found"),
                Localization.lang("Could not connect to any Writer document."
                        + " Please make sure a document is open"
                        + " before using the 'Select Writer document' button"
                        + " to connect to it."),
                err);
    }

    public static OOError fromMisc(Exception err) {
        return new OOError(
                "Exception",
                err.getMessage(),
                err);
    }

    /*
     * Messages for error dialog. These are not thrown.
     */

    // noDataBaseIsOpenForCiting
    public static OOError noDataBaseIsOpenForCiting() {
        return new OOError(
                Localization.lang("No database"),
                Localization.lang("No library is open for citation")
                        + "\n"
                        + Localization.lang("Please open one before citing."));
    }

    public static OOError noDataBaseIsOpenForSyncingAfterCitation() {
        return new OOError(
                Localization.lang("No database"),
                Localization.lang("No library is open for updating citation markers after citing.")
                        + "\n"
                        + Localization.lang("Please open one before citing."));
    }

    // noDataBaseIsOpenForExport
    public static OOError noDataBaseIsOpenForExport() {
        return new OOError(
                Localization.lang("No database is open"),
                Localization.lang("Please open a library before exporting."));
    }

    // noDataBaseIsOpenForExport
    public static OOError noDataBaseIsOpen() {
        return new OOError(
                Localization.lang("No database is open"),
                Localization.lang("This operation requires an open library."));
    }

    // noValidStyleSelected
    public static OOError noValidStyleSelected() {
        return new OOError(Localization.lang("No valid style file defined"),
                Localization.lang("No citation style is selected for citation.")
                        + "\n"
                        + Localization.lang("Please select one before citing.")
                        + "\n"
                        + Localization.lang("You must select either a valid style file,"
                        + " or use one of the default styles."));
    }

    // noEntriesSelectedForCitation
    public static OOError noEntriesSelectedForCitation() {
        return new OOError(Localization.lang("No entries selected for citation"),
                Localization.lang("No library entries are selected for citation.")
                        + "\n"
                        + Localization.lang("Please select some from the library before citing."));
    }
}
