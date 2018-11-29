package org.jabref.gui.exporter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.exporter.TemplateExporter;

/**
 * ExporterViewModel wraps a TemplateExporter from logic and is used in the exporter customization dialog view
 * and ViewModel.
 *
 *
 */

public class ExporterViewModel {

    private final TemplateExporter exporter;
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty layoutFileName = new SimpleStringProperty();
    //If the TemplateExporter
    private final StringProperty extension = new SimpleStringProperty();

    public ExporterViewModel(TemplateExporter exporter) {
        this.exporter = exporter;
        this.name.setValue(exporter.getName());
        this.layoutFileName.setValue(exporter.getLayoutFileNameWithExtension());
        // Only the first of the extensions gotten from FileType is saved into the class using get(0)
        // substring(1) used to remove "*" from extension, i.e. "*.txt" to ".txt"
        String extensionString = exporter.getFileType().getExtensions().get(0);
        this.extension.setValue(extensionString);
    }

    public TemplateExporter Logic() {
        return this.exporter;
    }

    public StringProperty Name() {
        return this.name;

    }

    public StringProperty LayoutFileName() {
        return this.layoutFileName;
    }

    public StringProperty Extension() {
        return this.extension;
    }
}
