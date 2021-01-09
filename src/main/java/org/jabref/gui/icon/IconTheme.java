package org.jabref.gui.icon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconTheme {

    public static final Color DEFAULT_DISABLED_COLOR = Color.web("#c8c8c8");
    public static final Color SELECTED_COLOR = Color.web("#50618F");
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";
    private static final Logger LOGGER = LoggerFactory.getLogger(IconTheme.class);
    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");

    public static void loadFonts() {
        try (InputStream stream = getMaterialDesignIconsStream()) {
            Font.loadFont(stream, 7);
        } catch (IOException e) {
            LOGGER.error("Error loading Material Design Icons TTF font", e);
        }

        try (InputStream stream = getJabRefMaterialDesignIconsStream()) {
            Font.loadFont(stream, 7);
        } catch (IOException e) {
            LOGGER.error("Error loading custom font for custom JabRef icons", e);
        }
    }

    private static InputStream getMaterialDesignIconsStream() {
        return IconTheme.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf");
    }

    private static InputStream getJabRefMaterialDesignIconsStream() throws IOException {
        return IconTheme.class.getResource("/fonts/JabRefMaterialDesign.ttf").openStream();
    }

    public static Color getDefaultGroupColor() {
        return Color.web("#8a8a8a");
    }

    public static Image getJabRefImageFX() {
        return getImageFX("jabrefIcon48");
    }

    /*
     * Constructs an {@link Image} for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The {@link Image} for the function.
     */
    private static Image getImageFX(String name) {
        return new Image(getIconUrl(name).toString());
    }

    /**
     * Looks up the URL for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The URL to the actual image to use.
     */
    public static URL getIconUrl(String name) {
        String key = Objects.requireNonNull(name, "icon name");
        if (!KEY_TO_ICON.containsKey(key)) {
            LOGGER.warn("Could not find icon url by name " + name + ", so falling back on default icon "
                    + DEFAULT_ICON_PATH);
        }
        String path = KEY_TO_ICON.getOrDefault(key, DEFAULT_ICON_PATH);
        return Objects.requireNonNull(IconTheme.class.getResource(path), "Path must not be null for key " + key);
    }

    /**
     * Read a typical java property url into a Map. Currently doesn't support escaping
     * of the '=' character - it simply looks for the first '=' to determine where the key ends.
     * Both the key and the value is trimmed for whitespace at the ends.
     *
     * @param url    The URL to read information from.
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory where icon files are to be
     *               found.
     * @return A Map containing all key-value pairs found.
     */
    // FIXME: prefix can be removed?!
    private static Map<String, String> readIconThemeFile(URL url, String prefix) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(prefix, "prefix");

        Map<String, String> result = new HashMap<>();

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.contains("=")) {
                    continue;
                }

                int index = line.indexOf('=');
                String key = line.substring(0, index).trim();
                String value = prefix + line.substring(index + 1).trim();
                result.put(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to read default icon theme.", e);
        }
        return result;
    }

    public static List<Image> getLogoSetFX() {
        List<Image> jabrefLogos = new ArrayList<>();
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon16").toString()));
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon20").toString()));
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon32").toString()));
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon40").toString()));
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon48").toString()));
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon64").toString()));
        jabrefLogos.add(new Image(getIconUrl("jabrefIcon128").toString()));

        return jabrefLogos;
    }

    public enum JabRefIcons implements JabRefIcon {

        ADD(MaterialDesignIcon.PLUS_CIRCLE_OUTLINE),
        ADD_FILLED(MaterialDesignIcon.PLUS_CIRCLE),
        ADD_NOBOX(MaterialDesignIcon.PLUS),
        ADD_ARTICLE(MaterialDesignIcon.PLUS),
        ADD_ENTRY(MaterialDesignIcon.PLAYLIST_PLUS),
        EDIT_ENTRY(MaterialDesignIcon.TOOLTIP_EDIT),
        EDIT_STRINGS(MaterialDesignIcon.TOOLTIP_TEXT),
        FOLDER(MaterialDesignIcon.FOLDER_OUTLINE),
        REMOVE(MaterialDesignIcon.MINUS_BOX),
        REMOVE_NOBOX(MaterialDesignIcon.MINUS),
        FILE(MaterialDesignIcon.FILE_OUTLINE),
        PDF_FILE(MaterialDesignIcon.FILE_PDF),
        DOI(MaterialDesignIcon.BARCODE_SCAN),
        DUPLICATE(MaterialDesignIcon.CONTENT_DUPLICATE),
        EDIT(MaterialDesignIcon.PENCIL),
        NEW(MaterialDesignIcon.FOLDER_PLUS),
        SAVE(MaterialDesignIcon.CONTENT_SAVE),
        SAVE_ALL(MaterialDesignIcon.CONTENT_SAVE_ALL),
        CLOSE(MaterialDesignIcon.CLOSE_CIRCLE),
        PASTE(JabRefMaterialDesignIcon.PASTE),
        CUT(MaterialDesignIcon.CONTENT_CUT),
        COPY(MaterialDesignIcon.CONTENT_COPY),
        COMMENT(MaterialDesignIcon.COMMENT),
        REDO(MaterialDesignIcon.REDO),
        UNDO(MaterialDesignIcon.UNDO),
        MARKER(MaterialDesignIcon.MARKER),
        REFRESH(MaterialDesignIcon.REFRESH),
        DELETE_ENTRY(MaterialDesignIcon.DELETE),
        SEARCH(MaterialDesignIcon.MAGNIFY),
        FILE_SEARCH(MaterialDesignIcon.FILE_FIND),
        ADVANCED_SEARCH(Color.CYAN, MaterialDesignIcon.MAGNIFY),
        PREFERENCES(MaterialDesignIcon.SETTINGS),
        HELP(MaterialDesignIcon.HELP_CIRCLE),
        UP(MaterialDesignIcon.ARROW_UP),
        DOWN(MaterialDesignIcon.ARROW_DOWN),
        LEFT(MaterialDesignIcon.ARROW_LEFT_BOLD),
        RIGHT(MaterialDesignIcon.ARROW_RIGHT_BOLD),
        SOURCE(MaterialDesignIcon.CODE_BRACES),
        MAKE_KEY(MaterialDesignIcon.KEY_VARIANT),
        CLEANUP_ENTRIES(MaterialDesignIcon.BROOM),
        PRIORITY(MaterialDesignIcon.FLAG),
        PRIORITY_HIGH(Color.RED, MaterialDesignIcon.FLAG),
        PRIORITY_MEDIUM(Color.ORANGE, MaterialDesignIcon.FLAG),
        PRIORITY_LOW(Color.rgb(111, 204, 117), MaterialDesignIcon.FLAG),
        PRINTED(MaterialDesignIcon.PRINTER),
        RANKING(MaterialDesignIcon.STAR),
        RANK1(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE),
        RANK2(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE),
        RANK3(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE),
        RANK4(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE),
        RANK5(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR),
        WWW(MaterialDesignIcon.WEB),
        GROUP_INCLUDING(MaterialDesignIcon.FILTER_OUTLINE),
        GROUP_REFINING(MaterialDesignIcon.FILTER),
        AUTO_GROUP(MaterialDesignIcon.AUTO_FIX),
        GROUP_INTERSECTION(JabRefMaterialDesignIcon.SET_CENTER),
        GROUP_UNION(JabRefMaterialDesignIcon.SET_ALL),
        EMAIL(MaterialDesignIcon.EMAIL),
        EXPORT_TO_CLIPBOARD(MaterialDesignIcon.CLIPBOARD_ARROW_LEFT),
        ATTACH_FILE(MaterialDesignIcon.PAPERCLIP),
        AUTO_FILE_LINK(MaterialDesignIcon.FILE_FIND),
        AUTO_RENAME(MaterialDesignIcon.AUTO_FIX),
        DOWNLOAD_FILE(MaterialDesignIcon.DOWNLOAD),
        MOVE_TO_FOLDER(MaterialDesignIcon.FILE_SEND),
        COPY_TO_FOLDER(MaterialDesignIcon.CONTENT_COPY),
        RENAME(MaterialDesignIcon.RENAME_BOX),
        DELETE_FILE(MaterialDesignIcon.DELETE_FOREVER),
        REMOVE_LINK(MaterialDesignIcon.LINK_OFF),
        AUTO_LINKED_FILE(MaterialDesignIcon.BRIEFCASE_CHECK),
        QUALITY_ASSURED(MaterialDesignIcon.CERTIFICATE),
        QUALITY(MaterialDesignIcon.CERTIFICATE),
        OPEN(MaterialDesignIcon.FOLDER_OUTLINE),
        ADD_ROW(MaterialDesignIcon.SERVER_PLUS),
        REMOVE_ROW(MaterialDesignIcon.SERVER_MINUS),
        PICTURE(MaterialDesignIcon.FILE_IMAGE),
        READ_STATUS_READ(Color.rgb(111, 204, 117, 1), MaterialDesignIcon.EYE),
        READ_STATUS_SKIMMED(Color.ORANGE, MaterialDesignIcon.EYE),
        READ_STATUS(MaterialDesignIcon.EYE),
        RELEVANCE(MaterialDesignIcon.STAR_CIRCLE),
        MERGE_ENTRIES(MaterialDesignIcon.COMPARE),
        CONNECT_OPEN_OFFICE(MaterialDesignIcon.OPEN_IN_APP),
        PLAIN_TEXT_IMPORT_TODO(MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE),
        PLAIN_TEXT_IMPORT_DONE(MaterialDesignIcon.CHECKBOX_MARKED_CIRCLE_OUTLINE),
        DONATE(MaterialDesignIcon.GIFT),
        MOVE_TAB_ARROW(MaterialDesignIcon.ARROW_UP_BOLD),
        OPTIONAL(MaterialDesignIcon.LABEL_OUTLINE),
        REQUIRED(MaterialDesignIcon.LABEL),
        INTEGRITY_FAIL(Color.RED, MaterialDesignIcon.CLOSE_CIRCLE),
        INTEGRITY_INFO(MaterialDesignIcon.INFORMATION),
        INTEGRITY_WARN(MaterialDesignIcon.ALERT_CIRCLE),
        INTEGRITY_SUCCESS(MaterialDesignIcon.CHECKBOX_MARKED_CIRCLE_OUTLINE),
        GITHUB(MaterialDesignIcon.GITHUB_CIRCLE),
        TOGGLE_ENTRY_PREVIEW(MaterialDesignIcon.LIBRARY_BOOKS),
        TOGGLE_GROUPS(MaterialDesignIcon.VIEW_LIST),
        SHOW_PREFERENCES_LIST(MaterialDesignIcon.VIEW_LIST),
        WRITE_XMP(MaterialDesignIcon.IMPORT),
        FILE_WORD(MaterialDesignIcon.FILE_WORD),
        FILE_EXCEL(MaterialDesignIcon.FILE_EXCEL),
        FILE_POWERPOINT(MaterialDesignIcon.FILE_POWERPOINT),
        FILE_TEXT(MaterialDesignIcon.FILE_DOCUMENT),
        FILE_MULTIPLE(MaterialDesignIcon.FILE_MULTIPLE),
        FILE_OPENOFFICE(JabRefMaterialDesignIcon.OPEN_OFFICE),
        APPLICATION_EMACS(JabRefMaterialDesignIcon.EMACS),
        APPLICATION_LYX(JabRefMaterialDesignIcon.LYX),
        APPLICATION_TEXSTUDIO(JabRefMaterialDesignIcon.TEX_STUDIO),
        APPLICATION_TEXMAKER(JabRefMaterialDesignIcon.TEX_MAKER),
        APPLICATION_VIM(JabRefMaterialDesignIcon.VIM),
        APPLICATION_WINEDT(JabRefMaterialDesignIcon.WINEDT),
        KEY_BINDINGS(MaterialDesignIcon.KEYBOARD),
        FIND_DUPLICATES(MaterialDesignIcon.CODE_EQUAL),
        CONNECT_DB(MaterialDesignIcon.CLOUD_UPLOAD),
        SUCCESS(MaterialDesignIcon.CHECK_CIRCLE),
        CHECK(MaterialDesignIcon.CHECK),
        WARNING(MaterialDesignIcon.ALERT),
        ERROR(MaterialDesignIcon.ALERT_CIRCLE),
        CASE_SENSITIVE(MaterialDesignIcon.ALPHABETICAL),
        REG_EX(MaterialDesignIcon.REGEX),
        CONSOLE(MaterialDesignIcon.CONSOLE),
        FORUM(MaterialDesignIcon.FORUM),
        FACEBOOK(MaterialDesignIcon.FACEBOOK),
        TWITTER(MaterialDesignIcon.TWITTER),
        BLOG(MaterialDesignIcon.RSS),
        DATE_PICKER(MaterialDesignIcon.CALENDAR),
        DEFAULT_GROUP_ICON_COLORED(MaterialDesignIcon.PLAY),
        DEFAULT_GROUP_ICON(MaterialDesignIcon.LABEL_OUTLINE),
        ALL_ENTRIES_GROUP_ICON(MaterialDesignIcon.DATABASE),
        IMPORT(MaterialDesignIcon.CALL_RECEIVED),
        EXPORT(MaterialDesignIcon.CALL_MADE),
        PREVIOUS_LEFT(MaterialDesignIcon.CHEVRON_LEFT),
        PREVIOUS_UP(MaterialDesignIcon.CHEVRON_UP),
        NEXT_RIGHT(MaterialDesignIcon.CHEVRON_RIGHT),
        NEXT_DOWN(MaterialDesignIcon.CHEVRON_DOWN),
        LIST_MOVE_LEFT(MaterialDesignIcon.CHEVRON_LEFT),
        LIST_MOVE_UP(MaterialDesignIcon.CHEVRON_UP),
        LIST_MOVE_RIGHT(MaterialDesignIcon.CHEVRON_RIGHT),
        LIST_MOVE_DOWN(MaterialDesignIcon.CHEVRON_DOWN),
        FIT_WIDTH(MaterialDesignIcon.ARROW_EXPAND_ALL),
        FIT_SINGLE_PAGE(MaterialDesignIcon.NOTE),
        ZOOM_OUT(MaterialDesignIcon.MAGNIFY_MINUS),
        ZOOM_IN(MaterialDesignIcon.MAGNIFY_PLUS),
        ENTRY_TYPE(MaterialDesignIcon.PENCIL),
        NEW_GROUP(MaterialDesignIcon.PLUS),
        OPEN_LINK(MaterialDesignIcon.OPEN_IN_NEW),
        LOOKUP_IDENTIFIER(MaterialDesignIcon.MAGNIFY), // TODO: use WEB_SEARCH instead as soon as it is available
        FETCH_FULLTEXT(MaterialDesignIcon.MAGNIFY), // TODO: use WEB_SEARCH instead as soon as it is available
        FETCH_BY_IDENTIFIER(MaterialDesignIcon.CLIPBOARD_ARROW_DOWN),
        TOGGLE_ABBREVIATION(MaterialDesignIcon.FORMAT_ALIGN_CENTER),
        NEW_FILE(MaterialDesignIcon.PLUS),
        DOWNLOAD(MaterialDesignIcon.DOWNLOAD),
        OWNER(MaterialDesignIcon.ACCOUNT),
        CLOSE_JABREF(MaterialDesignIcon.GLASSDOOR),
        ARTICLE(MaterialDesignIcon.FILE_DOCUMENT),
        BOOK(MaterialDesignIcon.BOOK_OPEN_PAGE_VARIANT),
        LATEX_CITATIONS(JabRefMaterialDesignIcon.TEX_STUDIO),
        LATEX_FILE_DIRECTORY(MaterialDesignIcon.FOLDER_OUTLINE),
        LATEX_FILE(MaterialDesignIcon.FILE_OUTLINE),
        LATEX_COMMENT(MaterialDesignIcon.COMMENT_TEXT_OUTLINE),
        LATEX_LINE(MaterialDesignIcon.FORMAT_LINE_SPACING),
        PASSWORD_REVEALED(MaterialDesignIcon.EYE),
        ADD_ABBREVIATION_LIST(MaterialDesignIcon.FOLDER_PLUS),
        OPEN_ABBREVIATION_LIST(MaterialDesignIcon.FOLDER_OUTLINE),
        REMOVE_ABBREVIATION_LIST(MaterialDesignIcon.FOLDER_REMOVE),
        ADD_ABBREVIATION(MaterialDesignIcon.PLAYLIST_PLUS),
        REMOVE_ABBREVIATION(MaterialDesignIcon.PLAYLIST_MINUS),
        NEW_ENTRY_FROM_PLAIN_TEXT(MaterialDesignIcon.PLUS_BOX),
        REMOTE_DATABASE(MaterialDesignIcon.DATABASE),
        HOME(MaterialDesignIcon.HOME),
        LINK(MaterialDesignIcon.LINK),
        LINK_VARIANT(MaterialDesignIcon.LINK_VARIANT),
        PROTECT_STRING(MaterialDesignIcon.CODE_BRACES);

        private final JabRefIcon icon;

        JabRefIcons(GlyphIcons... icons) {
            icon = new InternalMaterialDesignIcon(icons);
        }

        JabRefIcons(Color color, MaterialDesignIcon... icons) {
            icon = new InternalMaterialDesignIcon(color, icons);
        }

        @Override
        public Node getGraphicNode() {
            return icon.getGraphicNode();
        }

        @Override
        public String unicode() {
            return icon.unicode();
        }

        @Override
        public String fontFamily() {
            return icon.fontFamily();
        }

        public Button asButton() {
            Button button = new Button();
            button.setGraphic(getGraphicNode());
            button.getStyleClass().add("icon-button");
            return button;
        }

        public ToggleButton asToggleButton() {
            ToggleButton button = new ToggleButton();
            button.setGraphic(getGraphicNode());
            button.getStyleClass().add("icon-button");
            return button;
        }

        @Override
        public JabRefIcon withColor(Color color) {
            return icon.withColor(color);
        }

        @Override
        public JabRefIcon disabled() {
            return icon.disabled();
        }
    }
}
