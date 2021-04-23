package org.jabref.logic.integrity;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class UTF8Checker implements EntryChecker {

    /**
     * Detect any non UTF-8 encoded field
     * @param entry the BibEntry of BibLatex.
     * @return return the warning of UTF-8 check for BibLatex.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        Charset charset = Charset.forName(System.getProperty("file.encoding"));
        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            boolean utfOnly = UTF8EncodingChecker(field.getValue().getBytes(charset));
            if (!utfOnly) {
                results.add(new IntegrityMessage(Localization.lang("Non-UTF-8 encoded field found"), entry,
                        field.getKey()));
            }
        }
        return results;
    }

    /**
     * Check whether a byte array is encoded in UTF-8 charset
     *
     * Use java api decoder and try&catch block to check the charset.
     * @param data the byte array used to check the encoding charset
     * @return true if is encoded in UTF-8 & false is not encoded in UTF-8
     */
    public static boolean UTF8EncodingChecker(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        try {
            decoder.decode(ByteBuffer.wrap(data));
        } catch (CharacterCodingException ex) {
            return false;
        }
        return true;
    }
}
