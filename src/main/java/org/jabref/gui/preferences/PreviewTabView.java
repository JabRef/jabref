package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class PreviewTabView extends AbstractPreferenceTabView implements PreferencesTab {

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

    private final ContextMenu contextMenu = new ContextMenu();

    private long lastKeyPressTime;
    private String listSearchTerm;

    private ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    private class EditAction extends SimpleCommand {

        private final StandardActions command;

        public EditAction(StandardActions command) {
            this.command = command;
        }

        @Override
        public void execute() {
            if (editArea != null) {
                switch (command) {
                    case COPY:
                        editArea.copy();
                        break;
                    case CUT:
                        editArea.cut();
                        break;
                    case PASTE:
                        editArea.paste();
                        break;
                    case SELECT_ALL:
                        editArea.selectAll();
                        break;
                }
                editArea.requestFocus();
            }
        }
    }

    public PreviewTabView(JabRefPreferences preferences) {
        this.preferences = preferences;
                ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Entry preview"); }

    public void initialize() {
        PreviewTabViewModel previewTabViewModel = new PreviewTabViewModel(dialogService, preferences, taskExecutor);
        this.viewModel = previewTabViewModel;

        lastKeyPressTime = System.currentTimeMillis();

        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.CUT, new PreviewTabView.EditAction(StandardActions.CUT)),
                factory.createMenuItem(StandardActions.COPY, new PreviewTabView.EditAction(StandardActions.COPY)),
                factory.createMenuItem(StandardActions.PASTE, new PreviewTabView.EditAction(StandardActions.PASTE)),
                factory.createMenuItem(StandardActions.SELECT_ALL, new PreviewTabView.EditAction(StandardActions.SELECT_ALL))
        );
        contextMenu.getItems().forEach(item -> item.setGraphic(null));
        contextMenu.getStyleClass().add("context-menu");

        availableListView.itemsProperty().bindBidirectional(previewTabViewModel.availableListProperty());
        previewTabViewModel.availableSelectionModelProperty().setValue(availableListView.getSelectionModel());
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getName)
                .install(availableListView);
        availableListView.setOnDragOver(this::dragOver);
        availableListView.setOnDragDetected(this::dragDetectedInAvailable);
        availableListView.setOnDragDropped(event -> dragDropped(previewTabViewModel.availableListProperty(), event));
        availableListView.setOnKeyTyped(event -> jumpToSearchKey(availableListView, event));
        availableListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        chosenListView.itemsProperty().bindBidirectional(previewTabViewModel.chosenListProperty());
        previewTabViewModel.chosenSelectionModelProperty().setValue(chosenListView.getSelectionModel());
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getName)
                .setOnDragDropped(this::dragDroppedInChosenCell)
                .install(chosenListView);
        chosenListView.setOnDragOver(this::dragOver);
        chosenListView.setOnDragDetected(this::dragDetectedInChosen);
        chosenListView.setOnDragDropped(event -> dragDropped(previewTabViewModel.chosenListProperty(), event));
        chosenListView.setOnKeyTyped(event -> jumpToSearchKey(chosenListView, event));
        chosenListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chosenListView.selectionModelProperty().getValue().selectedItemProperty().addListener((observable, oldValue, newValue) -> previewTabViewModel.setPreviewLayout(newValue));

        toRightButton.disableProperty().bind(previewTabViewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNull());

        toLeftButton.disableProperty().bind(previewTabViewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortUpButton.disableProperty().bind(previewTabViewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortDownButton.disableProperty().bind(previewTabViewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());

        previewTab.setContent(new PreviewViewer(new BibDatabaseContext(), dialogService, Globals.stateManager));
        ((PreviewViewer) previewTab.getContent()).setEntry(TestEntry.getTestEntry());
        EasyBind.subscribe(previewTabViewModel.layoutProperty(), value -> ((PreviewViewer) previewTab.getContent()).setLayout(value));
        previewTab.getContent().visibleProperty().bind(previewTabViewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull());

        editArea.clear();
        editArea.setParagraphGraphicFactory(LineNumberFactory.get(editArea));
        editArea.setContextMenu(contextMenu);
        editArea.visibleProperty().bind(previewTabViewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull());
        previewTabViewModel.sourceTextProperty().addListener((observable, oldValue, newValue) -> editArea.replaceText(newValue));
        editArea.textProperty().addListener((observable, oldValue, newValue) -> {
            previewTabViewModel.sourceTextProperty().setValue(newValue);
            editArea.setStyleSpans(0, previewTabViewModel.computeHighlighting(newValue));
        });
        editArea.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                previewTabViewModel.refreshPreview();
            }
        });

        readOnlyLabel.visibleProperty().bind(previewTabViewModel.selectedIsEditableProperty().not());
        resetDefaultButton.disableProperty().bind(previewTabViewModel.selectedIsEditableProperty().not());
        contextMenu.getItems().get(0).disableProperty().bind(previewTabViewModel.selectedIsEditableProperty().not());
        contextMenu.getItems().get(2).disableProperty().bind(previewTabViewModel.selectedIsEditableProperty().not());
        editArea.editableProperty().bind(previewTabViewModel.selectedIsEditableProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(previewTabViewModel.chosenListValidationStatus(), chosenListView));
    }

    /**
     * This is called, if a user starts typing some characters into the keyboard with focus on one ListView.
     * The ListView will scroll to the next cell with the name of the PreviewLayout fitting those characters.
     * @param list The ListView currently focused
     * @param keypressed The pressed character
     */

    private void jumpToSearchKey(ListView<PreviewLayout> list, KeyEvent keypressed) {
        if (keypressed.getCharacter() == null) {
            return;
        }

        if (System.currentTimeMillis() - lastKeyPressTime < 1000) {
            listSearchTerm += keypressed.getCharacter().toLowerCase();
        } else {
            listSearchTerm = keypressed.getCharacter().toLowerCase();
        }

        lastKeyPressTime = System.currentTimeMillis();

        list.getItems().stream().filter(item -> item.getName().toLowerCase().startsWith(listSearchTerm))
            .findFirst().ifPresent(list::scrollTo);
    }

    private void dragOver(DragEvent event) { ((PreviewTabViewModel) viewModel).dragOver(event); }

    private void dragDetectedInAvailable(MouseEvent event) {
        PreviewTabViewModel previewTabViewModel = (PreviewTabViewModel) viewModel;
        List<PreviewLayout> selectedLayouts = new ArrayList<>(previewTabViewModel.availableSelectionModelProperty().getValue().getSelectedItems());
        if (!selectedLayouts.isEmpty()) {
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            previewTabViewModel.dragDetected(previewTabViewModel.availableListProperty(), selectedLayouts, dragboard);
        }
        event.consume();
    }

    private void dragDetectedInChosen(MouseEvent event) {
        PreviewTabViewModel previewTabViewModel = (PreviewTabViewModel) viewModel;
        List<PreviewLayout> selectedLayouts = new ArrayList<>(previewTabViewModel.chosenSelectionModelProperty().getValue().getSelectedItems());
        if (!selectedLayouts.isEmpty()) {
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            previewTabViewModel.dragDetected(previewTabViewModel.chosenListProperty(), selectedLayouts, dragboard);
        }
        event.consume();
    }

    private void dragDropped(ListProperty<PreviewLayout> targetList, DragEvent event) {
        boolean success = ((PreviewTabViewModel) viewModel).dragDropped(targetList, event.getDragboard());
        event.setDropCompleted(success);
        event.consume();
    }

    private void dragDroppedInChosenCell(PreviewLayout targetLayout, DragEvent event) {
        boolean success = ((PreviewTabViewModel) viewModel).dragDroppedInChosenCell(targetLayout, event.getDragboard());
        event.setDropCompleted(success);
        event.consume();
    }

    public void toRightButtonAction() { ((PreviewTabViewModel) viewModel).addToChosen(); }

    public void toLeftButtonAction() { ((PreviewTabViewModel) viewModel).removeFromChosen(); }

    public void sortUpButtonAction() { ((PreviewTabViewModel) viewModel).selectedInChosenUp(); }

    public void sortDownButtonAction() { ((PreviewTabViewModel) viewModel).selectedInChosenDown(); }

    public void resetDefaultButtonAction() { ((PreviewTabViewModel) viewModel).resetDefaultLayout(); }
}
