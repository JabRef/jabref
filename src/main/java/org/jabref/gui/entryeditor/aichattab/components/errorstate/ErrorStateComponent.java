package org.jabref.gui.entryeditor.aichattab.components.errorstate;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import com.airhacks.afterburner.views.ViewLoader;

public class ErrorStateComponent extends Pane {
    @FXML private Text titleText;
    @FXML private Text contentText;

    public ErrorStateComponent(String title, String content) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        setTitle(title);
        setContent(content);
    }

    public String getTitle() {
        return titleText.getText();
    }

    public void setTitle(String title) {
        titleText.setText(title);
    }

    public String getContent() {
        return contentText.getText();
    }

    public void setContent(String content) {
        contentText.setText(content);
    }
}
