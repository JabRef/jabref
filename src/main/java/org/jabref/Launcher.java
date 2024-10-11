package org.jabref;

import org.jabref.cli.JabKit;
import org.jabref.gui.JabRefGUI;

/// The main entry point for the JabRef application.
///
/// It has two main functions:
///
/// - Handle the command line arguments
/// - Start the JavaFX application (if not in CLI mode)
public class Launcher extends JabKit {

    public static void main(String[] args) {
        Result result = JabKit.processArguments(args);
        // The method `processArguments` quites the whole JVM if no GUI is needed.

        JabRefGUI.setup(result.uiCommands(), result.preferences(), result.fileUpdateMonitor());
        JabRefGUI.launch(JabRefGUI.class, args);
    }
}
