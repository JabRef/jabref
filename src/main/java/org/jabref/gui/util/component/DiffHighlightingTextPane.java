package org.jabref.gui.util.component;

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class DiffHighlightingTextPane extends JTextPane {

    private static final String BODY_STYLE = "body{font:sans-serif}";
    private static final String ADDITION_STYLE = ".add{color:blue;text-decoration:underline}";
    private static final String REMOVAL_STYLE = ".del{color:red;text-decoration:line-through;}";
    private static final String CHANGE_STYLE = ".change{color:#006400;text-decoration:underline}";

    private static final String CONTENT_TYPE = "text/html";


    public DiffHighlightingTextPane() {
        super();
        setContentType(CONTENT_TYPE);
        StyleSheet sheet = ((HTMLEditorKit) getEditorKit()).getStyleSheet();
        sheet.addRule(BODY_STYLE);
        sheet.addRule(ADDITION_STYLE);
        sheet.addRule(REMOVAL_STYLE);
        sheet.addRule(CHANGE_STYLE);
        setEditable(false);
    }

}
