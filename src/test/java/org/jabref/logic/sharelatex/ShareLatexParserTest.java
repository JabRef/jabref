package org.jabref.logic.sharelatex;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.sharelatex.ShareLatexProject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShareLatexParserTest {

    private final ShareLatexParser parser = new ShareLatexParser();

    @Test
    public void testGetSharelatexProjects() {
        JsonParser jsonParser = new JsonParser();
        String jsonString = "{\"projects\":[{\"id\":\"58df8f5e27aa5281020536ea\",\"name\":\"PLoS one\",\"lastUpdated\":\"2017-06-07T08:42:14.632Z\",\"publicAccessLevel\":\"private\",\"accessLevel\":\"owner\",\"archived\":false,\"owner_ref\":\"58df8f3627aa5281020536e1\",\"owner\":{\"_id\":\"58df8f3627aa5281020536e1\",\"last_name\":\"\",\"first_name\":\"cschwentker\"}},{\"id\":\"5950e47621d8ee3e76616374\",\"name\":\"Example\",\"lastUpdated\":\"2017-07-16T09:49:16.241Z\",\"publicAccessLevel\":\"private\",\"accessLevel\":\"owner\",\"archived\":false,\"owner_ref\":\"58df8f3627aa5281020536e1\",\"owner\":{\"_id\":\"58df8f3627aa5281020536e1\",\"last_name\":\"\",\"first_name\":\"cschwentker\"}}],\"tags\":[{\"_id\":\"59353074a47d9c0eb124ed11\",\"user_id\":\"58df8f3627aa5281020536e1\",\"name\":\"TestFolder\",\"project_ids\":[]}],\"notifications\":[]}";
        JsonElement jsonTree = jsonParser.parse(jsonString);
        JsonObject obj = jsonTree.getAsJsonObject();

        List<ShareLatexProject> actual = parser.getProjectFromJson(obj);

        List<ShareLatexProject> expected = new ArrayList<>();
        ShareLatexProject project = new ShareLatexProject("58df8f5e27aa5281020536ea", "PLoS one", "cschwentker", "", "2017-06-07T08:42:14.632Z");
        expected.add(project);
        project = new ShareLatexProject("5950e47621d8ee3e76616374", "Example", "cschwentker", "", "2017-07-16T09:49:16.241Z");
        expected.add(project);

        assertEquals(expected, actual);

    }

    @Test
    public void testFixWrongUTF8IsoEncoded() {
        String wrongEncoded = "asdf_WrocÅawskiej";
        String rightEncoded = "asdf_Wrocławskiej";

        String actual = parser.fixUTF8Strings(wrongEncoded);
        assertEquals(rightEncoded, actual);
    }

    @Test
    public void parseBibTexString() {

        String message = "6:::78988+[null,[\"@book{adams1995hitchhiker,       \",\"   title={The Hitchhiker's Guide to the Galaxy},\",\"  author={Adams, D.},\",\"  isbn={9781417642595},\",\"  url={http://books.google.com/books?id=W-xMPgAACAAJ},\",\"  year={199},\",\"  publisher={San Val}\",\"}\",\"\"],74,[],{}]";
        String expected = "@book{adams1995hitchhiker,       \n" +
                "   title={The Hitchhiker's Guide to the Galaxy},\n" +
                "  author={Adams, D.},\n" +
                "  isbn={9781417642595},\n" +
                "  url={http://books.google.com/books?id=W-xMPgAACAAJ},\n" +
                "  year={199},\n" +
                "  publisher={San Val}\n" +
                "}\n" +
                "";

        String parsed = parser.getBibTexStringFromJsonMessage(message);
        assertEquals(expected, parsed);
    }

    @Test
    public void testgetDatabaseWithId() {
        String document = "6:::1+[null,{\"_id\":\"5909edaff31ff96200ef58dd\",\"name\":\"Test\",\"rootDoc_id\":\"5909edaff31ff96200ef58de\",\"rootFolder\":[{\"_id\":\"5909edaff31ff96200ef58dc\",\"name\":\"rootFolder\",\"folders\":[],\"fileRefs\":[{\"_id\":\"5909edb0f31ff96200ef58e0\",\"name\":\"universe.jpg\"},{\"_id\":\"59118cae98ba55690073c2a0\",\"name\":\"all2.ris\"}],\"docs\":[{\"_id\":\"5909edaff31ff96200ef58de\",\"name\":\"main.tex\"},{\"_id\":\"5909edb0f31ff96200ef58df\",\"name\":\"references.bib\"},{\"_id\":\"5911801698ba55690073c29c\",\"name\":\"aaaaaaaaaaaaaa.bib\"},{\"_id\":\"59368d551bd5906b0082f53a\",\"name\":\"aaaaaaaaaaaaaa (copy 1).bib\"}]}],\"publicAccesLevel\":\"private\",\"dropboxEnabled\":false,\"compiler\":\"pdflatex\",\"description\":\"\",\"spellCheckLanguage\":\"en\",\"deletedByExternalDataSource\":false,\"deletedDocs\":[],\"members\":[{\"_id\":\"5912e195a303b468002eaad0\",\"first_name\":\"jim\",\"last_name\":\"\",\"email\":\"jim@example.com\",\"privileges\":\"readAndWrite\",\"signUpDate\":\"2017-05-10T09:47:01.325Z\"}],\"invites\":[],\"owner\":{\"_id\":\"5909ed80761dc10a01f7abc0\",\"first_name\":\"joe\",\"last_name\":\"\",\"email\":\"joe@example.com\",\"privileges\":\"owner\",\"signUpDate\":\"2017-05-03T14:47:28.665Z\"},\"features\":{\"trackChanges\":true,\"references\":true,\"templates\":true,\"compileGroup\":\"standard\",\"compileTimeout\":180,\"github\":false,\"dropbox\":true,\"versioning\":true,\"collaborators\":-1,\"trackChangesVisible\":false}},\"owner\",2]";

        ShareLatexParser parser = new ShareLatexParser();
        String actual = parser.getFirstBibTexDatabaseId(document);
        String expected = "5909edb0f31ff96200ef58df";

        assertEquals(expected, actual);
    }

    @Test
    public void testgetVersionFromMessage() {
        String bibTexString = "6:::7+[null,[\"@book{adams1995hitchhiker,\",\"  title={The Hitchhiker's Guide to the Galaxy},\",\"  author={Adams, D.},\",\"  isbn={9781417642595},\",\"  url={http://books.google.com/books?id=W-xMPgAACAAJ},\",\"  year={1995}\",\"  publisher={San Val}\",\"}\",\"\"],5,[],{}]";

        int actual = parser.getVersionFromBibTexJsonString(bibTexString);
        assertEquals(5, actual);
    }

    @Test
    public void testGetErrorMessage() {
        String expected = "Delete component 'Åawskiej' does not match deleted text 'ławskiej}'";
        String errorMessageJson = "5:::{\"name\":\"otUpdateError\",\"args\":[\"Delete component 'Åawskiej' does not match deleted text 'ławskiej}'\",{\"project_id\":\"5936d96b1bd5906b0082f53c\",\"doc_id\":\"5936d96b1bd5906b0082f53e\",\"error\":\"Delete component 'Åawskiej' does not match deleted text 'ławskiej}'\"}]}";

        String actual = parser.getOtErrorMessageContent(errorMessageJson);
        assertEquals(expected, actual);

    }

    @Test
    public void testInsertNewText() {
        String before = "hello world";
        String after = "hello beautiful world";

        List<SharelatexDoc> docs = parser.generateDiffs(before, after);

        SharelatexDoc testDoc = new SharelatexDoc();
        testDoc.setContent("beautiful ");
        testDoc.setPosition(6);
        testDoc.setOperation("i");

        assertEquals(testDoc, docs.get(0));

    }

    @Test
    public void testShiftLaterInsertsByPreviousInserts() {
        String before = "the boy played with the ball";
        String after = "the tall boy played with the red ball";

        List<SharelatexDoc> expected = new ArrayList<>();
        SharelatexDoc testDoc = new SharelatexDoc();
        testDoc.setContent("tall ");
        testDoc.setPosition(4);
        testDoc.setOperation("i");
        expected.add(testDoc);

        SharelatexDoc testDoc2 = new SharelatexDoc();
        testDoc2.setContent("red ");
        testDoc2.setPosition(29);
        testDoc2.setOperation("i");

        expected.add(testDoc2);

        List<SharelatexDoc> docs = parser.generateDiffs(before, after);

        assertEquals(expected, docs);
    }

    @Test
    public void testDelete() {
        String before = "hello beautiful world";
        String after = "hello world";

        SharelatexDoc testdoc = new SharelatexDoc();
        testdoc.setContent("beautiful ");
        testdoc.setPosition(6);
        testdoc.setOperation("d");

        List<SharelatexDoc> docs = parser.generateDiffs(before, after);

        assertEquals(testdoc, docs.get(0));

    }

    @Test
    public void testShiftLaterDeleteByFirstDeletes() {

        String before = "the tall boy played with the red ball";
        String after = "the boy played with the ball";

        List<SharelatexDoc> expected = new ArrayList<>();
        SharelatexDoc testDoc = new SharelatexDoc();
        testDoc.setContent("tall ");
        testDoc.setPosition(4);
        testDoc.setOperation("d");
        expected.add(testDoc);

        SharelatexDoc testDoc2 = new SharelatexDoc();
        testDoc2.setContent("red ");
        testDoc2.setPosition(24);
        testDoc2.setOperation("d");
        expected.add(testDoc2);

        List<SharelatexDoc> docs = parser.generateDiffs(before, after);
        assertEquals(expected, docs);

    }

    @Test
    public void testBibTexString() {
        String bibTexBefore = "\n" +
                "@Testcase{Sam2007,\n" +
                "  year      = {2007},\n" +
                "  author    = {Sam And jason},\n" +
                "  file      = {:Huang2001 - Information Extraction from Voicemail.csv:csv},\n" +
                "  issue     = {3},\n" +
                "  journal   = {Wirtschaftsinformatik},\n" +
                "  keywords  = {software development processes; agile software development environments; time-to-market; Extreme Programming; Crystal methods family; Adaptive Software Development},\n" +
                "  language  = {english},\n" +
                "  mrnumber  = {0937-6429},\n" +
                "  owner     = {Christoph Schwentker},\n" +
                "  pages     = {237--248},\n" +
                "  publisher = {Gabler Verlag},\n" +
                "  timestamp = {2016.08.20},\n" +
                "  title     = {Agile Entwicklung Web-basierter Systeme},\n" +
                "  url       = {http://dx.doi.org/10.1007/BF03250842},\n" +
                "  volume    = {44},\n" +
                "}\n" +
                "";

        String bibtexAfter = "\n" +
                "@Testcase{Sam2007,\n" +
                "  year      = {2007},\n" +
                "  author    = {Sam And jason},\n" +
                "  file      = {:Huang2001 - Information Extraction from Voicemail.csv:csv},\n" +
                "  issue     = {3},\n" +
                "  journal   = {Test},\n" +
                "  keywords  = {software development processes; agile software development environments; time-to-market; Extreme Programming; Crystal methods family; Adaptive Software Development},\n" +
                "  language  = {english},\n" +
                "  mrnumber  = {0937-6429},\n" +
                "  owner     = {Christoph Schwentker},\n" +
                "  pages     = {237--248},\n" +
                "  publisher = {Gabler Verlag},\n" +
                "  timestamp = {2016.08.20},\n" +
                "  title     = {Agile Entwicklung Web-basierter Systeme},\n" +
                "  url       = {http://dx.doi.org/10.1007/BF03250842},\n" +
                "  volume    = {44},\n" +
                "}\n" +
                "";

        List<SharelatexDoc> expected = new ArrayList<>();
        SharelatexDoc testDoc = new SharelatexDoc();
        testDoc.setContent("Wirtschaftsinformatik");
        testDoc.setPosition(183);
        testDoc.setOperation("d");
        expected.add(testDoc);

        SharelatexDoc testDoc2 = new SharelatexDoc();
        testDoc2.setContent("Test");
        testDoc2.setPosition(183);
        testDoc2.setOperation("i");
        expected.add(testDoc2);

        List<SharelatexDoc> docs = parser.generateDiffs(bibTexBefore, bibtexAfter);
        assertEquals(expected, docs);

    }
}
