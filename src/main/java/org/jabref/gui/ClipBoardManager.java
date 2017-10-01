package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jabref.Globals;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fileformat.BibTeXMLImporter;
import org.jabref.logic.importer.fileformat.BiblioscapeImporter;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.fileformat.CopacImporter;
import org.jabref.logic.importer.fileformat.EndnoteImporter;
import org.jabref.logic.importer.fileformat.FreeCiteImporter;
import org.jabref.logic.importer.fileformat.GvkParser;
import org.jabref.logic.importer.fileformat.InspecImporter;
import org.jabref.logic.importer.fileformat.IsiImporter;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.importer.fileformat.MedlinePlainImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.importer.fileformat.MrDLibImporter;
import org.jabref.logic.importer.fileformat.MsBibImporter;
import org.jabref.logic.importer.fileformat.OvidImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.importer.fileformat.SilverPlatterImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClipBoardManager implements ClipboardOwner {
    private static final Log LOGGER = LogFactory.getLog(ClipBoardManager.class);

    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    private static final List<Function<ImportFormatPreferences, Parser>> parserSuppliers = Arrays.asList(ifps -> new BibtexParser(ifps), ifps -> new GvkParser());

    private static final List<Function<ImportFormatPreferences, Importer>> importerSuppliers = Arrays.asList(
            ifps -> new BiblioscapeImporter(),
            ifps -> new BibTeXMLImporter(),
            ifps -> new CopacImporter(),
            ifps -> new EndnoteImporter(ifps),
            ifps -> new InspecImporter(),
            ifps -> new IsiImporter(),
            ifps -> new MedlineImporter(),
            ifps -> new MedlinePlainImporter(),
            ifps -> new ModsImporter(),
            ifps -> new MrDLibImporter(),
            ifps -> new MsBibImporter(),
            ifps -> new OvidImporter(),
            // () -> new PdfContentImporter(Globals.prefs.getImportFormatPreferences()), PdfContentImporter does not support importDatabase(BufferedReader reader)
            // () -> new PdfXmpImporter(Globals.prefs.getXMPPreferences()), PdfXmpImporter does not support importDatabase(BufferedReader reader)
            ifps -> new RepecNepImporter(ifps),
            ifps -> new RisImporter(),
            ifps -> new SilverPlatterImporter(),
            ifps -> new FreeCiteImporter(ifps));

    private final ImportFormatPreferences ifps;

    public ClipBoardManager() {
        ifps = Globals.prefs.getImportFormatPreferences();
    }

    public ClipBoardManager(ImportFormatPreferences ifps) {
        this.ifps = ifps;
    }

    /**
     * Empty implementation of the ClipboardOwner interface.
     */
    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        //do nothing
    }

    /**
     * Places the string into the clipboard using a {@link Transferable}.
     */
    public void setTransferableClipboardContents(Transferable transferable) {
        CLIPBOARD.setContents(transferable, this);
    }

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an
     * empty String.
     */
    public String getClipboardContents() {
        String result = "";
        //odd: the Object param of getContents is not currently used
        Transferable contents = CLIPBOARD.getContents(null);
        if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                //highly unlikely since we are using a standard DataFlavor
                LOGGER.info("problem with getting clipboard contents", e);
            }
        }
        return result;
    }

    /**
     * Place a String on the clipboard, and make this class the
     * owner of the Clipboard's contents.
     */
    public void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        CLIPBOARD.setContents(stringSelection, this);
    }

    public List<BibEntry> extractBibEntriesFromClipboard() {
        // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
        Transferable content = CLIPBOARD.getContents(null);
        List<BibEntry> result = new ArrayList<>();

        if (content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)) {
            // We have determined that the clipboard data is a set of entries.
            try  {
                @SuppressWarnings("unchecked")
                List<BibEntry> contents = (List<BibEntry>) content.getTransferData(TransferableBibtexEntry.ENTRY_FLAVOR);
                result = contents;
            } catch (UnsupportedFlavorException | ClassCastException ex) {
                LOGGER.warn("Could not paste this type", ex);
            } catch (IOException ex) {
                LOGGER.warn("Could not paste", ex);
            }
        } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String data = (String) content.getTransferData(DataFlavor.stringFlavor);
                // fetch from doi
                if (DOI.parse(data).isPresent()) {
                    LOGGER.info("Found DOI in clipboard");
                    Optional<BibEntry> entry = new DoiFetcher(Globals.prefs.getImportFormatPreferences()).performSearchById(new DOI(data).getDOI());
                    entry.ifPresent(result::add);
                } else {
                    // try different parsers to import string
                    for (Function<ImportFormatPreferences, Parser> supplier : parserSuppliers) {
                        Parser parser = supplier.apply(ifps);
                        try (InputStream inputStream = IOUtils.toInputStream(data)) {
                            List<BibEntry> entries = parser.parseEntries(inputStream);
                            if ((entries != null) && (entries.size() > 0)) {
                                LOGGER.info("Parsed " + entries.size() + " entries from clipboard text using " + parser.getClass().getSimpleName());
                                result = entries;
                                break;
                            }
                        } catch (ParseException e) {
                            // parse failed for this parser, but others might be successful
                        }
                    }
                    if (result.isEmpty()) {
                        // try different importers to import string
                        for (Function<ImportFormatPreferences, Importer> supplier : importerSuppliers) {
                            Importer importer = supplier.apply(ifps);
                            BibDatabase db = null;
                            try (StringReader in = new StringReader(data); BufferedReader input = new BufferedReader(in)) {
                                db = importer.importDatabase(input).getDatabase();
                                if (db.hasEntries()) {
                                    BibEntry bibEntry = db.getEntries().get(0);
                                    if (!bibEntry.getField(FieldName.TITLE).isPresent() || !bibEntry.getField(FieldName.AUTHOR).isPresent()) {
                                        // this was most likely a false positive (see CopacImporter, IsiImporter)
                                        continue;
                                    }
                                    LOGGER.info("Parsed " + db.getEntries().size() + " entries from clipboard text using " + importer.getClass().getSimpleName());
                                    result = db.getEntries();
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (UnsupportedFlavorException ex) {
                LOGGER.warn("Could not parse this type", ex);
            } catch (IOException ex) {
                LOGGER.warn("Data is no longer available in the requested flavor", ex);
            } catch (FetcherException ex) {
                LOGGER.error("Error while fetching", ex);
            }

        }
        return result;
    }
}
