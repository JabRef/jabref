package org.jabref.es2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class ImportTest {

    BibtexImporter importer;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new BibtexImporter(Globals.prefs.getImportFormatPreferences());
    }

    @Test
    public void testeImportArquivo() throws IOException {
        //        Cria uma nova database
        BibDatabase database = new BibDatabase();
        //        Prepara o arquivo para importar
        InputStream stream = ImportTest.class.getResourceAsStream("testeImport.bib");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Globals.prefs.getDefaultEncoding()));
        //        Importa o arquivo para a database
        ParserResult pr = importer.importDatabase(reader);
        ImportFormatReader.UnknownFormatImport importResult = new ImportFormatReader.UnknownFormatImport(importer.getName(), pr);
        ParserResult pares = importResult.parserResult;
        database = pares.getDatabase();
        //        Pega a lista de entradas da database
        List<BibEntry> entries = database.getEntries();

        //        Verificação rápida se há apenas duas entradas (como consta no arquivo importado)
        assertEquals(2, entries.size());

        //        Verifica as duas entradas do arquivo importado estão de acordo após a importação
        for (BibEntry entry : entries) {
            if (entry.getCiteKey().equals("teste1")) {
                assertEquals(Optional.of("title_teste"), entry.getField("title"));
                assertEquals(Optional.of("publisher_teste"), entry.getField("publisher"));
                assertEquals(Optional.of("2017"), entry.getField("year"));
                assertEquals(Optional.of("author_teste"), entry.getField("author"));
                assertEquals(Optional.of("editor_teste"), entry.getField("editor"));
            } else if (entry.getCiteKey().equals("teste2")) {
                Optional<String> empty = Optional.empty();
                assertEquals(empty, entry.getField("title"));
                assertEquals(empty, entry.getField("publisher"));
                assertEquals(empty, entry.getField("year"));
                assertEquals(empty, entry.getField("author"));
                assertEquals(empty, entry.getField("editor"));
            }
        }

    }

}
