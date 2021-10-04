package org.jabref.gui.preferences.preview;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class PreviewTab extends AbstractPreferenceTabView<PreviewTabViewModel> implements PreferencesTab {

    @FXML private CheckBox showAsTabCheckBox;
    @FXML private ListView<PreviewLayout> availableListView;
    @FXML private ListView<PreviewLayout> chosenListView;
    @FXML private Button toRightButton;
    @FXML private Button toLeftButton;
    @FXML private Button sortUpButton;
    @FXML private Button sortDownButton;
    @FXML private Label readOnlyLabel;
    @FXML private Button resetDefaultButton;
    @FXML private Tab previewTab;
    @FXML private CodeArea editArea;
    @FXML private CustomTextField searchBox;

    @Inject private StateManager stateManager;

    private final ContextMenu contextMenu = new ContextMenu();

    private long lastKeyPressTime;
    private String listSearchTerm;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public PreviewTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    private class EditAction extends SimpleCommand {

        private final StandardActions command;

        public EditAction(StandardActions command) {
            this.command = command;
        }

        @Override
        public void execute() {
            if (editArea != null) {
                switch (command) {
                    case COPY -> editArea.copy();
                    case CUT -> editArea.cut();
                    case PASTE -> editArea.paste();
                    case SELECT_ALL -> editArea.selectAll();
                }
                editArea.requestFocus();
            }
        }
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry preview");
    }

    public void initialize() {
        searchBox.setPromptText(Localization.lang("Search") + "...");
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());

        this.viewModel = new PreviewTabViewModel(dialogService, preferencesService, taskExecutor, stateManager);

        lastKeyPressTime = System.currentTimeMillis();

        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT)),
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY)),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE)),
                factory.createMenuItem(StandardActions.SELECT_ALL, new EditAction(StandardActions.SELECT_ALL))
        );
        contextMenu.getItems().forEach(item -> item.setGraphic(null));
        contextMenu.getStyleClass().add("context-menu");

        showAsTabCheckBox.selectedProperty().bindBidirectional(viewModel.showAsExtraTabProperty());

        availableListView.setItems(viewModel.getFilteredPreviews());
        viewModel.availableSelectionModelProperty().setValue(availableListView.getSelectionModel());
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getDisplayName)
                .install(availableListView);
        availableListView.setOnDragOver(this::dragOver);
        availableListView.setOnDragDetected(this::dragDetectedInAvailable);
        availableListView.setOnDragDropped(event -> dragDropped(viewModel.availableListProperty(), event));
        availableListView.setOnKeyTyped(event -> jumpToSearchKey(availableListView, event));
        availableListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableListView.selectionModelProperty().getValue().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.setPreviewLayout(newValue));

        chosenListView.itemsProperty().bindBidirectional(viewModel.chosenListProperty());
        viewModel.chosenSelectionModelProperty().setValue(chosenListView.getSelectionModel());
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getDisplayName)
                .setOnDragDropped(this::dragDroppedInChosenCell)
                .install(chosenListView);
        chosenListView.setOnDragOver(this::dragOver);
        chosenListView.setOnDragDetected(this::dragDetectedInChosen);
        chosenListView.setOnDragDropped(event -> dragDropped(viewModel.chosenListProperty(), event));
        chosenListView.setOnKeyTyped(event -> jumpToSearchKey(chosenListView, event));
        chosenListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chosenListView.selectionModelProperty().getValue().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.setPreviewLayout(newValue));

        toRightButton.disableProperty().bind(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNull());

        toLeftButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortUpButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortDownButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());

        previewTab.setContent(new PreviewViewer(new BibDatabaseContext(), dialogService, stateManager));
        ((PreviewViewer) previewTab.getContent()).setEntry(TestEntry.getTestEntry());

        EasyBind.subscribe(viewModel.layoutProperty(), value -> ((PreviewViewer) previewTab.getContent()).setLayout(value));
        previewTab.getContent().visibleProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull()
                                                       .or(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNotNull()));
        ((PreviewViewer) previewTab.getContent()).setTheme(preferencesService.getTheme());

        editArea.clear();
        editArea.setParagraphGraphicFactory(LineNumberFactory.get(editArea));
        editArea.setContextMenu(contextMenu);
        editArea.visibleProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull()
                                        .or(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNotNull()));
        viewModel.sourceTextProperty().addListener((observable, oldValue, newValue) -> {
            editArea.replaceText(newValue);
        });
        editArea.textProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.sourceTextProperty().setValue(newValue);
            editArea.setStyleSpans(0, viewModel.computeHighlighting(newValue));
        });
        editArea.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                viewModel.refreshPreview();
            }
        });

        searchBox.textProperty().addListener((observable, previousText, searchTerm) -> {
            viewModel.setFilterPredicate(searchTerm);
        });

        readOnlyLabel.visibleProperty().bind(viewModel.selectedIsEditableProperty().not());
        resetDefaultButton.disableProperty().bind(viewModel.selectedIsEditableProperty().not());
        contextMenu.getItems().get(0).disableProperty().bind(viewModel.selectedIsEditableProperty().not());
        contextMenu.getItems().get(2).disableProperty().bind(viewModel.selectedIsEditableProperty().not());
        editArea.editableProperty().bind(viewModel.selectedIsEditableProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.chosenListValidationStatus(), chosenListView));
    }

    /**
     * This is called, if a user starts typing some characters into the keyboard with focus on one ListView. The
     * ListView will scroll to the next cell with the name of the PreviewLayout fitting those characters.
     *
     * @param list       The ListView currently focused
     * @param keypressed The pressed character
     */

    private void jumpToSearchKey(ListView<PreviewLayout> list, KeyEvent keypressed) {
        if (keypressed.getCharacter() == null) {
            return;
        }

        if ((System.currentTimeMillis() - lastKeyPressTime) < 1000) {
            listSearchTerm += keypressed.getCharacter().toLowerCase();
        } else {
            listSearchTerm = keypressed.getCharacter().toLowerCase();
        }

        lastKeyPressTime = System.currentTimeMillis();

        list.getItems().stream().filter(item -> item.getDisplayName().toLowerCase().startsWith(listSearchTerm))
            .findFirst().ifPresent(list::scrollTo);
    }

    private void dragOver(DragEvent event) {
        viewModel.dragOver(event);
    }

    private void dragDetectedInAvailable(MouseEvent event) {
        List<PreviewLayout> selectedLayouts = new ArrayList<>(viewModel.availableSelectionModelProperty().getValue().getSelectedItems());
        if (!selectedLayouts.isEmpty()) {
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            viewModel.dragDetected(viewModel.availableListProperty(), viewModel.availableSelectionModelProperty(), selectedLayouts, dragboard);
        }
        event.consume();
    }

    private void dragDetectedInChosen(MouseEvent event) {
        List<PreviewLayout> selectedLayouts = new ArrayList<>(viewModel.chosenSelectionModelProperty().getValue().getSelectedItems());
        if (!selectedLayouts.isEmpty()) {
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            viewModel.dragDetected(viewModel.chosenListProperty(), viewModel.chosenSelectionModelProperty(), selectedLayouts, dragboard);
        }
        event.consume();
    }

    private void dragDropped(ListProperty<PreviewLayout> targetList, DragEvent event) {
        boolean success = viewModel.dragDropped(targetList, event.getDragboard());
        event.setDropCompleted(success);
        event.consume();
    }

    private void dragDroppedInChosenCell(PreviewLayout targetLayout, DragEvent event) {
        boolean success = viewModel.dragDroppedInChosenCell(targetLayout, event.getDragboard());
        event.setDropCompleted(success);
        event.consume();
    }

    public void toRightButtonAction() {
        viewModel.addToChosen();
    }

    public void toLeftButtonAction() {
        viewModel.removeFromChosen();
    }

    public void sortUpButtonAction() {
        viewModel.selectedInChosenUp();
    }

    public void sortDownButtonAction() {
        viewModel.selectedInChosenDown();
    }

    public void resetDefaultButtonAction() {
        viewModel.resetDefaultLayout();
    }
}
