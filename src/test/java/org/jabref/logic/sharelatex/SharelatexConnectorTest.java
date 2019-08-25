package org.jabref.logic.sharelatex;

import java.io.IOException;
import java.net.URISyntaxException;

import org.jabref.JabRefExecutorService;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class SharelatexConnectorTest {

    @Test
    public void testLogin() throws IOException {
        //Czrently
        SharelatexConnector connector = new SharelatexConnector();
        connector.connectToServer("http://overleaf.com", "developers@jabref.org", "jabref");

        // connector.uploadFile("591188ed98ba55690073c29e",Paths.get("X:\\Users\\CS\\Documents\\_JABREFTEMP\\aaaaaaaaaaaaaa.bib"));
        //   connector.uploadFileWithWebClient("591188ed98ba55690073c29e",
        //         Paths.get("X:\\Users\\CS\\Documents\\_JABREFTEMP\\aaaaaaaaaaaaaa.bib"));

        JabRefExecutorService.INSTANCE.executeAndWait(() -> {

            try {
                connector.startWebsocketListener("5936d96b1bd5906b0082f53c", new BibDatabaseContext(),
                                                 mock(ImportFormatPreferences.class), new DummyFileUpdateMonitor());
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        });
    }


}