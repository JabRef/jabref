package org.jabref.gui.fieldeditors;

import java.net.URL;
<<<<<<< fix/issue-15108
=======
import java.util.Comparator;
import java.util.List;
>>>>>>> main
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordsEditor extends TagsEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordsEditor.class);
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

    private final KeywordsEditorViewModel viewModel;

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers) {

        super(field, suggestionProvider, fieldCheckers, Injector.instantiateModelOrService(UndoManager.class));

        this.viewModel = new KeywordsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                Injector.instantiateModelOrService(CliPreferences.class),
                undoManager);

<<<<<<< fix/issue-15108
        setupTagsField(
                KeywordsEditorViewModel.getStringConverter(),
                viewModel.getKeywordSeparator(),
                viewModel.keywordListProperty(),
                viewModel::getSuggestions);
    }

    @Override
    protected void customizeTag(Label tagLabel, Keyword keyword) {
=======
        keywordTagsField.setCellFactory(new ViewModelListCellFactory<Keyword>().withText(Keyword::get));
        keywordTagsField.setTagViewFactory(this::createTag);

        keywordTagsField.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        keywordTagsField.setConverter(KeywordsEditorViewModel.getStringConverter());
        keywordTagsField.setMatcher((keyword, searchText) -> keyword.get().toLowerCase().startsWith(searchText.toLowerCase()));
        keywordTagsField.setComparator(Comparator.comparing(Keyword::get));

        keywordTagsField.setNewItemProducer(searchText -> viewModel.parseKeyword(searchText));

        keywordTagsField.setShowSearchIcon(false);
        keywordTagsField.setOnMouseClicked(_ -> keywordTagsField.getEditor().requestFocus());
        keywordTagsField.getEditor().getStyleClass().clear();
        keywordTagsField.getEditor().getStyleClass().add("tags-field-editor");
        keywordTagsField.getEditor().focusedProperty().addListener((_, _, newValue) -> keywordTagsField.pseudoClassStateChanged(FOCUSED, newValue));

        String keywordSeparator = String.valueOf(viewModel.getKeywordSeparator());
        keywordTagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getText().equals(keywordSeparator)) {
                String editorText = keywordTagsField.getEditor().getText();

                if (isSeparatedKeyword(editorText, keywordSeparator)) {
                    keywordTagsField.commit();
                }
                event.consume();
            }
        });

        this.viewModel.keywordListProperty().addListener((_, _, _) -> {
            if (keywordTagsField.getTags().size() < 2) {
                isSortedTagsField = false;
            } else if ((Comparators.isInOrder(keywordTagsField.getTags(), Comparator.comparing(Keyword::get))) || isSortedTagsField) {
                isSortedTagsField = true;
                keywordTagsField.getTags().sort(Comparator.comparing(Keyword::get));
            }
        });

        keywordTagsField.getEditor().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);

            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.PASTE, event)) {
                String clipboardText = ClipBoardManager.getContents();
                if (!clipboardText.isEmpty()) {
                    KeywordList keywordsList = KeywordList.parse(clipboardText, viewModel.getKeywordSeparator());
                    keywordsList.stream().forEach(keyword -> keywordTagsField.addTags(keyword));
                    keywordTagsField.getEditor().clear();
                    event.consume();
                }
            }
        });

        Bindings.bindContentBidirectional(keywordTagsField.getTags(), viewModel.keywordListProperty());
    }

    private boolean isSeparatedKeyword(String keywordString, String keywordSeparator) {
        int separatorLastOccurrence = keywordString.lastIndexOf(keywordSeparator);
        if (separatorLastOccurrence == -1) {
            return false;
        }

        int separatorFirstOccurrence = keywordString.lastIndexOf(keywordSeparator);
        String substringWithSeparator = new StringBuilder(keywordString.substring(0, separatorFirstOccurrence)).reverse().toString();

        AtomicBoolean isSeparatedKeyword = new AtomicBoolean(true);
        substringWithSeparator.chars().takeWhile(symbol -> symbol == Keyword.DEFAULT_ESCAPE_SYMBOL)
                              .forEachOrdered(_ -> isSeparatedKeyword.set(!isSeparatedKeyword.get()));

        return isSeparatedKeyword.get();
    }

    private Node createTag(Keyword keyword) {
        Label tagLabel = new Label();
        tagLabel.setText(keywordTagsField.getConverter().toString(keyword));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(_ -> keywordTagsField.removeTags(keyword));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new KeywordsEditor.TagContextAction(StandardActions.COPY, keyword)),
                factory.createMenuItem(StandardActions.CUT, new KeywordsEditor.TagContextAction(StandardActions.CUT, keyword)),
                factory.createMenuItem(StandardActions.DELETE, new KeywordsEditor.TagContextAction(StandardActions.DELETE, keyword))
        );
        tagLabel.setContextMenu(contextMenu);
        tagLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                keywordTagsField.removeTags(keyword);
                keywordTagsField.getEditor().setText(KeywordList.serialize(List.of(keyword), viewModel.getKeywordSeparator()));
                keywordTagsField.getEditor().positionCaret(keyword.get().length());
            }
        });
        tagLabel.setOnDragOver(event -> {
            if (event.getGestureSource() != tagLabel && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // Checks Keyword for MSC code and displays tooltip with corresponding description
>>>>>>> main
        if (mscmap.containsKey(tagLabel.getText())) {
            String mscClassification = mscmap.get(tagLabel.getText());
            Tooltip tooltip = new Tooltip(mscClassification);
            tagLabel.setTooltip(tooltip);
        }
    }

    public KeywordsEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }
}
