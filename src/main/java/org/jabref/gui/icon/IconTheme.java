package org.jabref.gui.icon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignN;
import org.kordamp.ikonli.materialdesign2.MaterialDesignO;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.EnumSet.allOf;

public class IconTheme {

    public static final Color DEFAULT_DISABLED_COLOR = Color.web("#c8c8c8");
    public static final Color SELECTED_COLOR = Color.web("#50618F");
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";
    private static final Logger LOGGER = LoggerFactory.getLogger(IconTheme.class);
    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");
    private static final Set<Ikon> ICON_NAMES = new HashSet<>();

    public static Color getDefaultGroupColor() {
        return Color.web("#8a8a8a");
    }

    public static Optional<JabRefIcon> findIcon(String code, Color color) {
        if (ICON_NAMES.isEmpty()) {
            loadAllIkons();
        }
        return ICON_NAMES.stream().filter(icon -> icon.toString().equals(code.toUpperCase(Locale.ENGLISH)))
                         .map(internalMat -> new InternalMaterialDesignIcon(internalMat).withColor(color)).findFirst();
    }

    public static Image getJabRefImage() {
        return getImageFX("jabrefIcon48");
    }

    private static void loadAllIkons() {
        ServiceLoader<IkonProvider> providers = ServiceLoader.load(IkonProvider.class);

        for (IkonProvider provider : providers) {
            ICON_NAMES.addAll(allOf(provider.getIkon()));
        }
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

        ADD(MaterialDesignP.PLUS_CIRCLE_OUTLINE),
        ADD_FILLED(MaterialDesignP.PLUS_CIRCLE),
        ADD_NOBOX(MaterialDesignP.PLUS),
        ADD_ARTICLE(MaterialDesignP.PLUS),
        ADD_ENTRY(MaterialDesignP.PLAYLIST_PLUS),
        EDIT_ENTRY(MaterialDesignT.TOOLTIP_EDIT),
        EDIT_STRINGS(MaterialDesignT.TOOLTIP_TEXT),
        FOLDER(MaterialDesignF.FOLDER_OUTLINE),
        REMOVE(MaterialDesignM.MINUS_BOX),
        REMOVE_NOBOX(MaterialDesignM.MINUS),
        FILE(MaterialDesignF.FILE_OUTLINE),
        PDF_FILE(MaterialDesignF.FILE_PDF),
        DOI(MaterialDesignB.BARCODE_SCAN),
        DUPLICATE(MaterialDesignC.CONTENT_DUPLICATE),
        EDIT(MaterialDesignP.PENCIL),
        NEW(MaterialDesignF.FOLDER_PLUS),
        SAVE(MaterialDesignC.CONTENT_SAVE),
        SAVE_ALL(MaterialDesignC.CONTENT_SAVE_ALL),
        CLOSE(MaterialDesignC.CLOSE_CIRCLE),
        PASTE(JabRefMaterialDesignIcon.PASTE),
        CUT(MaterialDesignC.CONTENT_CUT),
        COPY(MaterialDesignC.CONTENT_COPY),
        COMMENT(MaterialDesignC.COMMENT),
        REDO(MaterialDesignR.REDO),
        UNDO(MaterialDesignU.UNDO),
        MARKER(MaterialDesignM.MARKER),
        REFRESH(MaterialDesignR.REFRESH),
        DELETE_ENTRY(MaterialDesignD.DELETE),
        SEARCH(MaterialDesignM.MAGNIFY),
        FILE_SEARCH(MaterialDesignF.FILE_FIND),
        ADVANCED_SEARCH(Color.CYAN, MaterialDesignM.MAGNIFY),
        PREFERENCES(MaterialDesignC.COG),
        SELECTORS(MaterialDesignS.STAR_SETTINGS),
        HELP(MaterialDesignH.HELP_CIRCLE),
        UP(MaterialDesignA.ARROW_UP),
        DOWN(MaterialDesignA.ARROW_DOWN),
        LEFT(MaterialDesignA.ARROW_LEFT_BOLD),
        RIGHT(MaterialDesignA.ARROW_RIGHT_BOLD),
        SOURCE(MaterialDesignC.CODE_BRACES),
        MAKE_KEY(MaterialDesignK.KEY_VARIANT),
        CLEANUP_ENTRIES(MaterialDesignB.BROOM),
        PRIORITY(MaterialDesignF.FLAG),
        PRIORITY_HIGH(Color.RED, MaterialDesignF.FLAG),
        PRIORITY_MEDIUM(Color.ORANGE, MaterialDesignF.FLAG),
        PRIORITY_LOW(Color.rgb(111, 204, 117), MaterialDesignF.FLAG),
        PRINTED(MaterialDesignP.PRINTER),
        RANKING(MaterialDesignS.STAR),
        RANK1(MaterialDesignS.STAR, MaterialDesignS.STAR_OUTLINE, MaterialDesignS.STAR_OUTLINE, MaterialDesignS.STAR_OUTLINE, MaterialDesignS.STAR_OUTLINE),
        RANK2(MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR_OUTLINE, MaterialDesignS.STAR_OUTLINE, MaterialDesignS.STAR_OUTLINE),
        RANK3(MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR_OUTLINE, MaterialDesignS.STAR_OUTLINE),
        RANK4(MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR_OUTLINE),
        RANK5(MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR, MaterialDesignS.STAR),
        WWW(MaterialDesignW.WEB),
        GROUP_INCLUDING(MaterialDesignF.FILTER_OUTLINE),
        GROUP_REFINING(MaterialDesignF.FILTER),
        AUTO_GROUP(MaterialDesignA.AUTO_FIX),
        GROUP_INTERSECTION(JabRefMaterialDesignIcon.SET_CENTER),
        GROUP_UNION(JabRefMaterialDesignIcon.SET_ALL),
        EMAIL(MaterialDesignE.EMAIL),
        EXPORT_TO_CLIPBOARD(MaterialDesignC.CLIPBOARD_ARROW_LEFT),
        ATTACH_FILE(MaterialDesignP.PAPERCLIP),
        AUTO_FILE_LINK(MaterialDesignF.FILE_FIND),
        AUTO_RENAME(MaterialDesignA.AUTO_FIX),
        DOWNLOAD_FILE(MaterialDesignD.DOWNLOAD),
        MOVE_TO_FOLDER(MaterialDesignF.FILE_SEND),
        COPY_TO_FOLDER(MaterialDesignC.CONTENT_COPY),
        RENAME(MaterialDesignR.RENAME_BOX),
        DELETE_FILE(MaterialDesignD.DELETE_FOREVER),
        REMOVE_LINK(MaterialDesignL.LINK_OFF),
        AUTO_LINKED_FILE(MaterialDesignL.LINK_PLUS),
        QUALITY_ASSURED(MaterialDesignC.CERTIFICATE),
        QUALITY(MaterialDesignC.CERTIFICATE),
        OPEN(MaterialDesignF.FOLDER_OUTLINE),
        OPEN_LIST(MaterialDesignF.FOLDER_OPEN_OUTLINE),
        ADD_ROW(MaterialDesignS.SERVER_PLUS),
        REMOVE_ROW(MaterialDesignS.SERVER_MINUS),
        PICTURE(MaterialDesignF.FILE_IMAGE),
        READ_STATUS_READ(Color.rgb(111, 204, 117, 1), MaterialDesignE.EYE),
        READ_STATUS_SKIMMED(Color.ORANGE, MaterialDesignE.EYE),
        READ_STATUS(MaterialDesignE.EYE),
        RELEVANCE(MaterialDesignS.STAR_CIRCLE),
        MERGE_ENTRIES(MaterialDesignC.COMPARE),
        CONNECT_OPEN_OFFICE(MaterialDesignO.OPEN_IN_APP),
        PLAIN_TEXT_IMPORT_TODO(MaterialDesignC.CHECKBOX_BLANK_CIRCLE_OUTLINE),
        PLAIN_TEXT_IMPORT_DONE(MaterialDesignC.CHECKBOX_MARKED_CIRCLE_OUTLINE),
        DONATE(MaterialDesignG.GIFT),
        MOVE_TAB_ARROW(MaterialDesignA.ARROW_UP_BOLD),
        OPTIONAL(MaterialDesignL.LABEL_OUTLINE),
        REQUIRED(MaterialDesignL.LABEL),
        INTEGRITY_FAIL(Color.RED, MaterialDesignC.CLOSE_CIRCLE),
        INTEGRITY_INFO(MaterialDesignI.INFORMATION),
        INTEGRITY_WARN(MaterialDesignA.ALERT_CIRCLE),
        INTEGRITY_SUCCESS(MaterialDesignC.CHECKBOX_MARKED_CIRCLE_OUTLINE),
        GITHUB(MaterialDesignG.GITHUB),
        TOGGLE_ENTRY_PREVIEW(MaterialDesignL.LIBRARY),
        TOGGLE_GROUPS(MaterialDesignV.VIEW_LIST),
        SHOW_PREFERENCES_LIST(MaterialDesignV.VIEW_LIST),
        WRITE_XMP(MaterialDesignI.IMPORT),
        FILE_WORD(MaterialDesignF.FILE_WORD),
        FILE_EXCEL(MaterialDesignF.FILE_EXCEL),
        FILE_POWERPOINT(MaterialDesignF.FILE_POWERPOINT),
        FILE_TEXT(MaterialDesignF.FILE_DOCUMENT),
        FILE_MULTIPLE(MaterialDesignF.FILE_MULTIPLE),
        FILE_OPENOFFICE(JabRefMaterialDesignIcon.OPEN_OFFICE),
        APPLICATION_EMACS(JabRefMaterialDesignIcon.EMACS),
        APPLICATION_LYX(JabRefMaterialDesignIcon.LYX),
        APPLICATION_TEXSTUDIO(JabRefMaterialDesignIcon.TEX_STUDIO),
        APPLICATION_TEXMAKER(JabRefMaterialDesignIcon.TEX_MAKER),
        APPLICATION_VIM(JabRefMaterialDesignIcon.VIM),
        APPLICATION_WINEDT(JabRefMaterialDesignIcon.WINEDT),
        KEY_BINDINGS(MaterialDesignK.KEYBOARD),
        FIND_DUPLICATES(MaterialDesignC.CODE_EQUAL),
        CONNECT_DB(MaterialDesignC.CLOUD_UPLOAD),
        SUCCESS(MaterialDesignC.CHECK_CIRCLE),
        CHECK(MaterialDesignC.CHECK),
        WARNING(MaterialDesignA.ALERT),
        ERROR(MaterialDesignA.ALERT_CIRCLE),
        CASE_SENSITIVE(MaterialDesignA.ALPHABETICAL),
        REG_EX(MaterialDesignR.REGEX),
        FULLTEXT(MaterialDesignF.FILE_EYE),
        CONSOLE(MaterialDesignC.CONSOLE),
        FORUM(MaterialDesignF.FORUM),
        FACEBOOK(MaterialDesignF.FACEBOOK),
        TWITTER(MaterialDesignT.TWITTER),
        BLOG(MaterialDesignR.RSS),
        DATE_PICKER(MaterialDesignC.CALENDAR),
        DEFAULT_GROUP_ICON_COLORED(MaterialDesignR.RECORD),
        DEFAULT_GROUP_ICON(MaterialDesignL.LABEL_OUTLINE),
        ALL_ENTRIES_GROUP_ICON(MaterialDesignD.DATABASE),
        IMPORT(MaterialDesignC.CALL_RECEIVED),
        EXPORT(MaterialDesignC.CALL_MADE),
        PREVIOUS_LEFT(MaterialDesignC.CHEVRON_LEFT),
        PREVIOUS_UP(MaterialDesignC.CHEVRON_UP),
        NEXT_RIGHT(MaterialDesignC.CHEVRON_RIGHT),
        NEXT_DOWN(MaterialDesignC.CHEVRON_DOWN),
        LIST_MOVE_LEFT(MaterialDesignC.CHEVRON_LEFT),
        LIST_MOVE_UP(MaterialDesignC.CHEVRON_UP),
        LIST_MOVE_RIGHT(MaterialDesignC.CHEVRON_RIGHT),
        LIST_MOVE_DOWN(MaterialDesignC.CHEVRON_DOWN),
        FIT_WIDTH(MaterialDesignA.ARROW_EXPAND_ALL),
        FIT_SINGLE_PAGE(MaterialDesignN.NOTE),
        ZOOM_OUT(MaterialDesignM.MAGNIFY_MINUS),
        ZOOM_IN(MaterialDesignM.MAGNIFY_PLUS),
        ENTRY_TYPE(MaterialDesignP.PENCIL),
        NEW_GROUP(MaterialDesignP.PLUS),
        OPEN_LINK(MaterialDesignO.OPEN_IN_NEW),
        LOOKUP_IDENTIFIER(MaterialDesignS.SEARCH_WEB),
        FETCH_FULLTEXT(MaterialDesignS.SEARCH_WEB),
        FETCH_BY_IDENTIFIER(MaterialDesignC.CLIPBOARD_ARROW_DOWN),
        TOGGLE_ABBREVIATION(MaterialDesignF.FORMAT_ALIGN_CENTER),
        NEW_FILE(MaterialDesignP.PLUS),
        DOWNLOAD(MaterialDesignD.DOWNLOAD),
        OWNER(MaterialDesignA.ACCOUNT),
        CLOSE_JABREF(MaterialDesignD.DOOR),
        ARTICLE(MaterialDesignF.FILE_DOCUMENT),
        BOOK(MaterialDesignB.BOOK_OPEN_PAGE_VARIANT),
        LATEX_CITATIONS(JabRefMaterialDesignIcon.TEX_STUDIO),
        LATEX_FILE_DIRECTORY(MaterialDesignF.FOLDER_OUTLINE),
        LATEX_FILE(MaterialDesignF.FILE_OUTLINE),
        LATEX_COMMENT(MaterialDesignC.COMMENT_TEXT_OUTLINE),
        LATEX_LINE(MaterialDesignF.FORMAT_LINE_SPACING),
        PASSWORD_REVEALED(MaterialDesignE.EYE),
        ADD_ABBREVIATION_LIST(MaterialDesignP.PLUS),
        OPEN_ABBREVIATION_LIST(MaterialDesignF.FOLDER_OUTLINE),
        REMOVE_ABBREVIATION_LIST(MaterialDesignM.MINUS),
        ADD_ABBREVIATION(MaterialDesignP.PLAYLIST_PLUS),
        REMOVE_ABBREVIATION(MaterialDesignP.PLAYLIST_MINUS),
        NEW_ENTRY_FROM_PLAIN_TEXT(MaterialDesignP.PLUS_BOX),
        REMOTE_DATABASE(MaterialDesignD.DATABASE),
        HOME(MaterialDesignH.HOME),
        LINK(MaterialDesignL.LINK),
        LINK_VARIANT(MaterialDesignL.LINK_VARIANT),
        PROTECT_STRING(MaterialDesignC.CODE_BRACES),
        SELECT_ICONS(MaterialDesignA.APPS),
        KEEP_SEARCH_STRING(MaterialDesignE.EARTH),
        KEEP_ON_TOP(MaterialDesignP.PIN),
        KEEP_ON_TOP_OFF(MaterialDesignP.PIN_OFF_OUTLINE),
        OPEN_GLOBAL_SEARCH(MaterialDesignO.OPEN_IN_NEW);

        private final JabRefIcon icon;

        JabRefIcons(Ikon... icons) {
            icon = new InternalMaterialDesignIcon(icons);
        }

        JabRefIcons(Color color, Ikon... icons) {
            icon = new InternalMaterialDesignIcon(color, icons);
        }

        @Override
        public Ikon getIkon() {
            return icon.getIkon();
        }

        @Override
        public Node getGraphicNode() {
            return icon.getGraphicNode();
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
