package org.jabref.styletester;

import javafx.scene.Parent;

import com.airhacks.afterburner.views.ViewLoader;

public class StyleTesterView {
    private Parent content;

    public StyleTesterView() {
        content = ViewLoader.view(this)
                            .load()
                            .getView();
    }

    public Parent getContent() {
        return content;
    }
}
