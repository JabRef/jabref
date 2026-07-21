package org.jabref.logic.openoffice;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationstyle.JabRefItemDataProvider;
import org.jabref.logic.openoffice.oocsltext.CSLCitationType;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.Date;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public record ZoteroReferenceMark(
        String name,
        List<String> citationKeys,
        List<Integer> citationNumbers,
        String uniqueId,
        CSLCitationType citationType) implements ReferenceMark {
    public static final String PREFIX = "ZOTERO_ITEM CSL_CITATION ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoteroReferenceMark.class);
    private static final Gson GSON = new Gson();
    private static final String JABREF_URI_PREFIX = "http://www.jabref.org/";
    private static final String ZOTERO_URI_PREFIX = "http://zotero.org/";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RANDOM_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String RANDOM_PREFIX = " RND";
    private static final int CITATION_ID_LENGTH = 8;
    private static final int RANDOM_LENGTH = 10;
    private static final Pattern RANDOM_SUFFIX_PATTERN = Pattern.compile(" RND[0-9A-Za-z]{" + RANDOM_LENGTH + "}$");
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("\\d+");

    public static boolean isZoteroReferenceMarkName(String name) {
        return name.startsWith(PREFIX);
    }

    /// Example: "ZOTERO_ITEM CSL_CITATION {json} RNDxxxxxxx"
    public static ZoteroReferenceMark buildReferenceMark(List<BibEntry> entries,
                                                         List<String> citationKeys,
                                                         List<Integer> citationNumbers,
                                                         int firstItemId,
                                                         CSLCitationType citationType,
                                                         BibDatabaseContext bibDatabaseContext,
                                                         BibEntryTypesManager entryTypesManager,
                                                         Map<String, String> zoteroUriByCitationKey) {
        String citationId = createRandomString(CITATION_ID_LENGTH);
        String suffix = createRandomSuffix();
        JabRefItemDataProvider itemDataProvider = new JabRefItemDataProvider();
        ZoteroCitationData citation = new ZoteroCitationData();
        citation.citationId = citationId;

        if (citationType == CSLCitationType.EMPTY) {
            citationType = CSLCitationType.NORMAL;
        }

        List<ZoteroCitationData.CitationItemData> citationItems = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String citationKey = citationKeys.get(i);
            String itemId = String.valueOf(firstItemId + i);
            ZoteroCitationData.ItemData itemData = GSON.fromJson(
                    itemDataProvider.toJson(entries.get(i), bibDatabaseContext, entryTypesManager),
                    ZoteroCitationData.ItemData.class);
            ZoteroCitationData.IssuedData issued = itemData.issued;
            if (issued != null &&
                    ((issued.dateParts == null) || issued.dateParts.isEmpty())) {
                String rawDate = issued.raw;
                if (rawDate != null && !StringUtil.isBlank(rawDate)) {
                    Date.parse(rawDate).ifPresent(date -> {
                        List<Object> dateParts = new ArrayList<>();
                        date.getYear().ifPresent(year -> dateParts.add(year.toString()));
                        date.getMonth().ifPresent(month -> dateParts.add(month.getNumber()));
                        date.getDay().ifPresent(dateParts::add);
                        if (!dateParts.isEmpty()) {
                            issued.dateParts = List.of(dateParts);
                            issued.raw = null;
                        }
                    });
                }
            }
            itemData.id = itemId;
            itemData.citationKey = citationKey;
            itemData.omitEmptyFields();

            ZoteroCitationData.CitationItemData citationItem = new ZoteroCitationData.CitationItemData();
            citationItem.id = itemId;
            citationItem.uris = createUris(citationKey, zoteroUriByCitationKey.getOrDefault(citationKey, ""));
            if (citationType == CSLCitationType.IN_TEXT) {
                citationItem.suppressAuthor = true;
            }
            citationItem.itemData = itemData;
            citationItems.add(citationItem);
        }
        citation.citationItems = citationItems;

        String referenceMarkName = PREFIX + GSON.toJson(citation) + " " + suffix;
        return new ZoteroReferenceMark(
                referenceMarkName,
                List.copyOf(citationKeys),
                List.copyOf(citationNumbers),
                suffix,
                citationType);
    }

    public static Optional<ZoteroReferenceMark> parse(String referenceMarkName) {
        ZoteroCitationData citation = getCitationData(referenceMarkName);
        List<String> citationKeys = new ArrayList<>();
        List<Integer> citationNumbers = new ArrayList<>();
        boolean suppressesAuthor = false;
        for (ZoteroCitationData.CitationItemData citationItem : Optional.ofNullable(citation.citationItems).orElse(List.of())) {
            getCitationKey(citationItem).ifPresent(citationKey -> {
                citationKeys.add(citationKey);
                citationNumbers.add(0);
            });
            suppressesAuthor = suppressesAuthor || Optional.ofNullable(citationItem.suppressAuthor).orElse(false);
        }

        if (citationKeys.isEmpty()) {
            return Optional.empty();
        }

        String uniqueId = getSuffix(referenceMarkName);

        CSLCitationType citationType = suppressesAuthor ? CSLCitationType.IN_TEXT : CSLCitationType.NORMAL;

        return Optional.of(new ZoteroReferenceMark(
                referenceMarkName,
                List.copyOf(citationKeys),
                List.copyOf(citationNumbers),
                uniqueId,
                citationType));
    }

    public static int getMaxItemId(String referenceMarkName) {
        int maxItemId = 0;
        for (ZoteroCitationData.CitationItemData citationItem : getCitationItems(referenceMarkName)) {
            if (!StringUtil.isBlank(citationItem.id) && ITEM_ID_PATTERN.matcher(citationItem.id).matches()) {
                maxItemId = Math.max(maxItemId, Integer.parseInt(citationItem.id));
            }
            if (citationItem.itemData != null &&
                    !StringUtil.isBlank(citationItem.itemData.id) &&
                    ITEM_ID_PATTERN.matcher(citationItem.itemData.id).matches()) {
                maxItemId = Math.max(maxItemId, Integer.parseInt(citationItem.itemData.id));
            }
        }
        return maxItemId;
    }

    private static List<String> createUris(String citationKey, String zoteroUri) {
        List<String> uris = new ArrayList<>();
        if (isZoteroUri(zoteroUri)) {
            uris.add(zoteroUri);
        }

        String jabRefUri = createJabRefUri(citationKey);
        if (!uris.contains(jabRefUri)) {
            uris.add(jabRefUri);
        }
        return List.copyOf(uris);
    }

    static String addJabRefUri(String referenceMarkName, int citationItemIndex, String jabRefUri) {
        try {
            JsonObject citation = JsonParser.parseString(getCSLJson(referenceMarkName)).getAsJsonObject();
            JsonArray citationItems = citation.getAsJsonArray("citationItems");

            JsonObject citationItem = citationItems.get(citationItemIndex).getAsJsonObject();

            JsonArray uris = citationItem.getAsJsonArray("uris");
            uris.add(jabRefUri);

            String suffix = getSuffix(referenceMarkName);
            return PREFIX + GSON.toJson(citation) + suffix;
        } catch (JsonParseException | IllegalStateException e) {
            LOGGER.debug("Could not add JabRef URI to Zotero reference mark at citation item index {}", citationItemIndex, e);
            return referenceMarkName;
        }
    }

    public static String updateCitationType(String referenceMarkName, CSLCitationType citationType) {
        if (citationType == CSLCitationType.EMPTY) {
            citationType = CSLCitationType.NORMAL;
        }

        ZoteroCitationData citation = getCitationData(referenceMarkName);
        for (ZoteroCitationData.CitationItemData citationItem : Optional.ofNullable(citation.citationItems).orElse(List.of())) {
            citationItem.suppressAuthor = citationType == CSLCitationType.IN_TEXT ? true : null;
        }

        String suffix = getSuffix(referenceMarkName);
        if (StringUtil.isBlank(suffix)) {
            suffix = createRandomSuffix();
        }
        return PREFIX + GSON.toJson(citation) + " " + suffix;
    }

    private static ZoteroCitationData getCitationData(String referenceMarkName) {
        return GSON.fromJson(getCSLJson(referenceMarkName), ZoteroCitationData.class);
    }

    public static String getCSLJson(String referenceMarkName) {
        int jsonStart = referenceMarkName.indexOf('{');
        int jsonEnd = referenceMarkName.lastIndexOf('}');
        return referenceMarkName.substring(jsonStart, jsonEnd + 1);
    }

    static List<ZoteroCitationData.CitationItemData> getCitationItems(String referenceMarkName) {
        return Optional.ofNullable(getCitationData(referenceMarkName).citationItems).orElse(List.of());
    }

    /// Create a map of "citationKey -> ZoteroUri".
    /// Next time JabRef cites the same citation, it will add ZoteroUri into citationItem.
    public static Map<String, String> extractZoteroUriByCitationKey(String referenceMarkName) {
        if (!isZoteroReferenceMarkName(referenceMarkName)) {
            return Map.of();
        }

        Map<String, String> zoteroUriByCitationKey = new HashMap<>();
        ZoteroCitationData citationData = getCitationData(referenceMarkName);

        for (ZoteroCitationData.CitationItemData citationItem : Optional.ofNullable(citationData.citationItems).orElse(List.of())) {
            Optional<String> extractedCitationKey = getCitationKey(citationItem);
            if (extractedCitationKey.isEmpty()) {
                continue;
            }

            String citationKey = extractedCitationKey.get();
            for (String uri : Optional.of(citationItem.uris).orElse(List.of())) {
                if (isZoteroUri(uri)) {
                    zoteroUriByCitationKey.putIfAbsent(citationKey, uri);
                    break;
                }
            }
        }
        return zoteroUriByCitationKey;
    }

    static Optional<String> getCitationKey(ZoteroCitationData.CitationItemData citationItem) {
        for (String uri : Optional.of(citationItem.uris).orElse(List.of())) {
            String citationKey = getCitationKeyFromJabRefUri(uri);
            if (!StringUtil.isBlank(citationKey)) {
                return Optional.of(citationKey);
            }
        }

        if (citationItem.itemData != null && !StringUtil.isBlank(citationItem.itemData.citationKey)) {
            return Optional.of(citationItem.itemData.citationKey);
        }

        if (citationItem.itemData != null && !StringUtil.isBlank(citationItem.itemData.id)) {
            return Optional.of(normalizeZoteroItemId(citationItem.itemData.id));
        }

        if (!StringUtil.isBlank(citationItem.id)) {
            return Optional.of(normalizeZoteroItemId(citationItem.id));
        }

        return Optional.empty();
    }

    static String createJabRefUri(String citationKey) {
        return JABREF_URI_PREFIX + citationKey;
    }

    static boolean isJabRefUri(String uri) {
        return uri.startsWith(JABREF_URI_PREFIX);
    }

    static boolean isZoteroUri(String uri) {
        return uri.startsWith(ZOTERO_URI_PREFIX);
    }

    static String getCitationKeyFromJabRefUri(String uri) {
        if (!isJabRefUri(uri)) {
            return "";
        }

        String citationKey = uri.substring(JABREF_URI_PREFIX.length());
        if (StringUtil.isBlank(citationKey)) {
            return "";
        }
        return citationKey;
    }

    private static String normalizeZoteroItemId(String itemId) {
        if (itemId.chars().allMatch(Character::isDigit)) {
            return "Zotero-" + itemId;
        }
        return itemId;
    }

    private static String getSuffix(String referenceMarkName) {
        Matcher matcher = RANDOM_SUFFIX_PATTERN.matcher(referenceMarkName);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group().trim();
    }

    static String createRandomSuffix() {
        return RANDOM_PREFIX.trim() + createRandomString(RANDOM_LENGTH);
    }

    private static String createRandomString(int length) {
        char[] randomString = new char[length];
        for (int i = 0; i < length; i++) {
            randomString[i] = RANDOM_CHARACTERS.charAt(RANDOM.nextInt(RANDOM_CHARACTERS.length()));
        }
        return String.valueOf(randomString);
    }
}
