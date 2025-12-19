package org.jabref.logic.citedrive;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.database.BibDatabaseContext;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiteDrivePush {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDrivePush.class);

    public static void push(BibDatabaseContext context, AccessToken accessToken, CliPreferences cliPreferences, NotificationService notificationService) throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        // bigger buffer helps avoid producer/consumer stalls
        PipedInputStream pis = new PipedInputStream(pos, 256 * 1024);

        Thread.startVirtualThread(() -> {
            try (pos; Writer writer = new OutputStreamWriter(pos, StandardCharsets.UTF_8)) {
                BibDatabaseWriter bibDatabaseWriter = new BibDatabaseWriter(writer, context, cliPreferences);
                bibDatabaseWriter.writeDatabase(context);
                writer.flush();
            } catch (Exception e) {
                LOGGER.error("Error writing BibTeX data for CiteDrive push", e);
            }
        });
        HttpResponse<String> httpResponse = Unirest.post("https://api-dev.citedrive.com/jabref/push/")
                                                   .header("Authorization", accessToken.toAuthorizationHeader())
                                                   .header("Content-Type", MediaTypes.APPLICATION_BIBTEX)
                                                   .body(pis)
                                                   .asString();
        httpResponse.ifFailure(response -> {
            LOGGER.error("CiteDrive push failed: {}", response);
            notificationService.notify(Localization.lang("CiteDrive push failed: %0", response.getStatus() + " " + response.getBody()));
        });
        httpResponse.ifSuccess(_ -> notificationService.notify(Localization.lang("CiteDrive push succeeded.")));
    }
}
