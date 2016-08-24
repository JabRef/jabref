package net.sf.jabref.gui.fieldeditors;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.TransferHandler;
import javax.swing.text.BadLocationException;

import net.sf.jabref.gui.EntryContainer;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PreviewPanelTransferHandler extends FileListEditorTransferHandler {

    private static final Log LOGGER = LogFactory.getLog(PreviewPanelTransferHandler.class);

    public PreviewPanelTransferHandler(JabRefFrame frame, EntryContainer entryContainer, TransferHandler textTransferHandler) {
        super(frame, entryContainer, textTransferHandler);
    }

    /**
     * LINK is unsupported as dropping into into Microsoft Word then leads to a link instead to a copy
     */
    @Override
    public int getSourceActions(JComponent component) {
        return DnDConstants.ACTION_COPY;
    }

    @Override
    protected Transferable createTransferable(JComponent component) {
        if (component instanceof JEditorPane) {
            // this method should be called from the preview panel only

            // the default TransferHandler implementation is aware of HTML
            // and returns an appropriate Transferable
            // as textTransferHandler.createTransferable() is not available and
            // I don't know any other method, I do the HTML conversion by hand

            // First, get the HTML of the selected text
            JEditorPane editorPane = (JEditorPane) component;
            StringWriter stringWriter = new StringWriter();
            try {
                editorPane.getEditorKit().write(stringWriter, editorPane.getDocument(), editorPane.getSelectionStart(), editorPane.getSelectionEnd());
            } catch (BadLocationException | IOException e) {
                LOGGER.warn("Cannot write preview", e);
            }

            // Second, return the HTML (and text as fallback)
            return new HtmlTransferable(stringWriter.toString(), editorPane.getSelectedText());
        } else {
            // if not called from the preview panel, return an error string
            return new StringSelection(Localization.lang("Operation not supported"));
        }
    }
}
