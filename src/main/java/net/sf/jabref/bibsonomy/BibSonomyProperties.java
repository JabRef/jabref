package net.sf.jabref.bibsonomy;

import java.util.Optional;
import java.util.Properties;

import net.sf.jabref.preferences.JabRefPreferences;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.enums.Order;

/**
 * read and write the plugin properties file.
 */
public class BibSonomyProperties extends Properties {

    //API properties
    private static final String API_URL = "api.url";
    private static final String API_USERNAME = "api.username";
    private static final String API_KEY = "api.key";

    //BibSonomy properties
    private static final String BIBSONOMY_SAVE_API_KEY = "bibsonomy.saveapikey";
    private static final String BIBSONOMY_DOCUMENTS_IMPORT = "bibsonomy.documents.import";
    private static final String BIBSONOMY_DOCUMENTS_EXPORT = "bibsonomy.documents.export";
    private static final String BIBSONOMY_TAGS_REFRESH_ON_STARTUP = "bibsonomy.tags.refreshonstartup";
    private static final String BIBSONOMY_TAGS_IGNORE_NO_TAGS = "bibsonomy.tags.ignorenotags";
    private static final String BIBSONOMY_NUMBER_OF_POSTS_PER_REQUEST = "bibsonomy.request.size";
    private static final String BIBSONOMY_IGNORE_WARNING_MORE_POSTS = "bibsonomy.request.size.ignorewarning";
    private static final String BIBSONOMY_EXTRA_TAB_FIELDS = "bibsonomy.tabs.extra";
    private static final String BIBSONOMY_VISIBILITY = "bibsonomy.visibilty";
    private static final String BIBSONOMY_TAG_CLOUD_SIZE = "bibsonomy.tagcloud.size";
    private static final String BIBSONOMY_SIDE_PANE_VISIBILITY_TYPE = "bibsonomy.sidepane.visibility.type";
    private static final String BIBSONOMY_SIDE_PANE_VISIBILITY_NAME = "bibsonomy.sidepane.visibility.name";
    private static final String BIBSONOMY_TAG_CLOUD_ORDER = "bibsonomy.tagcloud.order";

    //Array containing all property constants
    private static final String[] propsArray = {API_URL, API_USERNAME, API_KEY, BIBSONOMY_SAVE_API_KEY, BIBSONOMY_DOCUMENTS_IMPORT, BIBSONOMY_DOCUMENTS_EXPORT, BIBSONOMY_TAGS_REFRESH_ON_STARTUP, BIBSONOMY_TAGS_IGNORE_NO_TAGS, BIBSONOMY_NUMBER_OF_POSTS_PER_REQUEST, BIBSONOMY_IGNORE_WARNING_MORE_POSTS, BIBSONOMY_EXTRA_TAB_FIELDS, BIBSONOMY_VISIBILITY, BIBSONOMY_TAG_CLOUD_SIZE, BIBSONOMY_SIDE_PANE_VISIBILITY_TYPE, BIBSONOMY_SIDE_PANE_VISIBILITY_NAME, BIBSONOMY_TAG_CLOUD_ORDER};


    private static BibSonomyProperties INSTANCE;

