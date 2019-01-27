package org.jabref.gui.preferences;

import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import org.jabref.logic.l10n.Localization;
import org.jabref.pdfimport.ImportDialog;
import org.jabref.preferences.JabRefPreferences;

public class ImportSettingsTab extends Pane implements PrefsTab {

    public static final String[] DEFAULT_FILENAMEPATTERNS = new String[] {"[bibtexkey]",
            "[bibtexkey] - [title]"};

    public static final int DEFAULT_STYLE = ImportDialog.CONTENT;
    private static final String[] DEFAULT_FILENAMEPATTERNS_DISPLAY = new String[] {"bibtexkey", "bibtexkey - title",};

    private final JabRefPreferences prefs;
    private final RadioButton radioButtonXmp;
    private final RadioButton radioButtonPDFcontent;
    private final RadioButton radioButtonNoMeta;
    private final RadioButton radioButtononlyAttachPDF;
    private final CheckBox useDefaultPDFImportStyle;
    private final GridPane builder = new GridPane();
    private final TextField fileNamePattern;
    private final ComboBox<String> selectFileNamePattern;


    private final TextField fileDirPattern;

    public ImportSettingsTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);
        radioButtonNoMeta = new RadioButton(Localization.lang("Create blank entry linking the PDF"));
        radioButtonXmp = new RadioButton(Localization.lang("Create entry based on XMP-metadata"));
        radioButtonPDFcontent = new RadioButton(Localization.lang("Create entry based on content"));
        radioButtononlyAttachPDF = new RadioButton(Localization.lang("Only attach PDF"));

        useDefaultPDFImportStyle = new CheckBox(
                Localization.lang("Always use this PDF import style (and do not ask for each import)"));

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
        final ToggleGroup defaultImportStyleDragDropPdfs = new ToggleGroup();
        builder.add(new Separator(), 2, 1);
        builder.add(radioButtonNoMeta, 2, 2);
        builder.add(radioButtonXmp, 2, 3);
        builder.add(radioButtonPDFcontent, 2, 4);
        builder.add(radioButtononlyAttachPDF, 2, 5);
        radioButtonNoMeta.setToggleGroup(defaultImportStyleDragDropPdfs);
        radioButtonXmp.setToggleGroup(defaultImportStyleDragDropPdfs);
        radioButtonPDFcontent.setToggleGroup(defaultImportStyleDragDropPdfs);
        radioButtononlyAttachPDF.setToggleGroup(defaultImportStyleDragDropPdfs);
        builder.add(useDefaultPDFImportStyle, 2, 6);
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

        useDefaultPDFImportStyle.setSelected(prefs.getBoolean(JabRefPreferences.IMPORT_ALWAYSUSE));
        int style = prefs.getInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE);
        switch (style) {
            case ImportDialog.NOMETA:
                radioButtonNoMeta.setSelected(true);
                break;
            case ImportDialog.XMP:
                radioButtonXmp.setSelected(true);
                break;
            case ImportDialog.CONTENT:
                radioButtonPDFcontent.setSelected(true);
                break;
            case ImportDialog.ONLYATTACH:
                radioButtononlyAttachPDF.setSelected(true);
                break;
            default:
                // fallback
                radioButtonPDFcontent.setSelected(true);
                break;
        }
        fileNamePattern.setText(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        fileDirPattern.setText(prefs.get(JabRefPreferences.IMPORT_FILEDIRPATTERN));
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.IMPORT_ALWAYSUSE, useDefaultPDFImportStyle.isSelected());
        int style = ImportSettingsTab.DEFAULT_STYLE;
        if (radioButtonNoMeta.isSelected()) {
            style = ImportDialog.NOMETA;
        } else if (radioButtonXmp.isSelected()) {
            style = ImportDialog.XMP;
        } else if (radioButtonPDFcontent.isSelected()) {
            style = ImportDialog.CONTENT;
        } else if (radioButtononlyAttachPDF.isSelected()) {
            style = ImportDialog.ONLYATTACH;
        }
        prefs.putInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE, style);
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
