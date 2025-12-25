//keyworseditor

package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.Comparator;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;
import com.google.common.collect.Comparators;
import com.google.common.collect.HashBiMap;

import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class KeywordsEditor extends HBox implements FieldEditorFX {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordsEditor.class);
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
    private static HashBiMap<String, String> mscmap;

    static {
        URL resourceUrl = KeywordsEditor.class.getClassLoader().getResource("msc_codes.json");

        if (resourceUrl == null) {
            LOGGER.error("Resource not found: msc_codes.json");
            mscmap = HashBiMap.create();
        }

        try {
            Optional<HashBiMap<String, String>> optionalMscCodes = MscCodeUtils.loadMscCodesFromJson(resourceUrl);

            if (optionalMscCodes.isPresent()) {
                mscmap = optionalMscCodes.get();
            } else {
                LOGGER.warn("Resource not found msc_codes.json");
                mscmap = HashBiMap.create();
            }
        } catch (MscCodeLoadingException e) {
            LOGGER.error("Error loading MSC codes", e);
            mscmap = HashBiMap.create();
        }
    }

    @FXML private KeywordsEditorViewModel viewModel;
    @FXML private TagsField<Keyword> keywordTagsField;

    @Inject private CliPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;
    @Inject private ClipBoardManager clipBoardManager;

    private boolean isSortedTagsField = false;
    private Optional<Keyword> draggedKeyword = Optional.empty();

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        // =======================
        // 🔴 LAYOUT FIX (IMPORTANT)
        // =======================
        keywordTagsField.setMinHeight(Region.USE_COMPUTED_SIZE);
        keywordTagsField.setPrefHeight(Region.USE_COMPUTED_SIZE);
        keywordTagsField.setMaxHeight(150);

        ScrollPane scrollPane = new ScrollPane(keywordTagsField);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(150);
        scrollPane.setMinHeight(Region.USE_COMPUTED_SIZE);

        setMaxHeight(150);
        setMinHeight(Region.USE_COMPUTED_SIZE);

        getChildren().clear();
        getChildren().add(scrollPane);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        // =======================

        this.viewModel = new KeywordsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferences,
                undoManager);

        keywordTagsField.setCellFactory(new ViewModelListCellFactory<Keyword>().withText(Keyword::get));
        keywordTagsField.setTagViewFactory(this::createTag);

        keywordTagsField.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        keywordTagsField.setConverter(KeywordsEditorViewModel.getStringConverter());
        keywordTagsField.setMatcher((keyword, searchText) ->
                keyword.get().toLowerCase().startsWith(searchText.toLowerCase()));
        keywordTagsField.setComparator(Comparator.comparing(Keyword::get));

        keywordTagsField.setNewItemProducer(searchText ->
                KeywordsEditorViewModel.getStringConverter().fromString(searchText));

        keywordTagsField.setShowSearchIcon(false);
        keywordTagsField.setOnMouseClicked(_ -> keywordTagsField.getEditor().requestFocus());
        keywordTagsField.getEditor().getStyleClass().setAll("tags-field-editor");
        keywordTagsField.getEditor().focusedProperty()
                        .addListener((_, _, focused) ->
                                keywordTagsField.pseudoClassStateChanged(FOCUSED, focused));

        String keywordSeparator = String.valueOf(viewModel.getKeywordSeparator());
        keywordTagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getText().equals(keywordSeparator)) {
                keywordTagsField.commit();
                event.consume();
            }
        });

        this.viewModel.keywordListProperty().addListener((_, _, _) -> {
            if (keywordTagsField.getTags().size() < 2) {
                isSortedTagsField = false;
            } else if (Comparators.isInOrder(
                    keywordTagsField.getTags(), Comparator.comparing(Keyword::get)) || isSortedTagsField) {
                isSortedTagsField = true;
                keywordTagsField.getTags().sort(Comparator.comparing(Keyword::get));
            }
        });

        keywordTagsField.getEditor().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository =
                    Injector.instantiateModelOrService(KeyBindingRepository.class);

            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.PASTE, event)) {
                String clipboardText = ClipBoardManager.getContents();
                if (!clipboardText.isEmpty()) {
                    KeywordList.parse(clipboardText, viewModel.getKeywordSeparator())
                               .forEach(keywordTagsField::addTags);
                    keywordTagsField.getEditor().clear();
                    event.consume();
                }
            }
        });

        Bindings.bindContentBidirectional(
                keywordTagsField.getTags(),
                viewModel.keywordListProperty());
    }

    private Node createTag(Keyword keyword) {
        Label tagLabel = new Label(keywordTagsField.getConverter().toString(keyword));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        tagLabel.getGraphic().setOnMouseClicked(_ -> keywordTagsField.removeTags(keyword));

        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new TagContextAction(StandardActions.COPY, keyword)),
                factory.createMenuItem(StandardActions.CUT, new TagContextAction(StandardActions.CUT, keyword)),
                factory.createMenuItem(StandardActions.DELETE, new TagContextAction(StandardActions.DELETE, keyword))
        );
        tagLabel.setContextMenu(contextMenu);

        if (mscmap.containsKey(tagLabel.getText())) {
            tagLabel.setTooltip(new Tooltip(mscmap.get(tagLabel.getText())));
        }

        return tagLabel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public double getWeight() {
        return 1;
    }

    private class TagContextAction extends SimpleCommand {
        private final StandardActions command;
        private final Keyword keyword;

        TagContextAction(StandardActions command, Keyword keyword) {
            this.command = command;
            this.keyword = keyword;
        }

        @Override
        public void execute() {
            switch (command) {
                case COPY -> clipBoardManager.setContent(keyword.get());
                case CUT -> {
                    clipBoardManager.setContent(keyword.get());
                    keywordTagsField.removeTags(keyword);
                }
                case DELETE -> keywordTagsField.removeTags(keyword);
                default -> LOGGER.info("Action {} not defined", command);
            }
        }
    }
}
