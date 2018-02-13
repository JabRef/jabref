package org.jabref.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import javafx.scene.Node;
import javafx.scene.image.Image;

import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.preferences.JabRefPreferences;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconTheme {

    /**
     * JabRef's default color
     */
    public static final Color DEFAULT_COLOR = JabRefPreferences.getInstance().getColor(JabRefPreferences.ICON_ENABLED_COLOR);
    public static final Color DEFAULT_DISABLED_COLOR = JabRefPreferences.getInstance().getColor(JabRefPreferences.ICON_DISABLED_COLOR);
    public static Font FONT;
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";
    private static final Logger LOGGER = LoggerFactory.getLogger(IconTheme.class);
    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(
            IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");

    // Christmas edition
    //public static final Color DEFAULT_COLOR = new Color(0x155115);
    //public static final Color DEFAULT_DISABLED_COLOR = new Color(0x990000);
    private static Font FONT_16;
    private static javafx.scene.text.Font FX_FONT;

    static {
        try (InputStream stream = getMaterialDesignIconsStream()) {
            FONT = Font.createFont(Font.TRUETYPE_FONT, stream);
            FONT_16 = FONT.deriveFont(Font.PLAIN, 16f);
            try (InputStream stream2 = getMaterialDesignIconsStream()) {
                FX_FONT = javafx.scene.text.Font.loadFont(stream2, JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_LARGE));
            }
        } catch (FontFormatException | IOException e) {
            LOGGER.warn("Error loading font", e);
        }
    }

    private static InputStream getMaterialDesignIconsStream() {
        return FontBasedIcon.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf");
    }

    public static javafx.scene.paint.Color getDefaultColor() {
        return javafx.scene.paint.Color.rgb(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getGreen(), DEFAULT_COLOR.getBlue(), DEFAULT_COLOR.getAlpha() / 255.0);
    }

    /**
     * Constructs an ImageIcon for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The ImageIcon for the function.
     */
    public static ImageIcon getImage(String name) {
        return new ImageIcon(getIconUrl(name));
    }

    public static Image getJabRefImageFX() {
        return getImageFX("jabrefIcon48");
    }

    /**
     * Constructs an {@link Image} for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The {@link Image} for the function.
     */
    public static Image getImageFX(String name) {
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

    public static List<java.awt.Image> getLogoSet() {
        List<java.awt.Image> jabrefLogos = new ArrayList<>();
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon16")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon20")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon32")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon40")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon48")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon64")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon128")).getImage());

        return jabrefLogos;
    }

    public enum JabRefIcon {

        ADD(MaterialDesignIcon.PLUS_BOX),
        ADD_NOBOX(MaterialDesignIcon.PLUS),
        ADD_ENTRY(MaterialDesignIcon.TOOLTIP_OUTLINE_PLUS),
        EDIT_ENTRY(MaterialDesignIcon.TOOLTIP_EDIT),
        EDIT_STRINGS(MaterialDesignIcon.TOOLTIP_TEXT),
        FOLDER(MaterialDesignIcon.FOOD_FORK_DRINK),
        REMOVE(MaterialDesignIcon.MINUS_BOX),
        REMOVE_NOBOX(MaterialDesignIcon.MINUS),
        FILE(MaterialDesignIcon.FILE),
        PDF_FILE(MaterialDesignIcon.FILE_PDF),
        DOI(MaterialDesignIcon.BARCODE_SCAN),
        DUPLICATE(MaterialDesignIcon.CONTENT_DUPLICATE),
        EDIT(MaterialDesignIcon.PENCIL),
        NEW(MaterialDesignIcon.FILE_OUTLINE),
        SAVE(MaterialDesignIcon.CONTENT_SAVE),
        SAVE_ALL(MaterialDesignIcon.CONTENT_SAVE_ALL),
        CLOSE(MaterialDesignIcon.CLOSE),
        PASTE(MaterialDesignIcon.CONTENT_PASTE),
        CUT(MaterialDesignIcon.CONTENT_CUT),
        COPY(MaterialDesignIcon.CONTENT_COPY),
        COMMENT(MaterialDesignIcon.COMMENT),
        REDO(MaterialDesignIcon.REDO),
        UNDO(MaterialDesignIcon.UNDO),
        MARK_ENTRIES(MaterialDesignIcon.BOOKMARK),
        MARKER(MaterialDesignIcon.MARKER),
        UNMARK_ENTRIES(MaterialDesignIcon.BOOKMARK_OUTLINE),
        REFRESH(MaterialDesignIcon.REFRESH),
        DELETE_ENTRY(MaterialDesignIcon.DELETE),
        SEARCH(MaterialDesignIcon.MAGNIFY),
        ADVANCED_SEARCH(Color.CYAN, MaterialDesignIcon.MAGNIFY),
        PREFERENCES(MaterialDesignIcon.SETTINGS),
        HELP(MaterialDesignIcon.HELP_CIRCLE),
        UP(MaterialDesignIcon.CHEVRON_UP),
        DOWN(MaterialDesignIcon.CHEVRON_DOWN),
        LEFT(MaterialDesignIcon.ARROW_LEFT_BOLD),
        RIGHT(MaterialDesignIcon.ARROW_RIGHT_BOLD),
        SOURCE(MaterialDesignIcon.CODE_BRACES),
        MAKE_KEY(MaterialDesignIcon.KEY_VARIANT),
        CLEANUP_ENTRIES(MaterialDesignIcon.BROOM),
        PRIORITY(MaterialDesignIcon.FLAG),
        PRIORITY_HIGH(Color.RED, MaterialDesignIcon.FLAG),
        PRIORITY_MEDIUM(Color.ORANGE, MaterialDesignIcon.FLAG),
        PRIORITY_LOW(new Color(111, 204, 117), MaterialDesignIcon.FLAG),
        PRINTED(MaterialDesignIcon.PRINTER),
        RANKING(MaterialDesignIcon.STAR),
        RANK1(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE),
        RANK2(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE),
        RANK3(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE, MaterialDesignIcon.STAR_OUTLINE),
        RANK4(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR_OUTLINE),
        RANK5(MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR, MaterialDesignIcon.STAR),
        WWW(MaterialDesignIcon.WEB) /*css: web*/,
        GROUP_INCLUDING(MaterialDesignIcon.FILTER_OUTLINE) /*css: filter-outline*/,
        GROUP_REFINING(MaterialDesignIcon.FILTER) /*css: filter*/,
        AUTO_GROUP(MaterialDesignIcon.AUTO_FIX), /*css: auto-fix*/
        EMAIL(MaterialDesignIcon.EMAIL) /*css: email*/,
        EXPORT_TO_CLIPBOARD(MaterialDesignIcon.CLIPBOARD_ARROW_LEFT) /*css: clipboard-arrow-left */,
        ATTACH_FILE(MaterialDesignIcon.PAPERCLIP) /*css: paperclip*/,
        AUTO_FILE_LINK(MaterialDesignIcon.FILE_FIND) /*css: file-find */,
        QUALITY_ASSURED(MaterialDesignIcon.CERTIFICATE), /*css: certificate */
        QUALITY(MaterialDesignIcon.CERTIFICATE),/*css: certificate */
        OPEN(MaterialDesignIcon.FOLDER) /*css: folder */,
        ADD_ROW(MaterialDesignIcon.SERVER_PLUS) /* css: server-plus*/,
        REMOVE_ROW(MaterialDesignIcon.SERVER_MINUS) /*css: server-minus */,
        PICTURE(MaterialDesignIcon.FILE_IMAGE) /*css: file-image */,
        READ_STATUS_READ(new Color(111, 204, 117), MaterialDesignIcon.EYE), /*css: eye */
        READ_STATUS_SKIMMED(Color.ORANGE, MaterialDesignIcon.EYE), /*css: eye */
        READ_STATUS(MaterialDesignIcon.EYE),/*css: eye */
        RELEVANCE(MaterialDesignIcon.STAR_CIRCLE),/*css: star-circle */
        MERGE_ENTRIES(MaterialDesignIcon.COMPARE), /* css: compare */
        CONNECT_OPEN_OFFICE(MaterialDesignIcon.OPEN_IN_APP) /*css: open-in-app */,
        PLAIN_TEXT_IMPORT_TODO(MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE) /* css: checkbox-blank-circle-outline*/,
        PLAIN_TEXT_IMPORT_DONE(MaterialDesignIcon.CHECKBOX_MARKED_CIRCLE_OUTLINE) /* checkbox-marked-circle-outline */,
        DONATE(MaterialDesignIcon.GIFT), /* css: gift */
        MOVE_TAB_ARROW(MaterialDesignIcon.ARROW_UP_BOLD), /*css:  arrow-up-bold */
        OPTIONAL(MaterialDesignIcon.LABEL_OUTLINE), /*css: label-outline */
        REQUIRED(MaterialDesignIcon.LABEL), /*css: label */
        INTEGRITY_FAIL(Color.RED, MaterialDesignIcon.CLOSE_CIRCLE), /*css: close-circle */
        INTEGRITY_INFO(MaterialDesignIcon.INFORMATION), /*css: information */
        INTEGRITY_WARN(MaterialDesignIcon.ALERT_CIRCLE), /*css alert-circle */
        INTEGRITY_SUCCESS(MaterialDesignIcon.CHECKBOX_MARKED_CIRCLE_OUTLINE) /*css: checkbox-marked-circle-outline */,
        GITHUB(MaterialDesignIcon.GITHUB_CIRCLE), /*css: github-circle*/
        TOGGLE_ENTRY_PREVIEW(MaterialDesignIcon.LIBRARY_BOOKS), /*css: library-books */
        TOGGLE_GROUPS(MaterialDesignIcon.VIEW_LIST), /*css: view-list */
        WRITE_XMP(MaterialDesignIcon.IMPORT), /* css: import */
        FILE_WORD(MaterialDesignIcon.FILE_WORD), /*css: file-word */
        FILE_EXCEL(MaterialDesignIcon.FILE_EXCEL), /*css: file-excel */
        FILE_POWERPOINT(MaterialDesignIcon.FILE_POWERPOINT), /*css: file-powerpoint */
        FILE_TEXT(MaterialDesignIcon.FILE_DOCUMENT), /*css: file-document */
        FILE_MULTIPLE(MaterialDesignIcon.FILE_MULTIPLE), /*css: file-multiple */
        KEY_BINDINGS(MaterialDesignIcon.KEYBOARD), /*css: keyboard */
        FIND_DUPLICATES(MaterialDesignIcon.CODE_EQUAL), /*css: code-equal */
        PULL(MaterialDesignIcon.SOURCE_PULL), /*source-pull*/
        SUCCESS(MaterialDesignIcon.CHECK_CIRCLE),
        WARNING(MaterialDesignIcon.ALERT),
        ERROR(MaterialDesignIcon.ALERT_CIRCLE),
        OPEN_IN_NEW_WINDOW(MaterialDesignIcon.OPEN_IN_NEW), /*css: open-in-new */
        CASE_SENSITIVE(MaterialDesignIcon.ALPHABETICAL), /* css: mdi-alphabetical */
        REG_EX(MaterialDesignIcon.REGEX), /*css: mdi-regex */
        CONSOLE(MaterialDesignIcon.CONSOLE), /*css: console */
        FORUM(MaterialDesignIcon.FORUM), /* css: forum */
        FACEBOOK(MaterialDesignIcon.FACEBOOK), /* css: facebook */
        TWITTER(MaterialDesignIcon.TWITTER), /* css: twitter */
        BLOG(MaterialDesignIcon.RSS), /* css: rss */
        GLOBAL_SEARCH(MaterialDesignIcon.EARTH), /* css: earth */
        DATE_PICKER(MaterialDesignIcon.CALENDAR), /* css: calendar */
        DEFAULT_GROUP_ICON(MaterialDesignIcon.LABEL_OUTLINE), /* css: label-outline */
        ALL_ENTRIES_GROUP_ICON(DefaultGroupsFactory.ALL_ENTRIES_GROUP_DEFAULT_ICON),
        // STILL MISSING:
        GROUP_REGULAR(Color.RED, MaterialDesignIcon.SYNC);

        private final List<MaterialDesignIcon> icons;
        private final Color color;
        private final String unicode;

        JabRefIcon(MaterialDesignIcon... icons) {
            this(IconTheme.DEFAULT_COLOR, icons);
        }

        JabRefIcon(Color color, MaterialDesignIcon... icons) {
            this.icons = Arrays.asList(icons);
            this.color = color;
            this.unicode = Arrays.stream(icons).map(MaterialDesignIcon::unicode).collect(Collectors.joining());
        }

        public FontBasedIcon getIcon() {
            return new FontBasedIcon(this.unicode, this.color);
        }

        public List<MaterialDesignIcon> getUnderlyingIcons() {
            return icons;
        }

        public MaterialDesignIcon getUnderlyingIcon() {
            return icons.get(0);
        }

        public FontBasedIcon getSmallIcon() {
            return new FontBasedIcon(this.unicode, this.color, JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL));
        }

        public Node getGraphicNode() {
            return new MaterialDesignIconView(this.icons.get(0));
        }

        public String getCode() {
            return this.unicode;
        }
    }

    public static class FontBasedIcon implements Icon {

        private final String iconCode;
        private final Color iconColor;
        private final int size;

        public FontBasedIcon(String code, Color iconColor) {
            this.iconCode = code;
            this.iconColor = iconColor;
            this.size = JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_LARGE);
        }

        public FontBasedIcon(String code, Color iconColor, int size) {
            this.iconCode = code;
            this.iconColor = iconColor;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();

            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHints(rh);

            g2.setFont(FONT.deriveFont(Font.PLAIN, size));
            g2.setColor(iconColor);
            FontMetrics fm = g2.getFontMetrics();

            g2.translate(x, y + fm.getAscent());
            g2.drawString(iconCode, 0, 0);

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        public FontBasedIcon createDisabledIcon() {
            return new FontBasedIcon(this.iconCode, IconTheme.DEFAULT_DISABLED_COLOR, this.size);
        }

        public FontBasedIcon createSmallIcon() {
            return new FontBasedIcon(this.iconCode, this.iconColor, JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL));
        }

        public FontBasedIcon createWithNewColor(Color newColor) {
            return new FontBasedIcon(this.iconCode, newColor, this.size);
        }
    }
}
