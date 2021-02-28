package org.jabref.gui.theme;

import java.util.Objects;
import java.util.Optional;

public class Theme {

    public enum Type {
        LIGHT, DARK, CUSTOM
    }

    public static final String BASE_CSS = "Base.css";
    public static final String EMBEDDED_LIGHT_THEME_CSS = "Light.css";
    public static final String EMBEDDED_DARK_THEME_CSS = "Dark.css";

    private final Type type;
    private final String name;
    private final Optional<StyleSheet> additionalStylesheet;

    public Theme(String name) {
        this.name = name != null ? name : "";
        if (this.name.equals("")
                || BASE_CSS.equalsIgnoreCase(this.name)) {
            this.type = Type.LIGHT;
            this.additionalStylesheet = Optional.empty();
        } else if (EMBEDDED_DARK_THEME_CSS.equalsIgnoreCase(this.name)) {
            this.type = Type.DARK;
            this.additionalStylesheet = StyleSheet.create(EMBEDDED_DARK_THEME_CSS);
        } else {
            this.type = Type.CUSTOM;
            this.additionalStylesheet = StyleSheet.create(name);
        }
    }

    public static Theme light() {
        return new Theme("");
    }

    public static Theme dark() {
        return new Theme(EMBEDDED_DARK_THEME_CSS);
    }

    public static Theme custom(String name) {
        return new Theme(name);
    }

    /**
     * @return the Theme type. Guaranteed to be non-null.
     */
    public Type getType() {
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
        Theme that = (Theme) o;
        return type == that.type && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    public Optional<StyleSheet> getAdditionalStylesheet() {
        return additionalStylesheet;
    }

    @Override
    public String toString() {
        return "Theme{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
