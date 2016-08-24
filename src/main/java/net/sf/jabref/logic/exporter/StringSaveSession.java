package net.sf.jabref.logic.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StringSaveSession extends SaveSession {

    private static final Log LOGGER = LogFactory.getLog(StringSaveSession.class);

    private final ByteArrayOutputStream outputStream;

    public StringSaveSession(Charset encoding, boolean backup) {
        this(encoding, backup, new ByteArrayOutputStream());
    }

    private StringSaveSession(Charset encoding, boolean backup, ByteArrayOutputStream outputStream) {
        super(encoding, backup, new VerifyingWriter(outputStream, encoding));
        this.outputStream = outputStream;
    }

    public String getStringValue() {
        try {
            return outputStream.toString(encoding.name());
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn(e);
            return "";
        }
    }

    @Override
    public void commit(Path file) throws SaveException {
        try {
            Files.write(file, outputStream.toByteArray());
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    public void cancel() {
        try {
            outputStream.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }
}
