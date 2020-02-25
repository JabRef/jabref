package org.jabref.gui.referencemetadata;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.websocket.JabRefWebsocketServer;
import org.jabref.websocket.WsAction;
import org.jabref.websocket.WsClientType;
import org.jabref.websocket.handlers.HandlerInfoGoogleScholarCitationCounts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;

public class ReferenceMetadataFetcherGoogleScholar {

    private static int STATIC_DWELL_TIME_AFTER_REQUEST = 0; // [ms] (0 ... disabled) can be used to prevent captchas (robot detection) or the response "too many requests"
    private static boolean ENABLE_RANDOM_DWELL_TIME = true; // can be used to prevent captchas (robot detection) or the response "too many requests"
    private static int UPPER_BOUND_RANDOM_DWELL_TIME_AFTER_REQUEST = 0; // [ms] (0 ...disabled) can be used to prevent captchas (robot detection) or the response "too many requests"
    private static int NUM_ENTRIES_PER_REQUEST = 1; // tuning factor; default: 1 (recommended, which allows fine grained dwell times between every requested entry)

    private static boolean UPDATE_NOTE_FIELD_WITH_CITATION_COUNT = true; // optional; default: false (since a dedicated field for the citation count exists)

    private static final AtomicInteger ATOMIC_INTEGER_DIALOG_RESULT = new AtomicInteger(); // result: 0 ... cancel, 1 ... retry

