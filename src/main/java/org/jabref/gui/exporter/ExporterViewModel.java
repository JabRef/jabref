package org.jabref.gui.exporter;

import org.jabref.logic.exporter.TemplateExporter;

public class ExporterViewModel {

    TemplateExporter exporter;

    public ExporterViewModel(TemplateExporter exporter) {
        this.exporter = exporter;
    }

    public TemplateExporter getLogic() {
        return this.exporter;
    }

}
