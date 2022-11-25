package org.jabref.gui.commonfxcontrols;

import java.util.List;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrderConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveOrderConfigPanelViewModelTest {

    SortCriterionViewModel sortCriterionKey = new SortCriterionViewModel(new SaveOrderConfig.SortCriterion(StandardField.KEY, false));
    SortCriterionViewModel sortCriterionAuthor = new SortCriterionViewModel(new SaveOrderConfig.SortCriterion(StandardField.AUTHOR, false));
    SortCriterionViewModel sortCriterionTitle = new SortCriterionViewModel(new SaveOrderConfig.SortCriterion(StandardField.TITLE, true));

    SaveOrderConfigPanelViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new SaveOrderConfigPanelViewModel();
        viewModel.sortCriteriaProperty().addAll(List.of(sortCriterionKey, sortCriterionAuthor, sortCriterionTitle));
    }

    @Test
    void addCriterion() {
        viewModel.addCriterion();
        assertEquals(4, viewModel.sortCriteriaProperty().size());
    }

    @Test
    void removeCriterion() {
        SortCriterionViewModel unknownCriterion = new SortCriterionViewModel(new SaveOrderConfig.SortCriterion(StandardField.ABSTRACT, false));
        viewModel.removeCriterion(unknownCriterion);
        assertEquals(3, viewModel.sortCriteriaProperty().size());

        viewModel.removeCriterion(sortCriterionAuthor);
        assertEquals(2, viewModel.sortCriteriaProperty().size());
        assertEquals(List.of(sortCriterionKey, sortCriterionTitle), viewModel.sortCriteriaProperty());
    }

    @Test
    void moveCriterionUp() {
        viewModel.moveCriterionUp(sortCriterionTitle);
        assertEquals(List.of(sortCriterionKey, sortCriterionTitle, sortCriterionAuthor), viewModel.sortCriteriaProperty());

        viewModel.moveCriterionUp(sortCriterionTitle);
        assertEquals(List.of(sortCriterionTitle, sortCriterionKey, sortCriterionAuthor), viewModel.sortCriteriaProperty());

        viewModel.moveCriterionUp(sortCriterionTitle);
        assertEquals(List.of(sortCriterionTitle, sortCriterionKey, sortCriterionAuthor), viewModel.sortCriteriaProperty());
    }

    @Test
    void moveCriterionDown() {
        viewModel.moveCriterionDown(sortCriterionKey);
        assertEquals(List.of(sortCriterionAuthor, sortCriterionKey, sortCriterionTitle), viewModel.sortCriteriaProperty());

        viewModel.moveCriterionDown(sortCriterionKey);
        assertEquals(List.of(sortCriterionAuthor, sortCriterionTitle, sortCriterionKey), viewModel.sortCriteriaProperty());

        viewModel.moveCriterionDown(sortCriterionKey);
        assertEquals(List.of(sortCriterionAuthor, sortCriterionTitle, sortCriterionKey), viewModel.sortCriteriaProperty());
    }
}
