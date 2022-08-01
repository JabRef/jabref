package org.jabref.gui.theme;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents one of three types of a css based Theme for JabRef:
 * <p>
 * The Default type of theme is the light theme (which is in fact the absence of any theme), the dark theme is currently
 * the only embedded theme and the custom themes, that can be created by loading a proper css file.
 */
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
        Objects.requireNonNull(name);

        if (name.equals("") || BASE_CSS.equalsIgnoreCase(name)) {
            this.additionalStylesheet = Optional.empty();
            this.type = Type.DEFAULT;
            this.name = "";
        } else if (EMBEDDED_DARK_CSS.equalsIgnoreCase(name)) {
            this.additionalStylesheet = StyleSheet.create(EMBEDDED_DARK_CSS);
            if (this.additionalStylesheet.isPresent()) {
                this.type = Type.EMBEDDED;
                this.name = EMBEDDED_DARK_CSS;
            } else {
                this.type = Type.DEFAULT;
                this.name = "";
            }
        } else {
            this.additionalStylesheet = StyleSheet.create(name);
             if (this.additionalStylesheet.isPresent()) {
                this.type = Type.CUSTOM;
                this.name = name;
            } else {
                this.type = Type.DEFAULT;
                this.name = "";
            }
        }
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

    /**
     * This method allows callers to obtain the theme's additional stylesheet.
     *
     * @return called with the stylesheet location if there is an additional stylesheet present and available. The
     * location will be a local URL. Typically it will be a {@code 'data:'} URL where the CSS is embedded. However for
     * large themes it can be {@code 'file:'}.
     */
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
