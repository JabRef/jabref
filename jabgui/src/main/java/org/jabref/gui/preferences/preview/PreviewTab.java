package org.jabref.gui.preferences.preview;

import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.control.TextField;
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
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preview.BstPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class PreviewTab extends AbstractPreferenceTabView<PreviewTabViewModel> {

    // Controls of the custom available/chosen region, built in code and wired in wireControls().
    private ListView<PreviewLayout> cslListView;
    //    private ListView<PreviewLayout> cslListView;
    private ListView<PreviewLayout> customizedListView;
    private ListView<PreviewLayout> chosenListView;
    private TabPane availableTabPane;   // TODO: see if needs to be refactored
    private Tab cslTab;
    private Tab customizedTab;
    private Tab previousTab;    // used to remember the context of last focused tab
    private Button toRightButton;
    private Button toLeftButton;
    private Button sortUpButton;
    private Button sortDownButton;
    private Button addCustomStyleButton;
    private Button removeCustomStyleButton;
    private Label readOnlyLabel;
    private Button resetDefaultButton;
    private Tab previewTab;
    private CodeArea editArea;
    private CustomTextField searchBox;
    private TextField styleNameField;
    private boolean isCommittingStyleName = false;

    private final StateManager stateManager;
    private final JournalAbbreviationRepository abbreviationRepository;

    private final ContextMenu contextMenu = new ContextMenu();
    private long lastKeyPressTime;
    private String listSearchTerm;

    public PreviewTab() {
        this.stateManager = Injector.instantiateModelOrService(StateManager.class);
        this.abbreviationRepository = Injector.instantiateModelOrService(JournalAbbreviationRepository.class);
        this.viewModel = new PreviewTabViewModel(
                dialogService,
                preferences.getPreviewPreferences(),
                preferences.getLayoutFormatterPreferences(),
                taskExecutor,
                stateManager,
                abbreviationRepository);
        this.lastKeyPressTime = System.currentTimeMillis();

        Node dualListRegion = buildDualListRegion();
        Node editorRegion = buildEditorRegion();

        setContent(form()
                .checkbox(Localization.lang("Show preview as a tab in entry editor"), viewModel.showAsExtraTabProperty())
                .checkbox(Localization.lang("Show preview in entry table tooltip"), viewModel.showPreviewInEntryTableTooltip())
                .checkbox(Localization.lang("Download cover images"), viewModel.shouldDownloadCoversProperty())
                .button(Localization.lang("Add BST file"), this::selectBstFile)
                .custom(dualListRegion, lists -> lists
                        .validate(viewModel.chosenListValidationStatus(), chosenListView))
                .custom(editorRegion)
                .build());

        wireControls();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry preview");
    }

    @Override
    public String getTitle() {
        return Localization.lang("Current Preview");
    }

    // region custom region construction (the `.custom(Node)` hatch)

    private Node buildDualListRegion() {
        searchBox = new CustomTextField();
        searchBox.setPromptText(Localization.lang("Filter"));

        //        availableListView = layoutListView();
        //        VBox availableBox = new VBox(4.0, sectionLabel(Localization.lang("Available")), searchBox, availableListView);
        //        HBox.setHgrow(availableBox, Priority.ALWAYS);
        //        VBox.setVgrow(availableListView, Priority.ALWAYS);

        cslListView = layoutListView();
        customizedListView = layoutListView();
        this.cslTab = new Tab(Localization.lang("CSL"), cslListView);
        cslTab.setClosable(false);
        previousTab = cslTab;

        //        this.customizedTab = new Tab(Localization.lang("Customized"), customizedListView);
        addCustomStyleButton = new Button();
        removeCustomStyleButton = new Button();

        addCustomStyleButton.getStyleClass().add("icon-button");
        removeCustomStyleButton.getStyleClass().add("icon-button");

        addCustomStyleButton.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
        removeCustomStyleButton.setGraphic(IconTheme.JabRefIcons.REMOVE.getGraphicNode());

        addCustomStyleButton.setPrefWidth(30);
        removeCustomStyleButton.setPrefWidth(30);

        VBox buttonBox = new VBox(5, addCustomStyleButton, removeCustomStyleButton);
        buttonBox.setAlignment(Pos.TOP_CENTER);

        HBox customizedPane = new HBox(5);
        customizedPane.getChildren().addAll(customizedListView, buttonBox);
        HBox.setHgrow(customizedListView, Priority.ALWAYS);

        customizedTab = new Tab(Localization.lang("Customized"), customizedPane);
        customizedTab.setClosable(false);

        availableTabPane = new TabPane(cslTab, customizedTab);
        availableTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox.setVgrow(availableTabPane, Priority.ALWAYS);
        VBox availableBox = new VBox(4.0, sectionLabel(Localization.lang("Available")), searchBox, availableTabPane);
        HBox.setHgrow(availableBox, Priority.ALWAYS);

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

        styleNameField = new TextField();
        styleNameField.setPromptText(Localization.lang("Style name"));
        EasyBind.subscribe(viewModel.styleNameProperty(), styleNameField::setText);
        styleNameField.editableProperty().bind(viewModel.selectedIsEditableProperty());
        styleNameField.setOnAction(_ -> commitStyleNameEdit()); // Commit on Enter
        //        styleNameField.focusedProperty().addListener((_, wasFocused, isFocused) -> {  // unfocus commit
        //            // Commit on focus-lost from the styleNameField
        //            if (wasFocused && !isFocused) {
        //                commitStyleNameEdit(true);
        //            }
        //        });
        //        styleNameField.setOnKeyPressed(this::cancelRenameOnEscapeKeyPress); // allow to cancel rename with esc key

        readOnlyLabel = new Label(Localization.lang("Read only"));
        resetDefaultButton = new Button();
        resetDefaultButton.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        resetDefaultButton.getStyleClass().addAll("icon-button", "narrow");
        resetDefaultButton.setPrefSize(20.0, 20.0);
        resetDefaultButton.setTooltip(new Tooltip(Localization.lang("Reset default preview style")));
        resetDefaultButton.setOnAction(_ -> resetDefaultButtonAction());
        //        HBox topRight = new HBox(5.0, readOnlyLabel, resetDefaultButton);
        HBox topRight = new HBox(10, readOnlyLabel,
                resetDefaultButton,
                new Label(Localization.lang("Name")),
                styleNameField);

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
        button.setGraphic(icon.withSize(24).getGraphicNode());
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

    /*
    TODO: fix duplicate adding into selected
    fix arrow buttons, cannot add from customized into selected
    swapping csl->customized, cannot drag and drop

    csl into selected, I can add, but the csl does not lose it's value
    but when I move selected back into csl, it creates duplicate values. - fix

    when using arrow button, it remembers the last click, not whats highlighted on respective side
    and then it moves the same entry into the selected
    this only applies to when we move entries into Selected, from customized.
    click right, click left, click right, then click right button, it moves the last right clicked
    into the right, instead of left highlighted into the right.

    double clicking left side allows for duplicates on right/selected side

     */
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

        //        cslListView.setItems(viewModel.getFilteredAvailableLayouts());
        cslListView.setItems(viewModel.getFilteredCslLayouts());
        customizedListView.setItems(viewModel.getFilteredCustomizedLayouts());

        viewModel.availableSelectionModelProperty().setValue(cslListView.getSelectionModel());
        availableTabPane.getSelectionModel()
                        .selectedItemProperty()
                        .addListener((obs, oldTab, newTab) -> {

                            if (newTab == cslTab) {
                                viewModel.availableSelectionModelProperty()
                                         .setValue(cslListView.getSelectionModel());
                            } else {
                                viewModel.availableSelectionModelProperty()
                                         .setValue(customizedListView.getSelectionModel());
                            }
                            previousTab = newTab;
                            focusRightButtonBinding();
                        });
        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getDisplayName)
                .withContextMenu(this::createContextMenu)
                .install(cslListView);

        new ViewModelListCellFactory<PreviewLayout>()
                .withText(PreviewLayout::getDisplayName)
                .withContextMenu(this::createContextMenu)
                .install(customizedListView);

        cslListView.setOnDragOver(this::dragOver);
        cslListView.setOnDragDetected(this::dragDetectedInAvailable);
        cslListView.setOnDragDropped(event -> dragDropped(viewModel.cslListProperty(), event));
        cslListView.setOnKeyTyped(event -> jumpToSearchKey(cslListView, event));
        //        cslListView.setOnMouseClicked(this::mouseClickedAvailable);
        cslListView.setOnMouseClicked(event -> {
            viewModel.availableSelectionModelProperty()
                     .setValue(cslListView.getSelectionModel());
            mouseClickedAvailable(event);
        });
        cslListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cslListView.selectionModelProperty().getValue().selectedItemProperty().addListener((_, _, newValue) ->
                viewModel.setPreviewLayout(newValue));

        customizedListView.setOnDragOver(this::dragOver);
        customizedListView.setOnDragDetected(this::dragDetectedInAvailable);
        customizedListView.setOnDragDropped(event -> dragDropped(viewModel.customizedListProperty(), event));
        customizedListView.setOnKeyTyped(event -> jumpToSearchKey(customizedListView, event));
        //        customizedListView.setOnMouseClicked(this::mouseClickedAvailable);
        customizedListView.setOnMouseClicked(event -> {
            viewModel.availableSelectionModelProperty().setValue(customizedListView.getSelectionModel());
            mouseClickedAvailable(event);
        });
        customizedListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        customizedListView.selectionModelProperty().getValue().selectedItemProperty().addListener((_, _, newValue) ->
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
        //        chosenListView.setOnMouseClicked(this::mouseClickedChosen);
        chosenListView.setOnMouseClicked(event -> {
            viewModel.chosenSelectionModelProperty()
                     .setValue(chosenListView.getSelectionModel());
            mouseClickedChosen(event);
        });
        chosenListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chosenListView.selectionModelProperty().getValue().selectedItemProperty().addListener((_, _, newValue) ->
                viewModel.setPreviewLayout(newValue));

        addCustomStyleButton.setOnAction(event -> {
            viewModel.addCustomizedStyle();
            customizedListView.refresh();   // TODO: remove this if not needed
        });

        removeCustomStyleButton.setOnAction(event -> {
            viewModel.removeCustomizedStyle();
            customizedListView.refresh();   // TODO: remove this if not needed
        });

        removeCustomStyleButton.disableProperty().bind(customizedListView.getSelectionModel().selectedItemProperty().isNull());
        toRightButton.disableProperty().bind(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNull());
        toLeftButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortUpButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());
        sortDownButton.disableProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNull());

        PreviewViewer previewViewer = new PreviewViewer(dialogService, preferences, taskExecutor);
        previewViewer.setDatabaseContext(new BibDatabaseContext());
        previewViewer.setEntry(TestEntry.getTestEntry());
        EasyBind.subscribe(viewModel.selectedLayoutProperty(), previewViewer::setLayout);
        //        previewViewer.visibleProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull()
        //                                                      .or(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNotNull()));
        previewViewer.visibleProperty().bind(viewModel.selectedLayoutProperty().isNotNull());

        previewTab.setContent(previewViewer);

        editArea.clear();
        editArea.setParagraphGraphicFactory(LineNumberFactory.get(editArea));
        editArea.setContextMenu(contextMenu);
        //        editArea.visibleProperty().bind(viewModel.chosenSelectionModelProperty().getValue().selectedItemProperty().isNotNull()
        //                                                 .or(viewModel.availableSelectionModelProperty().getValue().selectedItemProperty().isNotNull()));
        editArea.visibleProperty().bind(viewModel.selectedLayoutProperty().isNotNull());
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

    //    private void dragDetectedInAvailable(MouseEvent event) {
    //        List<PreviewLayout> selectedLayouts = new ArrayList<>(viewModel.availableSelectionModelProperty().getValue().getSelectedItems());
    //        if (!selectedLayouts.isEmpty()) {
    //            Dragboard dragboard = cslListView.startDragAndDrop(TransferMode.MOVE);
    //            viewModel.dragDetected(viewModel.cslListProperty(), viewModel.availableSelectionModelProperty(), selectedLayouts, dragboard);
    //        }
    //        event.consume();
    //    }

    private void dragDetectedInAvailable(MouseEvent event) {
        List<PreviewLayout> selectedLayouts = new ArrayList<>(viewModel.availableSelectionModelProperty().getValue().getSelectedItems());
        ListView<PreviewLayout> sourceListView;
        ListProperty<PreviewLayout> sourceListProperty;
        if (availableTabPane.getSelectionModel().getSelectedItem() == cslTab) {
            sourceListView = this.cslListView;
            sourceListProperty = viewModel.cslListProperty();
        } else {
            sourceListView = this.customizedListView;
            sourceListProperty = viewModel.customizedListProperty();
        }

        if (!selectedLayouts.isEmpty()) {
            Dragboard dragboard = sourceListView.startDragAndDrop(TransferMode.MOVE);
            viewModel.dragDetected(sourceListProperty, viewModel.availableSelectionModelProperty(), selectedLayouts, dragboard);
        }
        event.consume();
    }

    private void dragDetectedInChosen(MouseEvent event) {
        List<PreviewLayout> selectedLayouts = new ArrayList<>(viewModel.chosenSelectionModelProperty().getValue().getSelectedItems());
        if (!selectedLayouts.isEmpty()) {
            Dragboard dragboard = chosenListView.startDragAndDrop(TransferMode.MOVE);
            viewModel.dragDetected(viewModel.chosenListProperty(), viewModel.chosenSelectionModelProperty(), selectedLayouts, dragboard);
        }
        event.consume();
    }

    private void dragDropped(ListProperty<PreviewLayout> targetList, DragEvent event) {
        boolean success = viewModel.dragDropped(targetList, event.getDragboard());
        event.setDropCompleted(success);
        // Only switch tabs when routing OUT of Selected INTO Available
        // dropping in Selected has no need to refocus the Available tabs.
        if (success && targetList != viewModel.chosenListProperty()) {
            focusTabOnLastRoutedLayout();
        }
        event.consume();
    }

    private void dragDroppedInChosenCell(PreviewLayout targetLayout, DragEvent event) {
        boolean success = viewModel.dragDroppedInChosenCell(targetLayout, event.getDragboard());
        event.setDropCompleted(success);
        //        if (success) {
        //            focusTabOnLastRoutedLayout();
        //        }
        event.consume();
    }

    //    public void toRightButtonAction() {
    //        viewModel.addToChosen();
    //    }
    public void toRightButtonAction() {
        //        if (availableTabPane.getSelectionModel().getSelectedItem() == cslTab) {
        if (previousTab == cslTab) {
            viewModel.addToChosen(viewModel.cslListProperty());
        } else {
            viewModel.addToChosen(viewModel.customizedListProperty());
        }
    }

    //    public void toLeftButtonAction() {
    //        viewModel.removeFromChosen();
    //    }
    public void toLeftButtonAction() {
        viewModel.removeFromChosen();
        focusTabOnLastRoutedLayout();
        //        if (previousTab == cslTab) {
        //            viewModel.removeFromChosen();
        //            //            viewModel.removeFromChosen(viewModel.cslListProperty());
        //        } else {
        //            viewModel.removeFromChosen();
        //            //            viewModel.removeFromChosen(viewModel.customizedListProperty());
        //        }
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

    //    private void mouseClickedAvailable(MouseEvent event) {
    //        if (event.getClickCount() == 2) {
    //            viewModel.addToChosen();
    //            event.consume();
    //        }
    //    }

    private void mouseClickedAvailable(MouseEvent event) {
        if (event.getClickCount() == 2) {
            if (previousTab == cslTab) {
                viewModel.addToChosen(viewModel.cslListProperty());
            } else {
                viewModel.addToChosen(viewModel.customizedListProperty());
            }
            event.consume();
        }
    }

    //    private void mouseClickedChosen(MouseEvent event) {
    //        if (event.getClickCount() == 2) {
    //            viewModel.removeFromChosen();
    //            event.consume();
    //        }
    //    }

    private void mouseClickedChosen(MouseEvent event) {
        if (event.getClickCount() == 2) {
            viewModel.removeFromChosen();
            focusTabOnLastRoutedLayout();
            //            if (previousTab == cslTab) {
            //                viewModel.removeFromChosen(viewModel.cslListProperty());
            //            } else {
            //                viewModel.removeFromChosen(viewModel.customizedListProperty());
            //            }
            //            viewModel.removeFromChosen(viewModel.cslListProperty());
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

    private void focusTabOnLastRoutedLayout() {
        PreviewLayout moved = viewModel.lastRoutedLayoutProperty().getValue();
        if (moved != null) {
            availableTabPane.getSelectionModel()
                            .select((moved instanceof TextBasedPreviewLayout) ? customizedTab : cslTab);
            //            availableTabPane.getSelectionModel().select(customizedTab);
        }
    }

    private void focusRightButtonBinding() {
        toRightButton.disableProperty().unbind();
        toRightButton.disableProperty().bind(viewModel.availableSelectionModelProperty()
                                                      .getValue()
                                                      .selectedItemProperty()
                                                      .isNull());
    }

    private void commitStyleNameEdit() {
        if (isCommittingStyleName) {
            return;
        }
        try {
            isCommittingStyleName = true;
            viewModel.renameSelectedStyle(styleNameField.getText());
            customizedListView.refresh();   // refresh view to display rename in 'customized'
            chosenListView.refresh();       // refresh view to display rename in 'selected'
        } finally {
            isCommittingStyleName = false;
        }

        //        viewModel.renameSelectedStyle(styleNameField.getText());
        //        customizedListView.refresh();   // refresh view to display rename in 'customized'
    }

    //    private void cancelRenameOnEscapeKeyPress(KeyEvent event) {
    //        if (event.getCode() == KeyCode.ESCAPE) {
    //            styleNameField.setText(viewModel.selectedLayoutProperty().getValue() != null
    //                                   ? viewModel.selectedLayoutProperty().getValue().getDisplayName()
    //                                   : "");
    //            chosenListView.requestFocus();
    //            //            editArea.requestFocus();
    //            event.consume();
    //        }
    //    }
}
