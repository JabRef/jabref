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

package org.jabref.gui.documentviewer;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.AbstractController;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.TaskExecutor;

public class DocumentViewerController extends AbstractController<DocumentViewerViewModel> {

    @FXML private ChoiceBox<String> fileChoice;
    @FXML private BorderPane mainPane;

    @Inject private StateManager stateManager;
    @Inject private TaskExecutor taskExecutor;

    @FXML
    private void initialize() {
        viewModel = new DocumentViewerViewModel(stateManager);

        setupViewer();

        fileChoice.itemsProperty().bind(viewModel.filesProperty());
        fileChoice.getSelectionModel().selectFirst();
    }

    private void setupViewer() {
        DocumentViewerControl viewer = new DocumentViewerControl(taskExecutor);
        viewModel.currentDocumentProperty().addListener((observable, oldDocument, newDocument) -> {
            if (newDocument != null) {
                viewer.show(newDocument);
            }
        });
        viewer.show(viewModel.currentDocumentProperty().get());
        mainPane.setCenter(viewer);
    }
}
