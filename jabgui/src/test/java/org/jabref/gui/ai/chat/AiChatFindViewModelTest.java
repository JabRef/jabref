package org.jabref.gui.ai.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// [utest->feat~ai.chat.find~1]
class AiChatFindViewModelTest {

    private AiChatFindViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AiChatFindViewModel();
        viewModel.visibleProperty().set(true);
        viewModel.queryProperty().set("co");
        viewModel.setTotal(3);
    }

    @Test
    void resultTextShowsCurrentOfTotal() {
        assertEquals("1/3", viewModel.resultTextProperty().get());
    }

    @Test
    void nextWrapsAround() {
        viewModel.next();
        viewModel.next();
        viewModel.next();
        assertEquals(0, viewModel.getCurrent());
    }

    @Test
    void previousWrapsAround() {
        viewModel.previous();
        assertEquals("3/3", viewModel.resultTextProperty().get());
    }

    @Test
    void changingQueryResetsCurrent() {
        viewModel.next();
        viewModel.queryProperty().set("cho");
        assertEquals(0, viewModel.getCurrent());
    }

    @Test
    void shrinkingTotalResetsCurrent() {
        viewModel.previous();
        assertFalse(viewModel.setTotal(1));
        assertEquals(0, viewModel.getCurrent());
    }

    @Test
    void hiddenBarHasNoActiveQuery() {
        viewModel.visibleProperty().set(false);
        assertEquals("", viewModel.getActiveQuery());
        assertEquals("", viewModel.resultTextProperty().get());
    }

    @Test
    void noOccurrencesShowsZero() {
        viewModel.setTotal(0);
        assertEquals("0/0", viewModel.resultTextProperty().get());
    }
}
