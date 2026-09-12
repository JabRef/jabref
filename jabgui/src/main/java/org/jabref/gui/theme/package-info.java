/// Theming of the JavaFX user interface: which stylesheets every scene gets, and keeping them
/// current when preferences, the operating system's color scheme, or a watched CSS file change.
///
/// [ThemeManager] installs the stylesheets on each scene. [ThemePreset] lists the selectable
/// themes: JabRef's own and the community ones, all bundled from
/// <https://themes.jabref.org/>, each with a bundled screenshot that [ThemePreviewView] shows in the
/// settings. [ThemeColorScheme] is the light/dark/follow-system choice, and [StyleSheet] wraps a CSS
/// source (classpath resource, file, or data URL).
///
/// The appearance settings that drive this package live in [org.jabref.gui.preferences.general].
/// How users write their own theme is described at <https://docs.jabref.org/advanced/custom-themes>.
package org.jabref.gui.theme;