    public static BibSonomyProperties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadPropertiesFromJabRefPreferences(JabRefPreferences.getInstance());
        }
        return INSTANCE;
    }

    private static BibSonomyProperties loadPropertiesFromJabRefPreferences(JabRefPreferences preferences) {
        Optional<String> prefsOpt = Optional.ofNullable(preferences.get(JabRefPreferences.BIBSONOMY_PROPERTIES));

        if (!prefsOpt.isPresent()) {
            return new BibSonomyProperties();
        }

        BibSonomyProperties bibSonomyProperties = new BibSonomyProperties();

        String prefs = prefsOpt.get();
        for (String property : propsArray) {
            if (prefs.contains(property)) {
                int lastIndexOf = prefs.lastIndexOf(property + "=") + property.length() + 1;
                String propertyValue;

                if (prefs.indexOf(",", lastIndexOf) >= 0) {
                    propertyValue = prefs.substring(lastIndexOf, prefs.indexOf(",", lastIndexOf));
                } else {
                    propertyValue = prefs.substring(lastIndexOf, prefs.indexOf("}", lastIndexOf));
                }
                bibSonomyProperties.setProperty(property, propertyValue);
            }
        }

        return bibSonomyProperties;
    }

    private BibSonomyProperties() {
    }

    /**
     * Saves the properties and takes care of the "do save API key" option
     */
    public static void save() {
        String apiKey = getApiKey();
        if (!getStoreApiKey()) {
            // if the key shoujld not be stored in the preferences, store an empty key in the preferences.
            setApiKey("");
        }

        JabRefPreferences preferences = JabRefPreferences.getInstance();
        preferences.clear(JabRefPreferences.BIBSONOMY_PROPERTIES);
        preferences.put(JabRefPreferences.BIBSONOMY_PROPERTIES, INSTANCE.toString());

        setApiKey(apiKey);
    }

    public static boolean ignoreNoTagsAssigned() {
        return Boolean.valueOf(getInstance().getProperty(BIBSONOMY_TAGS_IGNORE_NO_TAGS, "false"));
    }

    public static String getUsername() {
        return getInstance().getProperty(API_USERNAME, BibSonomyGlobals.API_USERNAME);
    }

    public static String getApiKey() {
        return getInstance().getProperty(API_KEY, BibSonomyGlobals.API_KEY);
    }

    public static String getApiUrl() {
        return getInstance().getProperty(API_URL, BibSonomyGlobals.API_URL);
    }

    public static boolean getDownloadDocumentsOnImport() {
        return Boolean.parseBoolean(getInstance().getProperty(BIBSONOMY_DOCUMENTS_IMPORT, "true"));
    }

    public static int getNumberOfPostsPerRequest() {
        return Integer.parseInt(getInstance().getProperty(BIBSONOMY_NUMBER_OF_POSTS_PER_REQUEST, BibSonomyGlobals.BIBSONOMY_NUMBER_OF_POSTS_PER_REQUEST));
    }

    public static boolean getIgnoreMorePostsWarning() {
        return Boolean.parseBoolean(getInstance().getProperty(BIBSONOMY_IGNORE_WARNING_MORE_POSTS, "false"));
    }

    public static String getExtraTabFields() {
        return getInstance().getProperty(BIBSONOMY_EXTRA_TAB_FIELDS, "issn;isbn");
    }

    public static String getDefaultVisibilty() {
        return getInstance().getProperty(BIBSONOMY_VISIBILITY, "public");
    }

    public static boolean getStoreApiKey() {
        return Boolean.parseBoolean(getInstance().getProperty(BIBSONOMY_SAVE_API_KEY, "true"));
    }

    public static boolean getUpdateTagsOnStartUp() {
        return Boolean.parseBoolean(getInstance().getProperty(BIBSONOMY_TAGS_REFRESH_ON_STARTUP, "false"));
    }

    public static boolean getUploadDocumentsOnExport() {
        return Boolean.parseBoolean(getInstance().getProperty(BIBSONOMY_DOCUMENTS_EXPORT, "true"));
    }

    public static int getTagCloudSize() {
        return Integer.parseInt(getInstance().getProperty(BIBSONOMY_TAG_CLOUD_SIZE, "100"));
    }

    public static void setUsername(String text) {
        getInstance().setProperty(API_USERNAME, text);
    }

    public static void setApiKey(String text) {
        getInstance().setProperty(API_KEY, text);
    }

    public static void setStoreApiKey(boolean selected) {
        getInstance().setProperty(BIBSONOMY_SAVE_API_KEY, String.valueOf(selected));
    }

    public static void setNumberOfPostsPerRequest(int value) {
        getInstance().setProperty(BIBSONOMY_NUMBER_OF_POSTS_PER_REQUEST, String.valueOf(value));
    }

    public static void setTagCloudSize(int value) {
        getInstance().setProperty(BIBSONOMY_TAG_CLOUD_SIZE, String.valueOf(value));
    }

    public static void setIgnoreNoTagsAssigned(boolean selected) {
        getInstance().setProperty(BIBSONOMY_TAGS_IGNORE_NO_TAGS, String.valueOf(selected));
    }

    public static void setUpdateTagsOnStartup(boolean selected) {
        getInstance().setProperty(BIBSONOMY_TAGS_REFRESH_ON_STARTUP, String.valueOf(selected));
    }

    public static void setUploadDocumentsOnExport(boolean selected) {
        getInstance().setProperty(BIBSONOMY_DOCUMENTS_EXPORT, String.valueOf(selected));
    }

    public static void setDownloadDocumentsOnImport(boolean selected) {
        getInstance().setProperty(BIBSONOMY_DOCUMENTS_IMPORT, String.valueOf(selected));
    }

    public static void setDefaultVisisbility(String key) {
        getInstance().setProperty(BIBSONOMY_VISIBILITY, key);
    }

    public static void setIgnoreMorePostsWarning(boolean selected) {
        getInstance().setProperty(BIBSONOMY_IGNORE_WARNING_MORE_POSTS, String.valueOf(selected));
    }

    public static void setExtraFields(String text) {
        getInstance().setProperty(BIBSONOMY_EXTRA_TAB_FIELDS, text);
    }

    public static GroupingEntity getSidePaneVisibilityType() {
        return GroupingEntity.getGroupingEntity(getInstance().getProperty(BIBSONOMY_SIDE_PANE_VISIBILITY_TYPE, "ALL"));
    }

    public static String getSidePaneVisibilityName() {
        return getInstance().getProperty(BIBSONOMY_SIDE_PANE_VISIBILITY_NAME, "all users");
    }

    public static void setSidePaneVisibilityType(GroupingEntity entity) {
        getInstance().setProperty(BIBSONOMY_SIDE_PANE_VISIBILITY_TYPE, entity.toString());
    }

    public static void setSidePaneVisibilityName(String value) {
        getInstance().setProperty(BIBSONOMY_SIDE_PANE_VISIBILITY_NAME, value);
    }

    public static Order getTagCloudOrder() {
        String order = getInstance().getProperty(BIBSONOMY_TAG_CLOUD_ORDER, "FREQUENCY");
        return Order.getOrderByName(order);
    }

    public static void setTagCloudOrder(Order order) {
        getInstance().setProperty(BIBSONOMY_TAG_CLOUD_ORDER, order.toString());
    }

    public static void setApiUrl(String text) {
        getInstance().setProperty(API_URL, text);
    }
}
