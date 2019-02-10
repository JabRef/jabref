package org.jabref.gui;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jabref.model.entry.BibEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateResolverDialogTest {

    @Test
    void givenNewDuplicateResolverDialogAndEmptyParameters_whenNothing_thenNoError() {
        Stage stage = new Stage();
        JabRefFrame frame = new JabRefFrame(stage);
        BibEntry one = new BibEntry();
        BibEntry two = new BibEntry();
        DuplicateResolverDialog.DuplicateResolverType type = DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK;

        DuplicateResolverDialog sut = new DuplicateResolverDialog(frame, one, two, type);
    }

    @Test
    void givenNewStage_whenNothing_thenNoError() {
        Stage sut = new Stage();

    }

    @Test
    void givenNewJabRefFrame_whenNothing_thenNoError() {
        Stage stage = new Stage();
        JabRefFrame sut = new JabRefFrame(stage);

    }

    @Test
    void givenNewBibEntry_whenNothing_thenNoError() {
        BibEntry sut = new BibEntry();

    }

    @Test
    void givenNewDuplicateResolverType_whenNothing_thenNoError() {
        DuplicateResolverDialog.DuplicateResolverType sut = DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK;
        assertEquals(DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK,sut);
    }
}
