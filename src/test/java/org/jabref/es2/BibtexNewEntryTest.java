package org.jabref.es2;

import java.io.IOException;
import java.io.StringWriter;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BibtexNewEntryTest {

    BibEntryWriter writer;

    @Before
    public void setUpWriter() {
        LatexFieldFormatterPreferences latexPrefs = new LatexFieldFormatterPreferences();
        writer = new BibEntryWriter(new LatexFieldFormatter(latexPrefs), true);
    }

    @Test
    public void testeTodosCamposVaziosArtigo() throws IOException {
        //        Instancia um novo escritor (o escritor cria uma string a partir de um buffer)
        StringWriter sw = new StringWriter();
        //        Instancia uma nova entrada
        BibEntry be = new BibEntry("Article");

        //        Escreve a entrada dada (BibEntry - be) usando o escritor dado (StringWriter - sw)
        writer.write(be, sw, BibDatabaseMode.BIBTEX);
        //        String que representa a entrada gerada pelo programa (No caso, o article gerado na forma de uma string)
        String stringGerada = sw.toString();

        //        String que representa a entrada esperada
        String stringEsperada = OS.NEWLINE + "@Article{," + OS.NEWLINE + "}" + OS.NEWLINE;
        //        Verifica se a entrada gerada e a entrada esperada são iguais
        assertEquals(stringEsperada, stringGerada);
    }

    @Test
    public void testeTodosCamposPreenchidosArtigo() throws IOException {
        //      Instancia um novo escritor (o escritor cria uma string a partir de um buffer)
        StringWriter sw = new StringWriter();
        //        Instancia uma nova entrada
        BibEntry be = new BibEntry("Article");

        //        Preenche os campos da entrada
        //        Campos requeridos
        be.setField("author", "author_teste");
        be.setField("title", "title_teste");
        be.setField("journal", "journal_teste");
        be.setField("year", "2017");
        be.setField("bibtexkey", "key_teste");
        //        Campos opcionais
        be.setField("volume", "1");
        be.setField("number", "1");
        be.setField("pages", "1");
        be.setField("month", "jan");
        be.setField("issn", "1");
        be.setField("note", "note_teste");
        be.setField("crossref", "crossref_teste");
        be.setField("keywords", "keywords_teste");
        be.setField("doi", "1");
        be.setField("url", "www.teste.com");
        be.setField("comment", "comment_teste");
        be.setField("owner", "owner_teste");
        be.setField("timestamp", "2017-07-09");
        be.setField("abstract", "abstract_test");
        be.setField("review", "very teste, much wow");

        //        Escreve a entrada dada (BibEntry - be) usando o escritor dado (StringWriter - sw)
        writer.write(be, sw, BibDatabaseMode.BIBTEX);
        //        String que representa a entrada gerada pelo programa (No caso, o article gerado na forma de uma string)
        String stringGerada = sw.toString();

        //      String que representa a saída esperada (formato pode ser visto na aba BibTex source)

        String stringEsperada = OS.NEWLINE + "@Article{key_teste," + OS.NEWLINE +
                "  author    = {author_teste}," + OS.NEWLINE +
                "  title     = {title_teste}," + OS.NEWLINE +
                "  journal   = {journal_teste}," + OS.NEWLINE +
                "  year      = {2017}," + OS.NEWLINE +
                "  volume    = {1}," + OS.NEWLINE +
                "  number    = {1}," + OS.NEWLINE +
                "  pages     = {1}," + OS.NEWLINE +
                "  month     = {jan}," + OS.NEWLINE +
                "  issn      = {1}," + OS.NEWLINE +
                "  note      = {note_teste}," + OS.NEWLINE +
                "  abstract  = {abstract_test}," + OS.NEWLINE +
                "  comment   = {comment_teste}," + OS.NEWLINE +
                "  crossref  = {crossref_teste}," + OS.NEWLINE +
                "  doi       = {1}," + OS.NEWLINE +
                "  keywords  = {keywords_teste}," + OS.NEWLINE +
                "  owner     = {owner_teste}," + OS.NEWLINE +
                "  review    = {very teste, much wow}," + OS.NEWLINE +
                "  timestamp = {2017-07-09}," + OS.NEWLINE +
                "  url       = {www.teste.com}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        //        Verifica se a entrada gerada e a entrada esperada são iguais
        assertEquals(stringEsperada, stringGerada);
    }

    @Test
    public void testeTodosCamposVaziosLivro() throws IOException {
        //        Instancia um novo escritor (o escritor cria uma string a partir de um buffer)
        StringWriter sw = new StringWriter();
        //        Instancia uma nova entrada
        BibEntry be = new BibEntry("Book");

        //        Escreve a entrada dada (BibEntry - be) usando o escritor dado (StringWriter - sw)
        writer.write(be, sw, BibDatabaseMode.BIBTEX);
        //        String que representa a entrada gerada pelo programa (No caso, o article gerado na forma de uma string)
        String stringGerada = sw.toString();

        //        String que representa a entrada esperada
        String stringEsperada = OS.NEWLINE + "@Book{," + OS.NEWLINE + "}" + OS.NEWLINE;
        //        Verifica se a entrada gerada e a entrada esperada são iguais
        assertEquals(stringEsperada, stringGerada);
    }

    @Test
    public void testeTodosCamposPreenchidosLivro() throws IOException {
        //      Instancia um novo escritor (o escritor cria uma string a partir de um buffer)
        StringWriter sw = new StringWriter();
        //        Instancia uma nova entrada
        BibEntry be = new BibEntry("Book");

        //        Preenche os campos da entrada
        //        Campos requeridos
        be.setField("title", "title_teste");
        be.setField("publisher", "publisher_teste");
        be.setField("year", "2017");
        be.setField("author", "author_teste");
        be.setField("editor", "editor_teste");
        be.setField("bibtexkey", "key_teste");
        //        Campos opcionais
        be.setField("volume", "1");
        be.setField("number", "1");
        be.setField("series", "1");
        be.setField("adress", "adress_teste");
        be.setField("edition", "1");
        be.setField("month", "jan");
        be.setField("isbn", "1");
        be.setField("note", "note_teste");
        be.setField("crossref", "crossref_teste");
        be.setField("keywords", "keywords_teste");
        be.setField("doi", "1");
        be.setField("url", "www.teste.com");
        be.setField("comment", "comment_teste");
        be.setField("owner", "owner_teste");
        be.setField("timestamp", "2017-07-09");
        be.setField("abstract", "abstract_test");
        be.setField("review", "very teste, much wow");

        //        Escreve a entrada dada (BibEntry - be) usando o escritor dado (StringWriter - sw)
        writer.write(be, sw, BibDatabaseMode.BIBTEX);
        //        String que representa a entrada gerada pelo programa (No caso, o article gerado na forma de uma string)
        String stringGerada = sw.toString();
        //      String que representa a saída esperada (formato pode ser visto na aba BibTex source)

        String stringEsperada = OS.NEWLINE + "@Book{key_teste," + OS.NEWLINE +
                "  title     = {title_teste}," + OS.NEWLINE +
                "  publisher = {publisher_teste}," + OS.NEWLINE +
                "  year      = {2017}," + OS.NEWLINE +
                "  author    = {author_teste}," + OS.NEWLINE +
                "  editor    = {editor_teste}," + OS.NEWLINE +
                "  volume    = {1}," + OS.NEWLINE +
                "  number    = {1}," + OS.NEWLINE +
                "  series    = {1}," + OS.NEWLINE +
                "  edition   = {1}," + OS.NEWLINE +
                "  month     = {jan}," + OS.NEWLINE +
                "  isbn      = {1}," + OS.NEWLINE +
                "  note      = {note_teste}," + OS.NEWLINE +
                "  abstract  = {abstract_test}," + OS.NEWLINE +
                "  adress    = {adress_teste}," + OS.NEWLINE +
                "  comment   = {comment_teste}," + OS.NEWLINE +
                "  crossref  = {crossref_teste}," + OS.NEWLINE +
                "  doi       = {1}," + OS.NEWLINE +
                "  keywords  = {keywords_teste}," + OS.NEWLINE +
                "  owner     = {owner_teste}," + OS.NEWLINE +
                "  review    = {very teste, much wow}," + OS.NEWLINE +
                "  timestamp = {2017-07-09}," + OS.NEWLINE +
                "  url       = {www.teste.com}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        //        Verifica se a entrada gerada e a entrada esperada são iguais
        assertEquals(stringEsperada, stringGerada);
    }
    
    //      Testes de Null Pointer Exception
    //          Artigos
    @Test(expected = NullPointerException.class)
    public void testAuthorNullArticle() {
        BibEntry be = new BibEntry("article");
        
        be.setField("author", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testTitleNullArticle() {
        BibEntry be = new BibEntry("article");
        
        be.setField("title", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testJournalNullArticle() {
        BibEntry be = new BibEntry("article");
        
        be.setField("journal", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testYearNullArticle() {
        BibEntry be = new BibEntry("article");
        
        be.setField("year", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testKeyNullArticle() {
        BibEntry be = new BibEntry("article");
        
        be.setField("bibtexkey", null);
        Assert.fail();
    }
    
    //          Livros
    @Test(expected = NullPointerException.class)
    public void testAuthorNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("author", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testTitleNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("title", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testJournalNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("journal", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testYearNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("year", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testKeyNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("bibtexkey", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testPublisherNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("publisher", null);
        Assert.fail();
    }
    
    @Test(expected = NullPointerException.class)
    public void testEditorNullBook() {
        BibEntry be = new BibEntry("book");
        
        be.setField("editor", null);
        Assert.fail();
    }
}
