package org.jabref.logic.integrity;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class IntegrityCheckResultTxtWriter extends IntegrityCheckResultWriter {

    public IntegrityCheckResultTxtWriter(Writer writer, List<IntegrityMessage> messages) {
        super(writer, messages);
    }

    @Override
    public void writeFindings() throws IOException {
        for (IntegrityMessage message : messages) {
            writer.write(message.toString());
            writer.write(System.lineSeparator());
        }
    }
}
