package org.jabref.gui.autocompleter;

/**
 * <ul>
 *   <li>For "ONLY_FULL", the auto completer returns the full name, e.g. "Smith, Bob"</li>
 *   <li>For "ONLY_ABBREVIATED", the auto completer returns the first name abbreviated, e.g. "Smith, B."</li>
 *   <li>For "BOTH", the auto completer returns both versions.</li>
 * </ul>
 */
public enum AutoCompleteFirstNameMode {
    ONLY_FULL,
    ONLY_ABBREVIATED,
    BOTH;

    public static AutoCompleteFirstNameMode parse(String input) {
        try {
            return AutoCompleteFirstNameMode.valueOf(input);
        } catch (IllegalArgumentException ex) {
            // Should only occur when preferences are set directly via preferences.put and not via setFirstnameMode
            return AutoCompleteFirstNameMode.BOTH;
        }
    }
}
