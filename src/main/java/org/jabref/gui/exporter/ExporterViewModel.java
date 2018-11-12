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
    private final StringProperty lfFileName = new SimpleStringProperty();
    //If the TemplateExporter
    private final StringProperty extension = new SimpleStringProperty();

    public ExporterViewModel(TemplateExporter exporter) {
        this.exporter = exporter;
        this.name.setValue(exporter.getName());
        this.lfFileName.setValue(exporter.getLayoutFileNameWithExtension());
        // Only the first of the extensions gotten from FileType is saved into the class using get(0)
        // substring(1) used to remove "*" from extension, i.e. "*.txt" to ".txt"
        String extensionString = exporter.getFileType().getExtensionsWithDot().get(0).substring(1);
        this.extension.setValue(extensionString);
    }

    public TemplateExporter getLogic() {
        return this.exporter;
    }

    public StringProperty getName() {
        return this.name;

    }

    public StringProperty getLayoutFileName() {
        return this.lfFileName;
    }

    public StringProperty getExtension() {
        return this.extension;
    }
}
