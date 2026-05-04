package org.jabref.gui.theme;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

/// Represents one of four types of a CSS-based Theme for JabRef:
///
/// - System - Tightly coupled to the <code>shouldThemeSyncOs</code> setting,
/// meaning we do not want to override the theme at all
/// - Light - Ignore whatever the OS is set to and make JabRef use the light theme
/// - Dark - Ignore whatever the OS is set to and make JabRef use the dark theme
/// - Custom - CSS provided by the user
public class Theme {

    public enum Type {
        SYSTEM, LIGHT, DARK, CUSTOM
    }

    public static final String SYSTEM = "";

    private static final String JABREF_THEME_CSS = "jabref-theme.css";

    private static final String LIGHT = "light";
    private static final String DARK = "dark";

    private final Type type;
    private final String name;
    private final Optional<StyleSheet> additionalStylesheet;

    public Theme(@NonNull String name) {
        if (SYSTEM.equalsIgnoreCase(name)) {
            this.additionalStylesheet = Optional.empty();
            this.type = Type.SYSTEM;
            this.name = name;
        } else if (LIGHT.equalsIgnoreCase(name)) {
            this.additionalStylesheet = Optional.empty();
            this.type = Type.LIGHT;
            this.name = name;
        } else if (DARK.equalsIgnoreCase(name)) {
            this.additionalStylesheet = Optional.empty();
            this.type = Type.DARK;
            this.name = name;
        } else {
            this.additionalStylesheet = StyleSheet.create(name);
            if (this.additionalStylesheet.isPresent()) {
                this.type = Type.CUSTOM;
            } else {
                this.type = Type.SYSTEM;
            }
            this.name = name;
        }
    }

    public static Theme light() {
        return new Theme(LIGHT);
    }

    public static Theme dark() {
        return new Theme(DARK);
    }

    public static Theme custom(String name) {
        return new Theme(name);
    }

    public static Theme system() {
        return new Theme(SYSTEM);
    }

    /// @return the Theme type. Guaranteed to be non-null.
    public Type getType() {
        return type;
    }

    /// Provides the name of the CSS, either for a built in theme, or for a raw, configured custom CSS location.
    /// This should be a file system path, but the raw string is
    /// returned even if it is not valid in some way. For this reason, the main use case for this getter is to
    /// storing or display the user preference, rather than to read and use the CSS file.
    ///
    /// @return the raw configured CSS location. Guaranteed to be non-null.
    public String getName() {
        return name;
    }

    /// This method allows callers to obtain the theme's additional stylesheet.
    ///
    /// @return called with the stylesheet location if there is an additional stylesheet present and available. The
    /// location will be a local URL. Typically it will be a `'data:'` URL where the CSS is embedded. However for
    /// large themes it can be `'file:'`.
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

    public static StyleSheet getJabRefTheme() {
        return StyleSheet.create(JABREF_THEME_CSS).orElseThrow();
    }
}
