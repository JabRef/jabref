package org.jabref.gui.preferences.preview;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preview.BstPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class PreviewTab extends AbstractFormTabView<PreviewTabViewModel> {

    // Controls of the custom available/chosen region, built in code and wired in wireControls().
    private ListView<PreviewLayout> availableListView;
    private ListView<PreviewLayout> chosenListView;
    private Button toRightButton;
    private Button toLeftButton;
    private Button sortUpButton;
    private Button sortDownButton;
    private Label readOnlyLabel;
    private Button resetDefaultButton;
    private Tab previewTab;
    private CodeArea editArea;
    private CustomTextField searchBox;

    private final StateManager stateManager;
    private final JournalAbbreviationRepository abbreviationRepository;

    private final ContextMenu contextMenu = new ContextMenu();
    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    private long lastKeyPressTime;
    private String listSearchTerm;

    public PreviewTab() {
        this.stateManager = Injector.instantiateModelOrService(StateManager.class);
        this.abbreviationRepository = Injector.instantiateModelOrService(JournalAbbreviationRepository.class);
        this.viewModel = new PreviewTabViewModel(dialogService, preferences, taskExecutor, stateManager, abbreviationRepository);
        this.lastKeyPressTime = System.currentTimeMillis();

        Node dualListRegion = buildDualListRegion();
        Node editorRegion = buildEditorRegion();

        getChildren().add(form()
                .title(Localization.lang("Current Preview"))
                .checkbox(Localization.lang("Show preview as a tab in entry editor"), viewModel.showAsExtraTabProperty())
                .checkbox(Localization.lang("Show preview in entry table tooltip"), viewModel.showPreviewInEntryTableTooltip())
                .checkbox(Localization.lang("Download cover images"), viewModel.shouldDownloadCoversProperty())
                .button(Localization.lang("Add BST file"), null, this::selectBstFile)
                .custom(dualListRegion)
                .custom(editorRegion)
                .build());

        wireControls();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry preview");
    }

    // region custom region construction (the `.custom(Node)` hatch)

    private Node buildDualListRegion() {
        searchBox = new CustomTextField();
        searchBox.setPromptText(Localization.lang("Filter"));

        availableListView = layoutListView();
        VBox availableBox = new VBox(4.0, sectionLabel(Localization.lang("Available")), searchBox, availableListView);
        HBox.setHgrow(availableBox, Priority.ALWAYS);
        VBox.setVgrow(availableListView, Priority.ALWAYS);

        toRightButton = moveButton(IconTheme.JabRefIcons.LIST_MOVE_RIGHT, this::toRightButtonAction);
        toLeftButton = moveButton(IconTheme.JabRefIcons.LIST_MOVE_LEFT, this::toLeftButtonAction);
        VBox moveButtons = new VBox(4.0, sectionLabel(""), spacer(24.0), toRightButton, toLeftButton);
        moveButtons.setAlignment(Pos.CENTER);

        chosenListView = layoutListView();
        VBox chosenBox = new VBox(4.0, sectionLabel(Localization.lang("Selected")), spacer(24.0), chosenListView);
        HBox.setHgrow(chosenBox, Priority.ALWAYS);
        VBox.setVgrow(chosenListView, Priority.ALWAYS);

        sortUpButton = moveButton(IconTheme.JabRefIcons.LIST_MOVE_UP, this::sortUpButtonAction);
        sortDownButton = moveButton(IconTheme.JabRefIcons.LIST_MOVE_DOWN, this::sortDownButtonAction);
        VBox sortButtons = new VBox(4.0, sectionLabel(""), spacer(24.0), sortUpButton, sortDownButton);
        sortButtons.setAlignment(Pos.CENTER);

        return new HBox(4.0, availableBox, moveButtons, chosenBox, sortButtons);
    }

    private Node buildEditorRegion() {
        previewTab = new Tab(Localization.lang("Preview"));
        previewTab.setClosable(false);

        editArea = new CodeArea();
        Tab editTab = new Tab(Localization.lang("Edit"), new VirtualizedScrollPane<>(editArea));
        editTab.setClosable(false);

        TabPane tabPane = new TabPane(previewTab, editTab);
        tabPane.setPrefHeight(250.0);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);

        readOnlyLabel = new Label(Localization.lang("Read only"));
        resetDefaultButton = new Button();
        resetDefaultButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.REFRESH));
        resetDefaultButton.getStyleClass().addAll("icon-button", "narrow");
        resetDefaultButton.setPrefSize(20.0, 20.0);
        resetDefaultButton.setTooltip(new Tooltip(Localization.lang("Reset default preview style")));
        resetDefaultButton.setOnAction(_ -> resetDefaultButtonAction());
        HBox topRight = new HBox(5.0, readOnlyLabel, resetDefaultButton);
        topRight.setAlignment(Pos.CENTER_RIGHT);
        AnchorPane.setTopAnchor(topRight, 2.0);
        AnchorPane.setRightAnchor(topRight, 5.0);

        return new AnchorPane(tabPane, topRight);
    }

    private ListView<PreviewLayout> layoutListView() {
        ListView<PreviewLayout> listView = new ListView<>();
        listView.setMinHeight(150.0);
        listView.setPrefHeight(250.0);
        return listView;
    }

    private Button moveButton(IconTheme.JabRefIcons icon, Runnable action) {
        Button button = new Button();
        button.setGraphic(new JabRefIconView(icon, 24));
        button.getStyleClass().add("icon-button");
        button.setPrefSize(40.0, 40.0);
        button.setOnAction(_ -> action.run());
        return button;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sectionHeader");
        return label;
    }

    private Region spacer(double height) {
        Region region = new Region();
        region.setPrefHeight(height);
        return region;
    }

    // endregion

    private class EditAction extends SimpleCommand {

        private final StandardActions command;

        public EditAction(StandardActions command) {
            this.command = command;
        }

        @Override
        public void execute() {
            if (editArea != null) {
                switch (command) {
                    case COPY ->
                            editArea.copy();
                    case CUT ->
                            editArea.cut();
                    case PASTE ->
                            editArea.paste();
                    case SELECT_ALL ->
                            editArea.selectAll();
                }
                editArea.requestFocus();
            }
        }
    }

    private void selectBstFile() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BST)
                .withDefaultExtension(StandardFileType.BST)
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(bstFile -> viewModel.addBstStyle(bstFile));
    }

    private void wireControls() {
        searchBox.setPromptText(Localization.lang("Search..."));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());

        ActionFactory factory = new ActionFactory();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT)),
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY)),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE)),
                factory.createMenuItem(StandardActions.SELECT_ALL, new EditAction(StandardActions.SELECT_ALL))
        );
        contextMenu.getItems().forEach(item -> item.setGraphic(null));
        contextMenu.getStyleClass().add("context-menu");

        availableListView.setItems(viewModel.getFilteredAvailableLayouts());
        viewModel.availableSelectionModelProperty().setValue(availableListView.getSelectionModel());
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getDisplayName)
                .withContextMenu(this::createContextMenu)
                .install(availableListView);
        availableListView.setOnDragOver(this::dragOver);
        availableListView.setOnDragDetected(this::dragDetectedInAvailable);
        availableListView.setOnDragDropped(event -> dragDropped(viewModel.availableListProperty(), event));
        availableListView.setOnKeyTyped(event -> jumpToSearchKey(availableListView, event));
        availableListView.setOnMouseClicked(this::mouseClickedAvailable);
        availableListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableListView.selectionModelProperty().getValue().selectedItemProperty().addListener((_, _, newValue) ->
                viewModel.setPreviewLayout(newValue));

        chosenListView.itemsProperty().bindBidirectional(viewModel.chosenListProperty());
        viewModel.chosenSelectionModelProperty().setValue(chosenListView.getSelectionModel());
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getDisplayName)
                .setOnDragDropped(this::dragDroppedInChosenCell)
                .withContextMenu(this::createContextMenu)
                .install(chosenListView);
        chosenListView.setOnDragOver(this::dragOver);
        chosenListView.setOnDragDetected(this::dragDetectedInChosen);
        chosenListView.setOnDragDropped(event -> dragDropped(viewModel.chosenListProperty(), event));
        chosenListView.setOnKeyTyped(event -> jumpToSearchKey(chosenListView, event));
        chosenListView.setOnMouseClicked(this::mouseClickedChosen);
        chosenListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chosenListView.selectionModelProperty().getValue().selectedItemProperty().addListener((_, _, newValue) ->
                viewModel.setPreviewLayout(newValue));

        toRightButton.disableProperty().bind(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNull());
        toLeftButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortUpButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortDownButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());

        PreviewViewer previewViewer = new PreviewViewer(dialogService, preferences, taskExecutor);
        previewViewer.setDatabaseContext(new BibDatabaseContext());
        previewViewer.setEntry(TestEntry.getTestEntry());
        EasyBind.subscribe(viewModel.selectedLayoutProperty(), previewViewer::setLayout);
        previewViewer.visibleProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull()
                                                      .or(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNotNull()));
        previewTab.setContent(previewViewer);

        editArea.clear();
        editArea.setParagraphGraphicFactory(LineNumberFactory.get(editArea));
        editArea.setContextMenu(contextMenu);
        editArea.visibleProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull()
                                                 .or(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNotNull()));

        BindingsHelper.bindBidirectional(
                editArea.textProperty(),
                viewModel.sourceTextProperty(),
                newSourceText -> editArea.replaceText(newSourceText),
                newEditText -> {
                    viewModel.sourceTextProperty().setValue(newEditText);
                    viewModel.refreshPreview();
                });

        editArea.textProperty().addListener((_, _, newValue) ->
                editArea.setStyleSpans(0, viewModel.computeHighlighting(newValue)));

        editArea.focusedProperty().addListener((_, _, newValue) -> {
            if (!newValue) {
                viewModel.refreshPreview();
            }
        });

        searchBox.textProperty().addListener((_, _, searchTerm) -> viewModel.setAvailableFilter(searchTerm));

        readOnlyLabel.visibleProperty().bind(viewModel.selectedIsEditableProperty().not());
        resetDefaultButton.disableProperty().bind(viewModel.selectedIsEditableProperty().not());
        contextMenu.getItems().getFirst().disableProperty().bind(viewModel.selectedIsEditableProperty().not());
        contextMenu.getItems().get(2).disableProperty().bind(viewModel.selectedIsEditableProperty().not());
        editArea.editableProperty().bind(viewModel.selectedIsEditableProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.chosenListValidationStatus(), chosenListView));
    }

    /// This is called, if a user starts typing some characters into the keyboard with focus on one ListView. The
    /// ListView will scroll to the next cell with the name of the PreviewLayout fitting those characters.
    ///
    /// @param list       The ListView currently focused
    /// @param keypressed The pressed character
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

    private void mouseClickedAvailable(MouseEvent event) {
        if (event.getClickCount() == 2) {
            viewModel.addToChosen();
            event.consume();
        }
    }

    private void mouseClickedChosen(MouseEvent event) {
        if (event.getClickCount() == 2) {
            viewModel.removeFromChosen();
            event.consume();
        }
    }

    private ContextMenu createContextMenu(PreviewLayout layout) {
        if (layout instanceof BstPreviewLayout) {
            ContextMenu menu = new ContextMenu();
            MenuItem deleteItem = new MenuItem(Localization.lang("Remove"));
            deleteItem.setOnAction(_ -> viewModel.removeCustomStyle(layout));
            menu.getItems().add(deleteItem);
            return menu;
        }
        return null;
    }
}
