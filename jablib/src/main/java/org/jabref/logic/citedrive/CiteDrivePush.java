package org.jabref.logic.citedrive;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CiteDrivePush {
    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDrivePush.class);

    /// [impl->req~citedrive.push~1]
    ///
    /// Uploads the entries as plain BibTeX: JabRef's metadata (local directories etc.) stays local.
    ///
    /// @param entries copies of the library's entries: writing applies save actions and key generation to them,
    ///                which must not touch the live library
    /// @return true if CiteDrive accepted the library
    public static boolean push(BibDatabaseContext context, List<BibEntry> entries, AccessToken accessToken, CliPreferences cliPreferences, NotificationService notificationService) throws IOException {
        return push(context, entries, accessToken, cliPreferences, notificationService, cliPreferences.getCiteDrivePreferences().getPushEndpoint());
    }

    static boolean push(BibDatabaseContext context, List<BibEntry> entries, AccessToken accessToken, CliPreferences cliPreferences, NotificationService notificationService, URI pushEndpoint) throws IOException {
        StringWriter writer = new StringWriter();
        SelfContainedSaveConfiguration exportConfiguration = cliPreferences.getSelfContainedExportConfiguration();
        SelfContainedSaveConfiguration plainBibtex = new SelfContainedSaveConfiguration(
                exportConfiguration.getSelfContainedSaveOrder(), false, BibDatabaseWriter.SaveType.PLAIN_BIBTEX, exportConfiguration.shouldReformatFile());
        new BibDatabaseWriter(
                new BibWriter(writer, context.getDatabase().getNewLineSeparator()),
                plainBibtex,
                cliPreferences.getFieldPreferences(),
                cliPreferences.getCitationKeyPatternPreferences(),
                cliPreferences.getCustomEntryTypesRepository())
                .writePartOfDatabase(context, entries);

        HttpResponse<String> httpResponse = Unirest.post(pushEndpoint.toASCIIString())
                                                   .header("Authorization", accessToken.toAuthorizationHeader())
                                                   .header("Content-Type", MediaTypes.APPLICATION_BIBTEX)
                                                   .body(writer.toString())
                                                   .asString();
        httpResponse.ifFailure(response -> {
            LOGGER.error("CiteDrive push failed: {} - {}", response.getStatus(), response.getBody());
            notificationService.notify(Localization.lang("CiteDrive push failed: %0", response.getStatus() + " " + response.getBody()));
        });
        httpResponse.ifSuccess(response -> {
            String body = response.getBody();
            String message;
            try {
                JSONObject jsonResponse = new JSONObject(body);
                int entryCount = jsonResponse.optInt("entry_count", 0);
                double fileSizeKb = jsonResponse.optDouble("file_size_kb", 0.0);

                if (entryCount > 0) {
                    message = Localization.lang("CiteDrive push succeeded: %0 entries (%1 KB)",
                            String.valueOf(entryCount),
                            String.valueOf(fileSizeKb));
                } else {
                    message = Localization.lang("CiteDrive push succeeded: %0 KB",
                            String.valueOf(fileSizeKb));
                }
            } catch (JSONException e) {
                LOGGER.warn("Could not parse CiteDrive push response: {}", body, e);
                message = Localization.lang("CiteDrive push succeeded.");
            }
            notificationService.notify(message);
        });
        return httpResponse.isSuccess();
    }
}
