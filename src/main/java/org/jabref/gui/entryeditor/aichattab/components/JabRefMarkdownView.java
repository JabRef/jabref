package org.jabref.gui.entryeditor.aichattab.components;

import one.jpro.platform.mdfx.MarkdownView;

import java.util.List;

public class JabRefMarkdownView extends MarkdownView {
    @Override
    protected List<String> getDefaultStylesheets() {
        return List.of("Base.css");
    }
}
