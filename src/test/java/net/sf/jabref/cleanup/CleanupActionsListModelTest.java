package net.sf.jabref.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.gui.cleanup.CleanupActionsListModel;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CleanupActionsListModelTest {

    @Test
    public void resetFiresItemsChanged() throws Exception {
        CleanupActionsListModel model = new CleanupActionsListModel(Collections.emptyList());
        ListDataListener listener = mock(ListDataListener.class);
        model.addListDataListener(listener);
        FieldFormatterCleanups defaultFormatters = mock(FieldFormatterCleanups.class);

        model.reset(defaultFormatters);

        ArgumentCaptor<ListDataEvent> argument = ArgumentCaptor.forClass(ListDataEvent.class);
        verify(listener).contentsChanged(argument.capture());
        assertEquals(ListDataEvent.CONTENTS_CHANGED, argument.getValue().getType());
    }

    @Test
    public void resetSetsFormattersToPassedList() throws Exception {
        CleanupActionsListModel model = new CleanupActionsListModel(Collections.emptyList());
        FieldFormatterCleanups defaultFormatters = mock(FieldFormatterCleanups.class);
        List<FieldFormatterCleanup> formatters = Arrays.asList(new FieldFormatterCleanup("test", new ClearFormatter()));
        when(defaultFormatters.getConfiguredActions()).thenReturn(formatters);

        model.reset(defaultFormatters);

        assertEquals(formatters, model.getAllActions());
    }

    public List<FieldFormatterCleanup> getDefaultFieldFormatterCleanups() {
        FieldFormatterCleanups formatters = FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS;
        //new ArrayList because configured actions is an unmodifiable collection
        return new ArrayList<>(formatters.getConfiguredActions());
    }

    @Test
    public void removedAtIndexOkay() {

        CleanupActionsListModel model = new CleanupActionsListModel(getDefaultFieldFormatterCleanups());
        ListDataListener listener = mock(ListDataListener.class);
        model.addListDataListener(listener);
        model.removeAtIndex(0);

        ArgumentCaptor<ListDataEvent> argument = ArgumentCaptor.forClass(ListDataEvent.class);
        verify(listener).intervalRemoved(argument.capture());
        assertEquals(ListDataEvent.INTERVAL_REMOVED, argument.getValue().getType());

    }

    @Test
    public void removedAtIndexMinus1DoesNothing() {

        CleanupActionsListModel model = new CleanupActionsListModel(getDefaultFieldFormatterCleanups());
        ListDataListener listener = mock(ListDataListener.class);
        model.addListDataListener(listener);
        model.removeAtIndex(-1);

        verifyZeroInteractions(listener);
    }

    @Test
    public void removedAtIndexgreaterListSizeDoesNothing() {

        CleanupActionsListModel model = new CleanupActionsListModel(getDefaultFieldFormatterCleanups());
        ListDataListener listener = mock(ListDataListener.class);
        model.addListDataListener(listener);
        model.removeAtIndex((getDefaultFieldFormatterCleanups().size() + 1));

        verifyZeroInteractions(listener);

    }

}