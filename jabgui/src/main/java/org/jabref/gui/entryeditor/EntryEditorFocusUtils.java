package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.Nullable;

/// Handles all focus and keyboard-navigation concerns for {@link EntryEditor}.
///
/// Owns: field-level focus capture/restore across entry changes; tab-to-tab keyboard navigation
/// (Tab/Shift-Tab wrapping) and jump-to-field lookups. DOM traversal is delegated to {@link EntryEditorFocusTraversal}.
class EntryEditorFocusUtils {

    private final TabPane tabPane;
    private final Node sceneSource;
    private final Supplier<BibDatabaseMode> databaseModeSupplier;

    private @Nullable Field lastFocusedField;

    EntryEditorFocusUtils(TabPane tabPane, Node sceneSource, Supplier<BibDatabaseMode> databaseModeSupplier) {
        this.tabPane = tabPane;
        this.sceneSource = sceneSource;
        this.databaseModeSupplier = databaseModeSupplier;
    }

    // region — field focus capture / restore

    void captureFocusedField() {
        Node focusedNode = sceneSource.getScene().getFocusOwner();
        if (focusedNode instanceof TextInputControl textInput && textInput.getId() != null) {
            lastFocusedField = FieldFactory.parseField(textInput.getId());
        }
    }

    /// Restores focus to the last captured field (if any) and moves the caret to the end.
    /// Clears the captured field afterwards so a subsequent entry change starts clean.
    void restoreLastFocusedField() {
        if (lastFocusedField == null) {
            return;
        }
        Field fieldToRestore = lastFocusedField;
        lastFocusedField = null;
        Platform.runLater(() -> {
            setFocusToField(fieldToRestore);
            Platform.runLater(() -> {
                Node focused = sceneSource.getScene().getFocusOwner();
                if (focused instanceof TextInputControl textInput) {
                    textInput.end();
                }
            });
        });
    }

    // endregion

    // region — jump to field

    void setFocusToField(Field field) {
        UiTaskExecutor.runInJavaFXThread(() -> getTabContainingField(field).ifPresentOrElse(
                tab -> selectTabAndField(tab, field),
                () -> {
                    Field aliasField = EntryConverter.FIELD_ALIASES.get(field);
                    getTabContainingField(aliasField).ifPresent(tab -> selectTabAndField(tab, aliasField));
                }
        ));
    }

    void focusOrAddField(Field field) {
        UiTaskExecutor.runInJavaFXThread(() -> getTabContainingField(field).ifPresentOrElse(
                tab -> selectTabAndField(tab, field),
                () -> {
                    Field requestedField = Optional.ofNullable(EntryConverter.FIELD_ALIASES.get(field))
                                                   .orElse(field);
                    getTabContainingField(requestedField).ifPresentOrElse(
                            tab -> selectTabAndField(tab, requestedField),
                            () -> addFieldViaAllFieldsTab(canonicalFieldForActiveMode(requestedField))
                    );
                }
        ));
    }

    private Field canonicalFieldForActiveMode(Field field) {
        Map<Field, Field> aliasesToCanonical = databaseModeSupplier.get() == BibDatabaseMode.BIBTEX
                                               ? EntryConverter.FIELD_ALIASES_BIBLATEX_TO_BIBTEX
                                               : EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX;
        return aliasesToCanonical.getOrDefault(field, field);
    }

    private Optional<FieldsEditorTab> getTabContainingField(Field field) {
        return tabPane.getTabs().stream()
                      .filter(FieldsEditorTab.class::isInstance)
                      .map(FieldsEditorTab.class::cast)
                      .filter(tab -> tab.getShownFields().contains(field))
                      .findFirst();
    }

    private void addFieldViaAllFieldsTab(Field field) {
        if (!FieldFactory.getAllFieldsWithOutInternal().contains(field)) {
            return;
        }
        tabPane.getTabs().stream()
               .filter(AllFieldsTab.class::isInstance)
               .map(AllFieldsTab.class::cast)
               .findFirst()
               .ifPresent(allFieldsTab -> {
                   tabPane.getSelectionModel().select(allFieldsTab);
                   allFieldsTab.addFieldAndFocus(field);
               });
    }

    private void selectTabAndField(FieldsEditorTab tab, Field field) {
        Platform.runLater(() -> {
            tabPane.getSelectionModel().select(tab);
            tab.requestFocus(field);
        });
        // Explicitly brings focus back to the main window containing the Entry Editor.
        sceneSource.getScene().getWindow().requestFocus();
    }

    // endregion

    // region — tab keyboard navigation (Tab / Shift-Tab wrapping)

