package org.jabref.gui.externalfiles;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportHandlerTest {

    @Test
    void handleBibTeXData() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        PreferencesService preferencesService = mock(PreferencesService.class);
        when(preferencesService.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferencesService.getFilePreferences()).thenReturn(mock(FilePreferences.class));

        ImportHandler importHandler = new ImportHandler(
                mock(BibDatabaseContext.class),
                preferencesService,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                mock(ImportFormatReader.class));

        List<BibEntry> bibEntries = importHandler.handleBibTeXData("""
                @InProceedings{Wen2013,
                  author           = {Tao Wen},
                  booktitle        = {45. Kraftwerkstechnisches Kolloquium},
                  library          = {Tagungen\\2013\\KWTK45\\},
                  title            = {Zünd- Und Abbrandverhalten Von Staubförmigen Brennstoffen},
                  year             = {2013},
                  comment          = {978-3-944310-04-06},
                  creationdate     = {2023-03-13T13:09:59},
                  file             = {:Tagungen/2013/KWTK45/Wen2013 - Zünd Und Abbrandverhalten Von Staubförmigen Brennstoffen.pdf:PDF},
                  keywords         = {zündverzug, kohle, braunkohle, lausitz, zündofen, experiment, verzögerung, partikeldurchmesser, zündhyperbel, holzhackschnitzel, torf, staub, steinkohle, getreidestaub, brennstofffeuchte, biomasse, fossile, flüchtige, volatile, FIELD-Rohr, drop-tube, model, vergleich, berechnung,},
                  modificationdate = {2023-03-13T13:17:17},
                  owner            = {JOJ},
                  url              = {https://fis.tu-dresden.de/portal/de/publications/zund-und-abbrandverhalten-von-staubfoermigen-brennstoffen(2f9dd517-794a-4cf5-9897-bc9eb6f125c6).html},
                }
                """);

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Tao Wen");

        assertEquals(List.of(expected), bibEntries);
        // when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        // when(stateManager.getActiveDatabase()).thenReturn(Optional.of(current));
    }
}
