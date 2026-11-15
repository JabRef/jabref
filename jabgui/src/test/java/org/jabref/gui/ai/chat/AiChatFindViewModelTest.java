package org.jabref.gui.ai.chat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    void viewModelExposesCurrentAndTotalMatches() {
        assertEquals(0, viewModel.getCurrent());
        assertEquals(3, viewModel.totalProperty().get());
    }

    // [utest->req~ai.chat.find.navigation~1]
    @Test
    void nextWrapsAround() {
        List<Integer> navigatedMatches = new ArrayList<>();
        viewModel.onNavigateProperty().set(_ -> navigatedMatches.add(viewModel.getCurrent()));

        viewModel.next();
        viewModel.next();
        viewModel.next();

        assertEquals(List.of(1, 2, 0), navigatedMatches);
        assertEquals(0, viewModel.getCurrent());
    }

    // [utest->req~ai.chat.find.navigation~1]
    @Test
    void previousWrapsAround() {
        List<Integer> navigatedMatches = new ArrayList<>();
        viewModel.onNavigateProperty().set(_ -> navigatedMatches.add(viewModel.getCurrent()));

        viewModel.previous();

        assertEquals(List.of(2), navigatedMatches);
        assertEquals(2, viewModel.getCurrent());
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
    }

    @Test
    void noOccurrencesShowsZero() {
        viewModel.setTotal(0);
        assertEquals(0, viewModel.totalProperty().get());
    }

    @Test
    void navigationWithoutOccurrencesDoesNotNotify() {
        List<Integer> navigatedMatches = new ArrayList<>();
        viewModel.onNavigateProperty().set(_ -> navigatedMatches.add(viewModel.getCurrent()));
        viewModel.setTotal(0);

        viewModel.next();
        viewModel.previous();

        assertEquals(List.of(), navigatedMatches);
        assertEquals(0, viewModel.getCurrent());
    }
}
