package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Optional;

import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class RelatedArticlesTabTest extends JavaFxTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            , ,
            '', ,
            ' ', ,
            2024, , (2024)
            , 2024-05-17, (2024)
            2023, 2024-05-17, (2023)
            """)
    void displaysPublicationYearOnlyWhenPresent(@Nullable String year, @Nullable String date, @Nullable String expectedYear) {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Related article (2020)")
                .withField(StandardField.JOURNAL, "Example journal")
                .withField(StandardField.AUTHOR, "Ada Lovelace");
        Optional.ofNullable(year).ifPresent(value -> entry.withField(StandardField.YEAR, value));
        Optional.ofNullable(date).ifPresent(value -> entry.withField(StandardField.DATE, value));

        List<String> expectedYearTexts = Optional.ofNullable(expectedYear).stream().toList();

        interact(() -> {
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
            Parent content = (Parent) scrollPane.getContent();
            List<String> actualYearTexts = JavaFxExtension.lookupAll(content, "Text", Text.class).stream()
                                                          .map(Text::getText)
                                                          .filter(text -> text.startsWith("(") && text.endsWith(")"))
                                                          .toList();

            assertEquals(expectedYearTexts, actualYearTexts);
        });
    }
}
