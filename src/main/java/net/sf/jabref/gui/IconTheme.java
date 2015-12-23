/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.gui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class IconTheme {

    public static Font FONT;
    public static Font FONT_16;

    //public static final Color DEFAULT_COLOR = new Color(79, 95, 143); // JabRef's default color
    public static final Color DEFAULT_COLOR = new Color(0x155115); // Christmas edition

    //public static final Color DEFAULT_DISABLED_COLOR = new Color(200, 200, 200); // JabRef's default color
    public static final Color DEFAULT_DISABLED_COLOR = new Color(0x990000); // Christmas edition

    public static final int DEFAULT_SIZE = 24;
    public static final int SMALL_SIZE = 16;

    private static final Log LOGGER = LogFactory.getLog(IconTheme.class);

    static {
        try (InputStream stream = FontBasedIcon.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf")) {
            FONT = Font.createFont(Font.TRUETYPE_FONT, stream);
            FONT_16 = FONT.deriveFont(Font.PLAIN, 16f);
        } catch (FontFormatException | IOException e) {
            LOGGER.warn("Error loading font", e);
        }
    }

    public enum JabRefIcon {

        ADD("\uf4c0") /*css: mdi-plus-box*/,
        ADD_NOBOX("\uf4bf") /*css: plus */,
        ADD_ENTRY("\uf5b0") /*css: tooltip-outline-plus */,
        EDIT_ENTRY("\uf5ad") /*css: tooltip-edit */,
        EDIT_STRINGS("\uf5b1") /*css: tooltip-text */,
        FOLDER("\uf316"), /*css: folder */
        REMOVE("\uf429"), /*css: minus-box */
        REMOVE_NOBOX("\uf428") /*css: minus */,
        FILE("\uf2e0"), /* css: file*/
        PDF_FILE("\uf2f0"), /* css: file-pdf*/
        DOI("\uf61c") /*css: web*/,
        DUPLICATE("\uf261") /*css: content-duplicate */,
        EDIT("\uf499") /*css: pencil */,
        NEW("\uf2ef") /* css: file-outline */,
        SAVE("\uf263") /*css: content-save*/,
        SAVE_ALL("\uf264") /*css: content-save-all*/,
        CLOSE("\uf22b") /* css: close */,
        PASTE("\uf262") /*css: content-paste*/,
        CUT("\uf260") /*css: content-cut*/,
        COPY("\uf25f") /*css: content-copy */,
        REDO("\uf4ed") /*css: redo*/,
        UNDO("\uf5d0") /*css: undo*/,
        MARK_ENTRIES("\uf1a6") /*css: bookmark */,
        UNMARK_ENTRIES("\uf1a9") /*css: bookmark-outline */,
        REFRESH("\uf4ef") /*css: refresh */,
        DELETE_ENTRY("\uf28d") /*css: delete */,
        SEARCH("\uf401") /*css: magnify */,
        PREFERENCES("\uf529") /*css: settings */,
        HELP("\uf396") /*css: help-circle*/,
        HELP_CONTENTS("\uf1a4") /*css: book-open */,
        UP("\uf21c") /*css: chevron-up */,
        DOWN("\uf219") /*css: chevron-down */,
        LEFT("\uf145") /* css; arrow-left-bold */,
        RIGHT("\uf14a") /*css: arrow-right-bold */,
        SOURCE("\uf23c") /*css: code-braces*/,
        MAKE_KEY("\uf3c5") /*css: key-variant */,
        CLEANUP_ENTRIES("\uf1c6") /*css: broom */,
        PRIORITY("\uf306") /*css: flag */,
        PRIORITY_HIGH("\uf306", Color.RED) /*css: flag */,
        PRIORITY_MEDIUM("\uf306", Color.ORANGE) /*css: flag */,
        PRIORITY_LOW("\uf306", new Color(111, 204, 117)) /*css: flag */,
        PRINTED("\uf4d2") /*css: printer */,
        RANKING("\uf55e") /*css: star + star-outline: f561*/,
        RANK1("\uf55e\uf561\uf561\uf561\uf561"),
        RANK2("\uf55e\uf55e\uf561\uf561\uf561"),
        RANK3("\uf55e\uf55e\uf55e\uf561\uf561"),
        RANK4("\uf55e\uf55e\uf55e\uf55e\uf561"),
        RANK5("\uf55e\uf55e\uf55e\uf55e\uf55e"),
        WWW("\uf61c") /*css: web*/,
        GROUP_INCLUDING("\uf2fe") /*css: filter-outline*/,
        GROUP_REFINING("\uf2fe") /*css: filter*/,
        AUTO_GROUP("\uf158"), /*css: auto-fix*/
        EMAIL("\uf2ba") /*css: email*/,
        EXPORT_TO_CLIPBOARD("\uf224") /*css: clipboard-arrow-left */,
        ATTACH_FILE("\uf490") /*css: paperclip*/,
        AUTO_FILE_LINK("\uf2e9") /*css: file-find */,
        QUALITY_ASSURED("\uf1fe"), /*css: certificate */
        QUALITY("\uf1fe"),/*css: certificate */
        OPEN("\uf316") /*css: folder */,
        ADD_ROW("\uf526") /* css: server-plus*/,
        REMOVE_ROW("\uf522") /*css: server-minus */,
        PICTURE("\uf2ea") /*css: file-image */,
        READ_STATUS_READ("\uf2d4", new Color(111, 204, 117)), /*css: eye */
        READ_STATUS_SKIMMED("\uf2d4", Color.ORANGE), /*css: eye */
        READ_STATUS("\uf2d4"),/*css: eye */
        RELEVANCE("\uf55f"),/*css: star-circle */
        MERGE_ENTRIES("\uf25b"), /* css: compare */
        CONNECT_OPEN_OFFICE("\uf47a") /*css: open-in-app */,
        PLAIN_TEXT_IMPORT_TODO("\uf20a") /* css: checkbox-blank-circle-outline*/,
        PLAIN_TEXT_IMPORT_DONE("\uf20e") /* checkbox-marked-circle-outline */,
        DONATE("\uf367"), /* css: gift */
        MOVE_TAB_ARROW("\uf151"), /*css:  arrow-up-bold */
        OPTIONAL("\uf3cf"), /*css: label-outline */
        REQUIRED("\uf3ce"), /*css: label */
        INTEGRITY_FAIL("\uf22e", Color.RED), /*css: close-circle */
        INTEGRITY_INFO("\uf3b7"), /*css: information */
        INTEGRITY_WARN("\uf124"), /*css alert-circle */
        INTEGRITY_SUCCESS("\uf20e") /*css: checkbox-marked-circle-outline */,
        GITHUB("\uf36a"), /*css: github-circle*/
        TOGGLE_ENTRY_PREVIEW("\uf3ea"), /*css: library-books */
        TOGGLE_GROUPS("\uf5f1"), /*css: view-list */
        WRITE_XMP("\uf3b5"), /* css: import */
        FILE_WORD("\uf2f7"), /*css: file-word */
        FILE_EXCEL("\uf2e7"), /*css: file-excel */
        FILE_POWERPOINT("\uf2f2"), /*css: file-powerpoint */
        FILE_TEXT("\uf2e5"), /*css: file-document */
        FILE_MULTIPLE("\uf2ed"), /*css: file-multiple */
        KEY_BINDINGS("\uf3c6"), /*css: keyboard */
        FIND_DUPLICATES("\uf23e"), /*css: code-equal */

        OPEN_IN_NEW_WINDOW("\uf47b"), /*css: open-in-new */

        // STILL MISSING:
        GROUP_REGULAR("\uF4E6", Color.RED);

        private final String code;
        private final Color color;

        JabRefIcon(String code) {
            this(code, IconTheme.DEFAULT_COLOR);
        }

        JabRefIcon(String code, Color color) {
            this.code = code;
            this.color = color;
        }

        public FontBasedIcon getIcon() {
            return new FontBasedIcon(this.code, this.color);
        }

        public FontBasedIcon getSmallIcon() {
            return new FontBasedIcon(this.code, this.color, IconTheme.SMALL_SIZE);
        }

        public String getCode() {
            return this.code;
        }
    }

    public static class FontBasedIcon implements Icon {

        private final String iconCode;
        private final Color iconColor;
        private final int size;

        public FontBasedIcon(String code, Color iconColor) {
            this.iconCode = code;
            this.iconColor = iconColor;
            this.size = IconTheme.DEFAULT_SIZE;
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
            return new FontBasedIcon(this.iconCode, this.iconColor, IconTheme.SMALL_SIZE);
        }

        public FontBasedIcon createWithNewColor(Color newColor) {
            return new FontBasedIcon(this.iconCode, newColor, this.size);
        }

    }


    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";

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
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory
     *               where icon files are to be found.
     * @return A Map containing all key-value pairs found.
     */
    // FIXME: prefix can be removed?!
    private static Map<String, String> readIconThemeFile(URL url, String prefix) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(prefix, "prefix");

        Map<String, String> result = new HashMap<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.contains("=")) {
                    continue;
                }

                int index = line.indexOf("=");
                String key = line.substring(0, index).trim();
                String value = prefix + line.substring(index + 1).trim();
                result.put(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to read default icon theme.", e);
        }
        return result;
    }
}
