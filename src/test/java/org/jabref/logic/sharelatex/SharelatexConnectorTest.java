package org.jabref.logic.sharelatex;

import java.net.URISyntaxException;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

public class SharelatexConnectorTest {

    //Czrently
    SharelatexConnector connector = new SharelatexConnector();
    connector.connectToServer("http://overleaf.com","developers@jabref.org","jabref");
    connector.getProjects();
    // connector.uploadFile("591188ed98ba55690073c29e",Paths.get("X:\\Users\\CS\\Documents\\_JABREFTEMP\\aaaaaaaaaaaaaa.bib"));
    //   connector.uploadFileWithWebClient("591188ed98ba55690073c29e",
    //         Paths.get("X:\\Users\\CS\\Documents\\_JABREFTEMP\\aaaaaaaaaaaaaa.bib"));

    JabRefExecutorService.INSTANCE.executeAndWait(() -> {

        try {
            connector.startWebsocketListener("5936d96b1bd5906b0082f53c", new BibDatabaseContext(),
                    mock(ImportFormatPreferences.class));
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    });


}