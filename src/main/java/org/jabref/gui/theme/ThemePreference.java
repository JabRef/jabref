package org.jabref.gui.theme;

import java.util.Objects;

import org.jabref.gui.util.Theme;
import org.jabref.model.strings.StringUtil;

public class ThemePreference {

    static final String EMBEDDED_DARK_THEME_CSS = "Dark.css";

    private final Theme.Type type;

    private final String name;

    public ThemePreference(String name) {
        this.name = name != null ? name : "";
        if (StringUtil.isBlank(this.name) || Theme.BASE_CSS.equalsIgnoreCase(this.name)) {
            this.type = Theme.Type.LIGHT;
        } else if (EMBEDDED_DARK_THEME_CSS.equalsIgnoreCase(this.name)) {
            this.type = Theme.Type.DARK;
        } else {
            this.type = Theme.Type.CUSTOM;
        }
    }

    public static ThemePreference light() {
        return new ThemePreference("");
    }

    public static ThemePreference dark() {
        return new ThemePreference(EMBEDDED_DARK_THEME_CSS);
    }

    /**
     * @return the Theme type. Guaranteed to be non-null.
     */
    public Theme.Type getType() {
        return type;
    }

    /**
     * Provides the name of the CSS, either for a built in theme, or for a raw, configured custom CSS location.
     * This should be a file system path, but the raw string is
     * returned even if it is not valid in some way. For this reason, the main use case for this getter is to
     * storing or display the user preference, rather than to read and use the CSS file.
     *
     * @return the raw configured CSS location. Guaranteed to be non-null.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThemePreference that = (ThemePreference) o;
        return type == that.type && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return "ThemePreference{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
