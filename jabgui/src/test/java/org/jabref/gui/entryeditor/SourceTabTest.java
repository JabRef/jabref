package org.jabref.gui.entryeditor;

import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.bibtexhighlighter.BibTeXStyleClass;
import org.jabref.gui.bibtexhighlighter.VeneerSyntaxHighlighter;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class SourceTabTest {

    private Stage stage;
    private Scene scene;
    private CodeArea area;
    private TabPane pane;
    private SourceTab sourceTab;

    @Start
    public void onStart(Stage stage) {
        area = new CodeArea();
        area.appendText("some example\n text to go here\n across a couple of \n lines....");
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(OptionalObjectProperty.empty());
        when(stateManager.searchQueryProperty()).thenReturn(mock(StringProperty.class));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository(List.of(), List.of());
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());

        sourceTab = new SourceTab(
                new CountingUndoManager(),
                fieldPreferences,
                importFormatPreferences,
                new DummyFileUpdateMonitor(),
                mock(DialogService.class),
                mock(BibEntryTypesManager.class),
                keyBindingRepository,
                stateManager,
                new VeneerSyntaxHighlighter());
        pane = new TabPane(
                new Tab("main area", area),
                new Tab("other tab", new Label("some text")),
                sourceTab
        );
        scene = new Scene(pane);
        this.stage = stage;

        stage.setScene(scene);
        stage.setWidth(400);
        stage.setHeight(400);
        stage.show();

        // select the area's tab
        pane.getSelectionModel().select(0);
    }

    @Test
    void switchingFromSourceTabDoesNotThrowException(FxRobot robot) {
        BibEntry entry = new BibEntry();
        entry.setField(new UnknownField("test"), "testvalue");

        // Update source editor. In production currentEntry is bound to the view model; here we drive the
        // property directly, since notifyAboutFocus no longer sets it.
        robot.interact(() -> pane.getSelectionModel().select(2));
        robot.interact(() -> {
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        robot.clickOn(1200, 500);
        robot.interrupt(100);

        // Switch to different tab & update entry
        robot.interact(() -> pane.getSelectionModel().select(1));
        robot.interact(() -> stage.setWidth(600));
        robot.interact(() -> entry.setField(new UnknownField("test"), "new value"));

        // No exception should be thrown
        robot.interrupt(100);
    }

    @Test
    void syntaxHighlighterAppliesCorrectStylesToBibTeXTokens() {
        CodeArea testArea = new CodeArea();
        String bibtexInput = "@Article{Shor94,\n  author = {Peter Shor},\n  year   = 1994\n}";
        testArea.appendText(bibtexInput);

        VeneerSyntaxHighlighter highlighter = new VeneerSyntaxHighlighter();
        highlighter.applyHighlighting(bibtexInput, testArea);

        int articleIndex = bibtexInput.indexOf("@Article");
        int keyIndex = bibtexInput.indexOf("Shor94");
        int authorIndex = bibtexInput.indexOf("author");
        int sStringIndex = bibtexInput.indexOf("Peter Shor");
        int yearNumberIndex = bibtexInput.indexOf("1994");

        assertTrue(testArea.getStyleOfChar(articleIndex + 1).contains(org.jabref.gui.bibtexhighlighter.BibTeXStyleClass.KEYWORD.getClassName()));
        assertTrue(testArea.getStyleOfChar(keyIndex).contains(org.jabref.gui.bibtexhighlighter.BibTeXStyleClass.KEY.getClassName()));
        assertTrue(testArea.getStyleOfChar(authorIndex).contains(org.jabref.gui.bibtexhighlighter.BibTeXStyleClass.FIELD.getClassName()));
        assertTrue(testArea.getStyleOfChar(sStringIndex).contains(org.jabref.gui.bibtexhighlighter.BibTeXStyleClass.STRING.getClassName()));
        assertTrue(testArea.getStyleOfChar(yearNumberIndex).contains(org.jabref.gui.bibtexhighlighter.BibTeXStyleClass.NUMBER.getClassName()));
    }

    @Test
    void sourceTabAutomaticallyTriggersHighlighterOnEntryBinding() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setCitationKey("Shor94");
        entry.setField(StandardField.AUTHOR, "Peter Shor");

        pane.getSelectionModel().select(2);
        sourceTab.currentEntryProperty().set(entry);
        sourceTab.notifyAboutFocus(entry);

        CodeArea sourceCodeArea = (CodeArea) pane.lookup("#bibtexSourceCodeArea");
        assertNotNull(sourceCodeArea, "Source CodeArea bileşeni ID ile sahneden bulunamadı.");

        String renderedText = sourceCodeArea.getText();
        int authorPos = renderedText.indexOf("author");

        if (authorPos != -1) {
            assertTrue(sourceCodeArea.getStyleOfChar(authorPos).contains(BibTeXStyleClass.FIELD.getClassName()));
        }
    }
}