    public boolean fetchFor(BibDatabaseContext database, ObservableList<BibEntry> entries, DialogService dialogService) {

        JabRefWebsocketServer jabRefWebsocketServer = JabRefWebsocketServer.getInstance();

        while (true) {
            if (jabRefWebsocketServer.isWsClientWithGivenWsClientTypeRegistered(WsClientType.JABREF_BROWSER_EXTENSION)) {
                break;
            }
            else {
                System.out.println("JabRef cannot connect to the JabRef-Browser-Extension");

                int result = showCustomDialogAndWait(dialogService, Alert.AlertType.ERROR,
                        "JabRef-Browser-Extension Required",
                        "JabRef cannot connect to the JabRef-Browser-Extension.\n\nIn order to use this functionality, please make sure that a web browser is running, where the JabRef-Browser-Extension is installed. Furthermore, a Internet connection is required in order to fetch metadata online.",
                        "Retry",
                        "Cancel");

                if (result != 1) {
                    return false;
                }
            }
        }

        String databasePath = "";

        if (database.getDatabasePath().isPresent()) {
            databasePath = database.getDatabasePath().get().toString();
        } else {
            databasePath = "none_" + RandomStringUtils.random(20, true, true);
        }

        int startIndexEntriesBlock = 0;

        while (startIndexEntriesBlock < entries.size()) {
            // prepare request object
            JsonArray entriesArray = new JsonArray();

            for (int entryIndex = startIndexEntriesBlock; entryIndex - startIndexEntriesBlock < NUM_ENTRIES_PER_REQUEST && entryIndex < entries.size(); entryIndex++) {
                BibEntry entry = entries.get(entryIndex);

                String creatorsString = "";
                String creatorsType = "";

                creatorsString = entry.getField(StandardField.AUTHOR).orElse("").trim();

                if (creatorsString.length() != 0) {
                    creatorsType = "author";
                } else {
                    creatorsString = entry.getField(StandardField.EDITOR).orElse("").trim();

                    if (creatorsString.length() != 0) {
                        creatorsType = "editor";
                    } else {
                        creatorsType = "";
                    }
                }

                AuthorList authorList = AuthorList.parse(creatorsString);

                JsonArray creatorsArray = new JsonArray();

                for (Author author : authorList.getAuthors()) {
                    JsonObject creator = new JsonObject();
                    creator.addProperty("firstName", author.getFirst().orElse("").trim());
                    creator.addProperty("lastName", author.getLast().orElse("").trim());
                    creator.addProperty("creatorType", creatorsType);
                    creatorsArray.add(creator);
                }

                JsonObject entryObject = new JsonObject();
                // metadata
                entryObject.addProperty("_entryId", entry.getId());
                entryObject.addProperty("_lastEntry", entryIndex == entries.size() - 1);
                // entry data
                entryObject.addProperty("key", entry.getField(InternalField.KEY_FIELD).orElse("").trim());
                entryObject.addProperty("title", entry.getField(StandardField.TITLE).orElse("").trim());
                entryObject.addProperty("year", entry.getField(StandardField.YEAR).orElse("").trim());
                entryObject.addProperty("date", entry.getField(StandardField.DATE).orElse("").trim());
                entryObject.addProperty("DOI", entry.getField(StandardField.DOI).orElse("").trim());
                entryObject.addProperty("extra", entry.getField(StandardField.NOTE).orElse("").trim()); // send note field as extra field
                entryObject.add("creators", creatorsArray);

                entriesArray.add(entryObject);
            }

            JsonObject requestObject = new JsonObject();

            requestObject.addProperty("databasePath", databasePath);
            requestObject.add("entries", entriesArray);

            boolean retryFetchingMetadata = false;

            synchronized (HandlerInfoGoogleScholarCitationCounts.MESSAGE_SYNC_OBJECT) {
                // submit request object
                jabRefWebsocketServer.sendMessage(WsClientType.JABREF_BROWSER_EXTENSION, WsAction.CMD_FETCH_GOOGLE_SCHOLAR_CITATION_COUNTS, requestObject);

                // wait for response object
                try {
                    HandlerInfoGoogleScholarCitationCounts.MESSAGE_SYNC_OBJECT.wait();
                } catch (InterruptedException e) {
                }

                // process response object
                JsonObject rxMessagePayload = HandlerInfoGoogleScholarCitationCounts.getCurrentMessagePayload();

                String rxDatabasePath = rxMessagePayload.get("databasePath").getAsString();

                if (!databasePath.equals(rxDatabasePath)) {
                    System.out.println("databasePath of response does not match currently open database");
                    // info: Technically, the corresponding database could be searched as well, but since the progress
                    // dialog is open, no other action can be performed and thus no other database can be opened and
                    // and one cannot switch to a different database during this process. So this case should not happen
                    // anyway and everything is fine.

                    startIndexEntriesBlock += NUM_ENTRIES_PER_REQUEST; // next entries block, if any
                    continue;
                }

                JsonArray rxEntriesArray = rxMessagePayload.getAsJsonArray("entries");

                // process all entries of response object
                for (int rxEntryIndex = 0; rxEntryIndex < rxEntriesArray.size(); rxEntryIndex++) {
                    JsonObject rxEntryObject = rxEntriesArray.get(rxEntryIndex).getAsJsonObject();

                    // extract data
                    // - metadata
                    String _entryId = rxEntryObject.get("_entryId").getAsString();
                    boolean _lastEntry = rxEntryObject.get("_lastEntry").getAsBoolean();

                    JsonObject _status = rxEntryObject.getAsJsonObject("_status");

                    boolean _status_success = _status.get("success").getAsBoolean();
                    boolean _status_itemComplete = _status.get("itemComplete").getAsBoolean();
                    boolean _status_solvingCaptchaNeeded = _status.get("solvingCaptchaNeeded").getAsBoolean();
                    boolean _status_tooManyRequests = _status.get("tooManyRequests").getAsBoolean();

                    // - entry data
                    String key = rxEntryObject.get("key").getAsString();
                    String title = rxEntryObject.get("title").getAsString();
                    String creators = rxEntryObject.get("creators").getAsJsonArray().toString();
                    String note = rxEntryObject.get("extra").getAsString();
                    String citationCount = rxEntryObject.get("citationCount").getAsString();

                    System.out.println("success: " + _status_success);
                    System.out.println("itemComplete: " + _status_itemComplete);
                    System.out.println("solvingCaptchaNeeded: " + _status_solvingCaptchaNeeded);
                    System.out.println("tooManyRequests: " + _status_tooManyRequests);

                    if (!_status_itemComplete) {
                        System.out.println("Item incomplete: The citation count could not be determined, since the item data is incomplete.");

                        int result = showCustomDialogAndWait(dialogService, Alert.AlertType.INFORMATION,
                                "Item Incomplete",
                                "The reference metadata could not be determined for the following item, since its data is incomplete (title or authors are missing).\n\nItem Key: " + key + "\nItem Title: \"" + title + "\"\nItem Authors: \"" + creators + "\"",
                                "Skip and continue",
                                "Cancel");

                        if (result == 1) {
                            // skip and continue process
                        }
                        else {
                            return false; // cancel fetching metadata
                        }
                    }
                    else if (_status_solvingCaptchaNeeded) {
                        System.out.println("Captcha: The reference metadata could not be fetched. Please show Google Scholar, that you are not a robot, by opening this <a id=\"googleScholarCaptchaLink\" href=\"https://scholar.google.com/scholar?q=google\" target=\"_blank\">Google Scholar link</a> and solving the shown captcha.</li>");

                        int result = showCustomDialogAndWait(dialogService, Alert.AlertType.INFORMATION,
                                "Solving Capcha Needed",
                                "Please show Google Scholar, that you are not a robot, by opening this <a id=\"googleScholarCaptchaLink\" href=\"https://scholar.google.com/scholar?q=google\" target=\"_blank\">Google Scholar link</a> and solving the shown captcha, otherwise the reference metadata cannot be fetched.</li>",
                                "Continue",
                                "Cancel");

                        if (result == 1) {
                            retryFetchingMetadata = true; // retry fetching metadata
                            break;
                        }
                        else {
                            return false; // cancel fetching metadata
                        }
                    }
                    else if (_status_tooManyRequests) {
                        System.out.println("Too many requests: Google Scholar asks you to wait some time before sending further requests.");

                        int result = showCustomDialogAndWait(dialogService, Alert.AlertType.INFORMATION,
                                "Too Many Requests",
                                "Google Scholar asks you to wait some time before sending further requests.",
                                "Continue",
                                "Cancel");

                        if (result == 1) {
                            retryFetchingMetadata = true; // retry fetching metadata
                            break;
                        }
                        else {
                            return false; // cancel fetching metadata
                        }
                    }

                    // find entry to update
                    BibEntry entry = getBibEntryWithGivenEntryId(database, _entryId);

                    if (entry == null) {
                        System.out.println("skipping bibEntry, since entry with entryId=" + _entryId + " could not be found");

                        continue;
                    }

                    // set (updated) entry data (citation count)
                    entry.setField(SpecialField.CITATION_COUNT, citationCount);

                    if (UPDATE_NOTE_FIELD_WITH_CITATION_COUNT) {
                        entry.setField(StandardField.NOTE, note);
                    }
                }
            }

            if (ENABLE_RANDOM_DWELL_TIME) {
                Random randomNumberGenerator = new Random();
                int randomDwellTimeAfterRequest = randomNumberGenerator.nextInt(UPPER_BOUND_RANDOM_DWELL_TIME_AFTER_REQUEST + 1);

                if (randomDwellTimeAfterRequest > 0) {
                    try {
                        Thread.sleep(randomDwellTimeAfterRequest);
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (STATIC_DWELL_TIME_AFTER_REQUEST > 0) {
                try {
                    Thread.sleep(STATIC_DWELL_TIME_AFTER_REQUEST);
                } catch (InterruptedException e) {
                }
            }

            if (!retryFetchingMetadata) {
                startIndexEntriesBlock += NUM_ENTRIES_PER_REQUEST; // next entries block, if any
            }
        }

        return true;
    }

    private static BibEntry getBibEntryWithGivenEntryId(BibDatabaseContext database, String entryId) {
        for (BibEntry bibEntry : database.getEntries()) {
            if (bibEntry.getId().equals(entryId)) {
                return bibEntry;
            }
        }

        return null;
    }

    private int showCustomDialogAndWait(DialogService dialogService, Alert.AlertType alertType, String title, String content, String buttonNameYes, String ButtonNameCancelClose) {
        synchronized (ATOMIC_INTEGER_DIALOG_RESULT) {
            Platform.runLater(() -> {
                ButtonType buttonYes = new ButtonType(Localization.lang(buttonNameYes), ButtonBar.ButtonData.YES);
                ButtonType buttonCancelClose = new ButtonType(Localization.lang(ButtonNameCancelClose), ButtonBar.ButtonData.CANCEL_CLOSE);

                Optional<ButtonType> answer = dialogService.showCustomButtonDialogAndWait(alertType,
                        Localization.lang(title),
                        Localization.lang(content),
                        buttonYes,
                        buttonCancelClose);

                synchronized (ATOMIC_INTEGER_DIALOG_RESULT) {
                    if (answer.isPresent()) {
                        if (answer.get().equals(buttonYes)) {
                            ATOMIC_INTEGER_DIALOG_RESULT.set(1);
                        } else {
                            ATOMIC_INTEGER_DIALOG_RESULT.set(0);
                        }
                    } else {
                        ATOMIC_INTEGER_DIALOG_RESULT.set(0);
                    }

                    ATOMIC_INTEGER_DIALOG_RESULT.notify();
                }
            });

            try {
                ATOMIC_INTEGER_DIALOG_RESULT.wait();
            } catch (InterruptedException e) {
            }

            return ATOMIC_INTEGER_DIALOG_RESULT.get();
        }
    }
}
