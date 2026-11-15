package org.jabref.gui.maintable;

import java.util.concurrent.atomic.AtomicReference;

import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class MainTableTooltipTest extends JavaFxTest {

    @Test
    void tooltipStaysVisibleWhileHovered() {
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);

        AtomicReference<MainTableTooltip> tooltip = new AtomicReference<>();
        interact(() -> tooltip.set(new MainTableTooltip(mock(DialogService.class), preferences, mock(TaskExecutor.class))));

        assertEquals(Duration.INDEFINITE, tooltip.get().getShowDuration());
    }

    @Test
    void configuredDurationAppliesWhenTooltipIsCreated() {
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getPreviewPreferences().getTooltipDuration()).thenReturn(30, 0);
        AtomicReference<MainTableTooltip> tooltip = new AtomicReference<>();
        interact(() -> {
            tooltip.set(new MainTableTooltip(mock(DialogService.class), preferences, mock(TaskExecutor.class)));
            tooltip.get().createTooltip(mock(BibDatabaseContext.class), mock(BibEntry.class), "value");
        });
        assertEquals(Duration.seconds(30), tooltip.get().getShowDuration());

        interact(() -> tooltip.get().createTooltip(mock(BibDatabaseContext.class), mock(BibEntry.class), "value"));
        assertEquals(Duration.INDEFINITE, tooltip.get().getShowDuration());
    }
}
