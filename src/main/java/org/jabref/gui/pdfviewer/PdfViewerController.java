/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jabref.gui.pdfviewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Pagination;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.jabref.Globals;
import org.jabref.gui.AbstractController;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PdfViewerController extends AbstractController<PdfViewerController.PdfDocumentViewModel> {

    @FXML private Pagination pagination;

    @Inject private StateManager stateManager;

    private ObjectProperty<PdfDocumentViewModel> currentDocument;

    @FXML
    private void initialize() {
        currentDocument = new SimpleObjectProperty<>();
        currentDocument.addListener((observable, oldDocument, newDocument) -> {
            if (newDocument != null) {
                pagination.setCurrentPageIndex(0);
            }
        });
        pagination.pageCountProperty().bind(new IntegerBinding() {
            {
                super.bind(currentDocument);
            }
            @Override
            protected int computeValue() {
                return currentDocument.get() == null ? 0 : currentDocument.get().getNumberOfPages();
            }
        });
        pagination.disableProperty().bind(Bindings.isNull(currentDocument));

        pagination.setPageFactory(pageNumber -> currentDocument.get() == null ? null : new ImageView(currentDocument.get().getImage(pageNumber)));

        stateManager.getSelectedEntries().addListener((ListChangeListener<? super BibEntry>) c -> setCurrentEntries(stateManager.getSelectedEntries()));
        setCurrentEntries(stateManager.getSelectedEntries());
    }

    private void setCurrentEntries(List<BibEntry> entries) {
        if (!entries.isEmpty()) {
            BibEntry firstSelectedEntry = entries.get(0);
            setCurrentEntry(firstSelectedEntry);
        }
    }

    private void setCurrentEntry(BibEntry rawEntry) {
        BibDatabaseContext databaseContext = stateManager.activeDatabaseProperty().get().get();
        TypedBibEntry entry = new TypedBibEntry(rawEntry, databaseContext);
        List<ParsedFileField> linkedFiles = entry.getFiles();
        for (ParsedFileField linkedFile : linkedFiles) {
            // TODO: Find a better way to get the open database
            // TODO: It should be possible to simply write linkedFile.getFile()
            Optional<File> file = FileUtil.expandFilename(
                    databaseContext, linkedFile.getLink(), Globals.prefs.getFileDirectoryPreferences());
            if (file.isPresent()) {
                setCurrentDocument(file.get().toPath());
            }
        }
    }

    private void setCurrentDocument(Path path) {
        try {
            currentDocument.set(new PdfDocumentViewModel(PDDocument.load(path.toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class PdfDocumentViewModel extends AbstractViewModel {

        private final PDDocument document;

        public PdfDocumentViewModel(PDDocument document) {
            this.document = Objects.requireNonNull(document);
        }

        public int getNumberOfPages() {
            return document.getNumberOfPages();
        }

        public Image getImage(int pageNumber) {
            if (pageNumber <= 0 || pageNumber > document.getNumberOfPages()) {
                return null;
            }

            PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(pageNumber - 1);
            try {
                BufferedImage image = page.convertToImage();
                return SwingFXUtils.toFXImage(resize(image, 600, 800), null);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        // Taken from http://stackoverflow.com/a/9417836/873661
        private BufferedImage resize(BufferedImage img, int newW, int newH) {
            java.awt.Image tmp = img.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
            BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = dimg.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();

            return dimg;
        }
    }
}
