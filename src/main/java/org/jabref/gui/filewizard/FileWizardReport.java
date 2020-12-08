package org.jabref.gui.filewizard;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileWizardReport extends BaseDialog<Void> {

    @FXML ListView<String> unsuccessfulList;

    public FileWizardReport(List<BibEntry> unsuccessfullyLinked) {

        this.setTitle(Localization.lang("File Wizard"));

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        List<String> list = new ArrayList<>();

        for(int i = 0; i < unsuccessfullyLinked.size(); i++) {
            list.add(unsuccessfullyLinked.get(i).getAuthorTitleYear(60));
        }

        ObservableList<String> names = FXCollections.observableArrayList(list);
        unsuccessfulList.setItems(names);
    }
}
