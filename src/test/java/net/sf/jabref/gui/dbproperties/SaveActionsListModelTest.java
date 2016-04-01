package net.sf.jabref.gui.dbproperties;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SaveActionsListModelTest {

    @Test
    public void resetFiresItemsChanged() throws Exception {
        SaveActionsListModel model = new SaveActionsListModel(Collections.emptyList());
        ListDataListener listener = mock(ListDataListener.class);
        model.addListDataListener(listener);
        FieldFormatterCleanups defaultFormatters = mock(FieldFormatterCleanups.class);

        model.reset(defaultFormatters);

        ArgumentCaptor<ListDataEvent> argument = ArgumentCaptor.forClass(ListDataEvent.class);
        verify(listener).contentsChanged(argument.capture());
        assertEquals(ListDataEvent.CONTENTS_CHANGED,argument.getValue().getType());
    }

    @Test
    public void resetSetsFormattersToPassedList() throws Exception {
        SaveActionsListModel model = new SaveActionsListModel(Collections.emptyList());
        FieldFormatterCleanups defaultFormatters = mock(FieldFormatterCleanups.class);
        List<FieldFormatterCleanup> formatters = Arrays.asList(new FieldFormatterCleanup("test", new ClearFormatter()));
        when(defaultFormatters.getConfiguredActions()).thenReturn(formatters);

        model.reset(defaultFormatters);

        assertEquals(formatters, model.getAllActions());
    }
}