    /// Installs Tab/Shift-Tab wrapping key filters on every focusable node inside {@code tab}'s content.
    void setupNavigationForTab(FieldsEditorTab tab) {
        Node content = tab.getContent();
        if (content instanceof Parent parent) {
            findAndSetupTabNavigableNodes(parent);
        }
    }

    private void findAndSetupTabNavigableNodes(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            child.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                    if (isLastFieldInCurrentTab(child)) {
                        moveToNextTabAndFocus();
                        event.consume();
                    }
                }
                if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
                    if (isFirstFieldInCurrentTab(child)) {
                        moveToPreviousTabAndFocus();
                        event.consume();
                    }
                }
            });

            if (child instanceof Parent childParent) {
                findAndSetupTabNavigableNodes(childParent);
            }
        }
    }

    boolean isFirstFieldInCurrentTab(Node node) {
        return isBoundaryFieldInCurrentTab(node, true);
    }

    boolean isLastFieldInCurrentTab(Node node) {
        return isBoundaryFieldInCurrentTab(node, false);
    }

    private boolean isBoundaryFieldInCurrentTab(Node node, boolean first) {
        if (node == null || tabPane.getSelectionModel().getSelectedItem() == null) {
            return false;
        }

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (!(selectedTab instanceof FieldsEditorTab currentTab)) {
            return false;
        }

        Collection<Field> shownFields = currentTab.getShownFields();
        if (!shownFields.isEmpty() && node.getId() != null) {
            Optional<Field> boundaryField = first
                                            ? shownFields.stream().findFirst()
                                            : shownFields.stream().reduce((firstField, secondField) -> secondField);
            boolean matchesBoundaryFieldId = boundaryField.map(Field::getName)
                                                          .map(name -> name.equalsIgnoreCase(node.getId()))
                                                          .orElse(false);
            if (matchesBoundaryFieldId) {
                return true;
            }
        }

        if (currentTab.getContent() instanceof Parent parent) {
            Parent searchRoot = EntryEditorFocusTraversal.findEditorGridParent(parent).orElse(parent);
            Optional<Node> boundaryNode = first
                                          ? EntryEditorFocusTraversal.findFirstFocusableNode(searchRoot)
                                          : EntryEditorFocusTraversal.findLastFocusableNode(searchRoot);
            return boundaryNode.map(n -> n == node).orElse(false);
        }

        return false;
    }

    void moveToNextTabAndFocus() {
        tabPane.getSelectionModel().selectNext();
        Platform.runLater(() -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab instanceof FieldsEditorTab currentTab) {
                focusFirstFieldInTab(currentTab);
            }
        });
    }

    void moveToPreviousTabAndFocus() {
        tabPane.getSelectionModel().selectPrevious();
        Platform.runLater(() -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab instanceof FieldsEditorTab currentTab) {
                focusLastFieldInTab(currentTab);
            }
        });
    }

    private void focusFirstFieldInTab(FieldsEditorTab tab) {
        Node tabContent = tab.getContent();
        if (!(tabContent instanceof Parent parent)) {
            return;
        }

        Collection<Field> shownFields = tab.getShownFields();
        if (!shownFields.isEmpty()) {
            Field firstField = shownFields.iterator().next();
            Optional<Node> firstTextInput = EntryEditorFocusTraversal.findFirstTextInputById(parent, firstField.getName());
            if (firstTextInput.isPresent()) {
                firstTextInput.get().requestFocus();
                return;
            }
        }

        Optional<Node> anyTextInput = EntryEditorFocusTraversal.findFirstTextInput(parent);
        if (anyTextInput.isPresent()) {
            anyTextInput.get().requestFocus();
            return;
        }

        Parent searchRoot = EntryEditorFocusTraversal.findEditorGridParent(parent).orElse(parent);
        EntryEditorFocusTraversal.findFirstFocusableNode(searchRoot).ifPresent(Node::requestFocus);
    }

    private void focusLastFieldInTab(FieldsEditorTab tab) {
        Node tabContent = tab.getContent();
        if (!(tabContent instanceof Parent parent)) {
            return;
        }

        Collection<Field> shownFields = tab.getShownFields();
        if (!shownFields.isEmpty()) {
            Optional<Field> lastField = shownFields.stream().reduce((first, second) -> second);
            Optional<Node> lastTextInput = EntryEditorFocusTraversal.findFirstTextInputById(parent, lastField.get().getName());
            if (lastTextInput.isPresent()) {
                lastTextInput.get().requestFocus();
                return;
            }
        }

        Parent searchRoot = EntryEditorFocusTraversal.findEditorGridParent(parent).orElse(parent);
        EntryEditorFocusTraversal.findLastFocusableNode(searchRoot).ifPresent(Node::requestFocus);
    }

    // endregion
}
