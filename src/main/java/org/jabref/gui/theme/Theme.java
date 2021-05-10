package org.jabref.gui.theme;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public class Theme {

    public enum Type {
        DEFAULT, EMBEDDED, CUSTOM
    }

    public static final String BASE_CSS = "Base.css";
    public static final String EMBEDDED_DARK_CSS = "Dark.css";

    private final Type type;
    private final String name;
    private final Optional<StyleSheet> additionalStylesheet;

    public Theme(String name) {
        /* String themeName = name != null ? name : "";

        if (themeName.equals("") || BASE_CSS.equalsIgnoreCase(themeName)) {
            this.additionalStylesheet = Optional.empty();
            this.type = Type.DEFAULT;
            themeName = "";
        } else if (EMBEDDED_DARK_CSS.equalsIgnoreCase(themeName)) {
            this.additionalStylesheet = StyleSheet.create(EMBEDDED_DARK_CSS);

            if (this.additionalStylesheet.isPresent()) {
                this.type = Type.EMBEDDED;
                themeName = EMBEDDED_DARK_CSS;
            } else {
                this.type = Type.DEFAULT;
                themeName = "";
            }
        } else {
            this.additionalStylesheet = StyleSheet.create(name);
            if (this.additionalStylesheet.isPresent()) {
                this.type = Type.CUSTOM;
                themeName = name;
            } else {
                this.type = Type.DEFAULT;
                themeName = "";
            }
        }

        this.name = themeName; */

        this.name = name != null ? name : "";
        if (StringUtil.isBlank(this.name) || BASE_CSS.equalsIgnoreCase(this.name)) {
            this.type = Type.DEFAULT;
        } else if (EMBEDDED_DARK_CSS.equalsIgnoreCase(this.name)) {
            this.type = Type.EMBEDDED;
        } else {
            this.type = Type.CUSTOM;
        }
        this.additionalStylesheet = switch (type) {
            case DEFAULT -> StyleSheet.create("Light.css");
            case EMBEDDED -> StyleSheet.create("Dark.css");
            case CUSTOM -> StyleSheet.create(name);
        };
    }

    public static Theme light() {
        return new Theme("");
    }

    public static Theme dark() {
        return new Theme(EMBEDDED_DARK_CSS);
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

    public Optional<StyleSheet> getAdditionalStylesheet() {
        return additionalStylesheet;
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

    @Override
    public String toString() {
        return "Theme{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
