package org.jabref.gui.preferences;

import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class ImportSettingsTab extends Pane implements PrefsTab {

    public static final String[] DEFAULT_FILENAMEPATTERNS = new String[] {"[bibtexkey]",
            "[bibtexkey] - [title]"};

    private static final String[] DEFAULT_FILENAMEPATTERNS_DISPLAY = new String[] {"bibtexkey", "bibtexkey - title",};

    private final JabRefPreferences prefs;
    private final GridPane builder = new GridPane();
    private final TextField fileNamePattern;
    private final ComboBox<String> selectFileNamePattern;


    private final TextField fileDirPattern;

    public ImportSettingsTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);

        fileNamePattern = new TextField();
        fileDirPattern = new TextField();
        selectFileNamePattern = new ComboBox<>();
        selectFileNamePattern.getItems().addAll(FXCollections.observableArrayList(DEFAULT_FILENAMEPATTERNS_DISPLAY));
        selectFileNamePattern.setValue(Localization.lang("Choose pattern"));
        selectFileNamePattern.setOnAction(e -> {
            fileNamePattern.setText(selectFileNamePattern.getValue());
        });

        Label defaultImportStyle = new Label(Localization.lang("Default import style for drag and drop of PDFs"));
        defaultImportStyle.getStyleClass().add("sectionHeader");
        builder.add(defaultImportStyle, 1, 1);
        builder.add(new Separator(), 2, 1);
        builder.add(new Label(""), 1, 7);

        Label defaultPdfFileLinkAction = new Label(Localization.lang("Default PDF file link action"));
        defaultPdfFileLinkAction.getStyleClass().add("sectionHeader");
        builder.add(defaultPdfFileLinkAction, 1, 8);
        Label filenameFormatPattern = new Label(Localization.lang("Filename format pattern").concat(":"));
        builder.add(filenameFormatPattern, 1, 9);
        builder.add(fileNamePattern, 2, 9);
        builder.add(selectFileNamePattern, 3, 9);

        Label lbfileDirPattern = new Label(Localization.lang("File directory pattern").concat(":"));
        builder.add(lbfileDirPattern, 1, 10);
        builder.add(fileDirPattern, 2, 10);
    }

    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        fileNamePattern.setText(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        fileDirPattern.setText(prefs.get(JabRefPreferences.IMPORT_FILEDIRPATTERN));
    }

    @Override
    public void storeSettings() {
        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, fileNamePattern.getText());
        prefs.put(JabRefPreferences.IMPORT_FILEDIRPATTERN, fileDirPattern.getText());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Import");
    }

}
