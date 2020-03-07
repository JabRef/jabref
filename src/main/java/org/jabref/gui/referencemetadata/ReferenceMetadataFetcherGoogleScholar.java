package org.jabref.gui.referencemetadata;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import org.jabref.model.entry.field.StandardField;
import org.jabref.websocket.JabRefWebsocketServer;
import org.jabref.websocket.WebSocketAction;
import org.jabref.websocket.WebSocketClientType;
import org.jabref.websocket.handlers.HandlerInfoGoogleScholarCitationCounts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceMetadataFetcherGoogleScholar {

    private static int STATIC_DWELL_TIME_AFTER_REQUEST = 0; // [ms] (0 ... disabled) can be used to prevent captchas (robot detection) or the response "too many requests"
    private static boolean ENABLE_RANDOM_DWELL_TIME = true; // can be used to prevent captchas (robot detection) or the response "too many requests"
    private static int UPPER_BOUND_RANDOM_DWELL_TIME_AFTER_REQUEST = 0; // [ms] (0 ...disabled) can be used to prevent captchas (robot detection) or the response "too many requests"

    private static int NUM_ENTRIES_PER_REQUEST = 1; // tuning factor; default: 1 (recommended, which allows fine grained dwell times between every requested entry)

    private static boolean UPDATE_NOTE_FIELD_WITH_CITATION_COUNT = false; // optional; default: false (since a dedicated field for the citation count exists)
    private static boolean SHOW_EVERY_POTENTIALLY_INCOMPLETE_ENTRY_INTERACTIVELY = false; // default: false; note: not every incomplete item is actually really incomplete
    private static boolean SHOW_POTENTIALLY_INCOMPLETE_ENTRIES_AS_SUMMARY = true; // default: true; remark: not every incomplete item is actually really incomplete

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMetadataFetcherGoogleScholar.class);

    private static final AtomicInteger ATOMIC_INTEGER_DIALOG_RESULT = new AtomicInteger();

    private final ObservableList<BibEntry> entriesWithIncompleteMetadata = FXCollections.observableArrayList(); // this list contains all entries with still incomplete metadata

    /**
     * fetches reference metadata for the given entries
     *
     * @param database                   database from which the given <code>entries</code> come from
     * @param entries                    entries for which some reference metadata should be fetched
     * @param dialogService              dialog service which can be used for showing dialogs
     * @param fetchReferenceMetadataTask general task for fetching reference metadata
     * @return <code>false</code>if the the process has been completed successfully, <code>true</code> otherwise
     */
    public boolean fetchFor(BibDatabaseContext database, ObservableList<BibEntry> entries, DialogService dialogService, ExtendedTask<List<BibEntry>> fetchReferenceMetadataTask) {
        fetchReferenceMetadataTask.updateMessage("Fetching data from Google Scholar...");

        JabRefWebsocketServer jabRefWebsocketServer = JabRefWebsocketServer.getInstance();

        while (true) {
            if (jabRefWebsocketServer.isWebSocketClientWithGivenWebSocketClientTypeRegistered(WebSocketClientType.JABREF_BROWSER_EXTENSION)) {
                break;
            } else {
                LOGGER.warn("JabRef cannot connect to the JabRef-Browser-Extension");

                int result = showCustomDialogAndWait(dialogService, Alert.AlertType.ERROR,
                        "JabRef-Browser-Extension Required",
                        "JabRef cannot connect to the JabRef-Browser-Extension.\n\nIn order to use this " +
                                "functionality, please make sure that a web browser is running, where the " +
                                "JabRef-Browser-Extension is installed. Furthermore, a Internet connection is " +
                                "required in order to fetch metadata online.",
                        "Retry",
                        "Cancel");

                if (result != 1) {
                    entriesWithIncompleteMetadata.addAll(entries);
                    return true; // cancel fetching metadata
                }
            }
        }

        String databasePath = "";

        if (database.getDatabasePath().isPresent()) {
            databasePath = database.getDatabasePath().get().toString();
        } else {
            databasePath = "none_" + RandomStringUtils.random(20, true, true);
        }

        Set<IncompleteItem> potentiallyIncompleteItems = new HashSet<IncompleteItem>();

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
                jabRefWebsocketServer.sendMessage(WebSocketClientType.JABREF_BROWSER_EXTENSION,
                        WebSocketAction.CMD_FETCH_GOOGLE_SCHOLAR_CITATION_COUNTS, requestObject);

                // wait for response object
                try {
                    HandlerInfoGoogleScholarCitationCounts.MESSAGE_SYNC_OBJECT.wait();
                } catch (InterruptedException e) {
                }

                // process response object
                JsonObject rxMessagePayload = HandlerInfoGoogleScholarCitationCounts.getCurrentMessagePayload();

                String rxDatabasePath = rxMessagePayload.get("databasePath").getAsString();

                if (!databasePath.equals(rxDatabasePath)) {
                    LOGGER.warn("databasePath of response does not match currently open database");
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
                    JsonArray creators = rxEntryObject.get("creators").getAsJsonArray();
                    String note = rxEntryObject.get("extra").getAsString();
                    String citationCount = rxEntryObject.get("citationCount").getAsString();

                    LOGGER.debug("success: " + _status_success);
                    LOGGER.debug("itemComplete: " + _status_itemComplete);
                    LOGGER.debug("solvingCaptchaNeeded: " + _status_solvingCaptchaNeeded);
                    LOGGER.debug("tooManyRequests: " + _status_tooManyRequests);

                    if (!_status_itemComplete) {
                        LOGGER.info("item incomplete: the citation count could not be determined, since the " +
                                "item data is potentially incomplete");

                        potentiallyIncompleteItems.add(new IncompleteItem(key, title, creators));

                        entriesWithIncompleteMetadata.add(entries.get(startIndexEntriesBlock + rxEntryIndex));

                        if (SHOW_EVERY_POTENTIALLY_INCOMPLETE_ENTRY_INTERACTIVELY) {
                            int result = showCustomDialogAndWait(dialogService, Alert.AlertType.INFORMATION,
                                    "Potentially Incomplete Item Found",
                                    "No metadata could be fetched for the following reference, since it " +
                                            "doesn't have sufficient information for reliably fetching it (DOI or " +
                                            "title and author(s) are needed).\n\nCitation Key: \"" + key +
                                            "\"\nItem Title: \"" + title + "\"\n\nIn some cases, like references of " +
                                            "web pages, this is usually fine.",
                                    "Skip and continue",
                                    "Cancel");

                            if (result == 1) {
                                // skip and continue process
                            } else {
                                entriesWithIncompleteMetadata.addAll(ReferenceMetadataUtils.getAllEntriesStartingWithGivenIndex(startIndexEntriesBlock + rxEntryIndex, entries));
                                return true; // cancel fetching metadata
                            }
                        }
                    } else if (_status_solvingCaptchaNeeded) {
                        LOGGER.info("solving captcha needed: reference metadata could not be fetched, since " +
                                "solving the captcha is needed");

                        int result = showCustomDialogAndWait(dialogService, Alert.AlertType.INFORMATION,
                                "Solving Capcha Needed",
                                "Please show Google Scholar, that you are not a robot, by opening the link \n\n" +
                                        "https://scholar.google.com/scholar?q=google \n\nand solving the shown captcha, " +
                                        "otherwise the reference metadata cannot be fetched.",
                                "Continue",
                                "Cancel");

                        if (result == 1) {
                            retryFetchingMetadata = true; // retry fetching metadata
                            break;
                        } else {
                            entriesWithIncompleteMetadata.addAll(ReferenceMetadataUtils.getAllEntriesStartingWithGivenIndex(startIndexEntriesBlock + rxEntryIndex, entries));
                            return true; // cancel fetching metadata
                        }
                    } else if (_status_tooManyRequests) {
                        LOGGER.info("too many requests: Google Scholar asks you to wait some time before " +
                                "sending further requests");

                        int result = showCustomDialogAndWait(dialogService, Alert.AlertType.INFORMATION,
                                "Too Many Requests",
                                "Google Scholar asks you to wait some time before sending further requests.",
                                "Continue",
                                "Cancel");

                        if (result == 1) {
                            retryFetchingMetadata = true; // retry fetching metadata
                            break;
                        } else {
                            entriesWithIncompleteMetadata.addAll(ReferenceMetadataUtils.getAllEntriesStartingWithGivenIndex(startIndexEntriesBlock + rxEntryIndex, entries));
                            return true; // cancel fetching metadata
                        }
                    }

                    // find entry to update
                    BibEntry entry = getBibEntryWithGivenEntryId(database, _entryId);

                    if (entry == null) {
                        LOGGER.info("skipping bibEntry, since entry with entryId=" + _entryId + " could not " +
                                "be found");

                        continue;
                    }

                    // set (updated) entry data (citation count)
                    entry.setField(InternalField.CITATION_COUNT, citationCount);

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
                fetchReferenceMetadataTask.updateProgress(Math.min(startIndexEntriesBlock + 1, entries.size()), entries.size());

                startIndexEntriesBlock += NUM_ENTRIES_PER_REQUEST; // next entries block, if any
            }
        }

        if (SHOW_POTENTIALLY_INCOMPLETE_ENTRIES_AS_SUMMARY) {
            int numIncompleteItems = potentiallyIncompleteItems.size();

            if (numIncompleteItems > 0) {
                StringBuilder keysString = new StringBuilder();

                for (IncompleteItem incompleteItem : potentiallyIncompleteItems) {
                    if (keysString.length() != 0) {
                        keysString.append(", ");
                    }

                    keysString.append("\"" + incompleteItem.getKey() + "\"");
                }

                String content = "For " + numIncompleteItems + " " +
                        singularPluralChooser(numIncompleteItems, "reference", "references") +
                        " no metadata could be fetched, since " +
                        singularPluralChooser(numIncompleteItems, "it doesn't", "they don't") +
                        " have sufficient information for reliably fetching it (DOI or title and author(s) are needed).\n" +
                        "The citation " + singularPluralChooser(numIncompleteItems, "key", "keys") + " of the affected " +
                        singularPluralChooser(numIncompleteItems, "reference is", "references are") + ":\n\n" +
                        keysString.toString() + "\n\n" + "In some cases, like references of web pages, this is usually fine.\n";

                Platform.runLater(() -> {
                    dialogService.showInformationDialogAndWait(Localization.lang("Potentially Incomplete " +
                                    singularPluralChooser(numIncompleteItems, "Item", "Items") + " Found"),
                            Localization.lang(content));
                });
            }
        }

        return false;
    }

    private static BibEntry getBibEntryWithGivenEntryId(BibDatabaseContext database, String entryId) {
        for (BibEntry bibEntry : database.getEntries()) {
            if (bibEntry.getId().equals(entryId)) {
                return bibEntry;
            }
        }

        return null;
    }

    /**
     * @param dialogService         existing dialog service
     * @param alertType             type of alert
     * @param title                 title
     * @param content               description
     * @param buttonNameYes         button for confirmation/continuation
     * @param ButtonNameCancelClose button for cancelling/skipping
     * @return returns <code>0</code> for "cancel" and  <code>1</code> for "retry/continue"
     */
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

    public static String singularPluralChooser(int number, String singular, String plural) {
        if (number == 1) {
            return singular;
        } else {
            return plural;
        }
    }

    public ObservableList<BibEntry> getEntriesWithIncompleteMetadata() {
        return entriesWithIncompleteMetadata;
    }
}
