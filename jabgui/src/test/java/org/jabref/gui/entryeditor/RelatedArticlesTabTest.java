package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class RelatedArticlesTabTest extends JavaFxTest {

    @Test
    void getRelatedArticleInfoDoesNotAddPublicationYearWhenYearIsAbsent() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Related article")
                .withField(StandardField.AUTHOR, "Ada Lovelace");

        assertEquals(List.of(), publicationYearTextsFor(entry));
    }

    @Test
    void getRelatedArticleInfoDoesNotAddPublicationYearWhenYearIsBlank() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Related article")
                .withField(StandardField.AUTHOR, "Ada Lovelace")
                .withField(StandardField.YEAR, " ");

        assertEquals(List.of(), publicationYearTextsFor(entry));
    }

    @Test
    void getRelatedArticleInfoAddsPublicationYearWhenYearIsPresent() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Related article")
                .withField(StandardField.AUTHOR, "Ada Lovelace")
                .withField(StandardField.YEAR, "2024");

        assertEquals(List.of("(2024)"), publicationYearTextsFor(entry));
    }

    private List<String> publicationYearTextsFor(BibEntry entry) {
        AtomicReference<List<String>> publicationYearTexts = new AtomicReference<>(List.of());
        JavaFxExtension.invokeAndWait(() -> {
            RelatedArticlesTab tab = new RelatedArticlesTab(
                    mock(BuildInfo.class),
                    mock(GuiPreferences.class),
                    mock(DialogService.class),
                    mock(StateManager.class),
                    mock(TaskExecutor.class));
            MrDLibFetcher fetcher = mock(MrDLibFetcher.class);
            when(fetcher.getHeading()).thenReturn("Related articles");
            when(fetcher.getDescription()).thenReturn("Recommendations");

            ScrollPane scrollPane = tab.getRelatedArticleInfo(List.of(entry), fetcher);

            HBox relatedArticleRow = (HBox) ((VBox) scrollPane.getContent()).getChildren().get(2);
            publicationYearTexts.set(relatedArticleRow.getChildren().stream()
                                                      .filter(Text.class::isInstance)
                                                      .map(Text.class::cast)
                                                      .map(Text::getText)
                                                      .filter(text -> text.startsWith("(") && text.endsWith(")"))
                                                      .toList());
        });
        return publicationYearTexts.get();
    }
}
