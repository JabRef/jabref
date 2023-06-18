package org.jabref.gui.fieldeditors.journalinfo;

import java.util.Objects;

import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalInfoView extends VBox {
    private final JournalInfoViewModel viewModel;

    public JournalInfoView() {
        this.viewModel = new JournalInfoViewModel();

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.getStylesheets().add(Objects.requireNonNull(JournalInfoView.class.getResource("JournalInfo.css")).toExternalForm());
    }

    public Parent getNode() {
        return this;
    }
}
