package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsEditorViewModel extends AbstractEditorViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupsEditorViewModel.class);

    private final ListProperty<Keyword> groupListProperty;
    private final Character groupSeparator;
    private final SuggestionProvider<?> suggestionProvider;

    public GroupsEditorViewModel(Field field,
                                 SuggestionProvider<?> suggestionProvider,
                                 FieldCheckers fieldCheckers,
                                 CliPreferences preferences,
                                 UndoManager undoManager) {

        super(field, suggestionProvider, fieldCheckers, undoManager);

        groupListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.groupSeparator = preferences.getBibEntryPreferences().getKeywordSeparator();
        this.suggestionProvider = suggestionProvider;

        BindingsHelper.bindContentBidirectional(
                groupListProperty,
                text,
                this::serializeGroups,
                this::parseGroups);
    }

    private String serializeGroups(List<Keyword> groups) {
        return KeywordList.serialize(groups, groupSeparator);
    }

    private List<Keyword> parseGroups(String newText) {
        return KeywordList.parse(newText, groupSeparator).stream().toList();
    }

    public ListProperty<Keyword> groupListProperty() {
        return groupListProperty;
    }

    static StringConverter<Keyword> getStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Keyword group) {
                if (group == null) {
                    LOGGER.debug("Group is null");
                    return "";
                }
                return group.toString();
            }

            @Override
            public Keyword fromString(String groupString) {
                return Keyword.ofHierarchical(groupString);
            }
        };
    }

    public List<Keyword> getSuggestions(String request) {
        List<Keyword> suggestions = suggestionProvider.getPossibleSuggestions().stream()
                                                      .map(String.class::cast)
                                                      .filter(group -> group.toLowerCase().contains(request.toLowerCase()))
                                                      .map(Keyword::new)
                                                      .distinct()
                                                      .collect(Collectors.toList());

        Keyword requestedGroup = new Keyword(request);
        if (!suggestions.contains(requestedGroup)) {
            suggestions.addFirst(requestedGroup);
        }

        return suggestions;
    }

    public Character getGroupSeparator() {
        return groupSeparator;
    }
}
