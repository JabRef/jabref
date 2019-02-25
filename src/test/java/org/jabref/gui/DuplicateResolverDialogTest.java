package org.jabref.gui;

import org.jabref.model.entry.BibEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
//trying to find how this is linked to BibTexKey Duplicate Check throws NPE Bug#4614
class DuplicateResolverDialogTest {

    /**@Test
    void givenNewDuplicateResolverDialogAndEmptyParameters_whenInitializing_thenNoError() {
        Stage stage = new Stage();
        JabRefFrame frame = new JabRefFrame(stage);
        BibEntry one = new BibEntry();
        BibEntry two = new BibEntry();
        DuplicateResolverDialog.DuplicateResolverType type = DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK;

        DuplicateResolverDialog sut = new DuplicateResolverDialog(frame, one, two, type);
    }

    @Test
    void givenUninitializedStage_whenInitializing_thenNoError() {
        Stage sut;

    }

    @Test
    void givenInitializedStage_whenInitializing_thenNoError() {
        Stage sut = new Stage(StageStyle.DECORATED);

    }

    @Test
    void givenNewJabRefFrame_whenInitializing_thenNoError() {
        Stage stage = new Stage();
        JabRefFrame sut = new JabRefFrame(stage);

    }*/

    @Test
    void givenNewBibEntry_whenInitializing_thenNoError() {
        BibEntry sut = new BibEntry();

    }

    @Test
    void givenNewDuplicateResolverTypeIMPORT_CHECK_whenInitializing_thenEqualsIMPORT_CHECK() {
        DuplicateResolverDialog.DuplicateResolverType sut = DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK;
        assertEquals(DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK,sut);
    }

    @Test
    void givenNewDuplicateResolverResultKEEP_LEFT_whenInitializing_thenEqualsIMPORT_CHECK() {
        DuplicateResolverDialog.DuplicateResolverResult sut = DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT;
        assertEquals(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT,sut);
    }

    /**@Test
    public void givenDuplicateResolverDialog_whenGetMergedEntry_thenMergedBibEntryMeReturned(){
        Stage stage = new Stage();
        JabRefFrame frame = new JabRefFrame(stage);
        BibEntry one = new BibEntry();
        BibEntry two = new BibEntry();
        DuplicateResolverDialog.DuplicateResolverType type = DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK;

        DuplicateResolverDialog testDialog = new DuplicateResolverDialog(frame, one, two, type);

        BibEntry sut = testDialog.getMergedEntry();
        BibDatabaseMode mode = BibDatabaseMode.BIBTEX;

        MergeEntries ans = new MergeEntries(one, two, mode);
        assertEquals(ans, sut);
    }*/
}
